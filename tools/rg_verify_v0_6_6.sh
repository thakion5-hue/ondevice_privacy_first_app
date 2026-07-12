#!/usr/bin/env bash

set -u
set -o pipefail

EXIT_OK=0
EXIT_LEGACY_FOUND=2
EXIT_MISSING_066=3
EXIT_MISSING_V66=4
EXIT_V66_OUT_OF_SCOPE=5
EXIT_TOOLING=10
EXIT_USAGE=64

PROJECT_ROOT="."
CI=0
QUIET=0

show_help() {
  cat <<'EOF'
rg_verify_v0_6_6.sh

Verifies the v0.6.6 label policy for the On-device Privacy First App.

Rules enforced:
  1. Legacy version strings (v6.5 / 0.6.5 / v0.6.5) must be absent.
  2. The required version string 0.6.6 must be present at least once.
  3. The compatibility label v6.6 must be present and limited to:
       - app/src/main/assets/model_manifest.json
       - app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/provider/AiCoreTodoProvider.kt
       - app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/sessionfactory/AiCoreTodoSessionFactory.kt

Usage:
  ./tools/rg_verify_v0_6_6.sh
  ./tools/rg_verify_v0_6_6.sh --ci
  ./tools/rg_verify_v0_6_6.sh --project-root /path/to/repo --quiet

Options:
  -CI, --ci                   Machine-parseable CI output
  -Quiet, --quiet             Suppress per-hit output
  -ProjectRoot, --project-root PATH
                              Project root to scan
  -h, --help                  Show this help

Exit codes:
   0  All checks passed
   2  Legacy version strings still present
   3  Required version string 0.6.6 missing
   4  Required compatibility label v6.6 missing
   5  v6.6 found outside the allowed compatibility scope
  10  Tooling error (e.g. rg not installed)
  64  Usage error
EOF
}

while (($# > 0)); do
  case "$1" in
    -CI|--ci)
      CI=1
      ;;
    -Quiet|--quiet)
      QUIET=1
      ;;
    -ProjectRoot|--project-root)
      shift
      if (($# == 0)); then
        printf 'Missing value for %s\n' '--project-root' >&2
        exit "$EXIT_USAGE"
      fi
      PROJECT_ROOT="$1"
      ;;
    -h|--help)
      show_help
      exit 0
      ;;
    *)
      printf 'Unknown argument: %s\n\n' "$1" >&2
      show_help >&2
      exit "$EXIT_USAGE"
      ;;
  esac
  shift
done

declare -a RESULTS_CHECKS=()
declare -a RESULTS_STATUS=()
declare -a RESULTS_DETAIL=()

add_result() {
  RESULTS_CHECKS+=("$1")
  RESULTS_STATUS+=("$2")
  RESULTS_DETAIL+=("$3")
}

write_info() {
  if ((CI)); then
    printf '::info::%s\n' "$1"
  else
    printf '[INFO] %s\n' "$1"
  fi
}

write_pass() {
  if ((CI)); then
    printf '::pass::%s\n' "$1"
  else
    printf '[PASS] %s\n' "$1"
  fi
}

write_fail_line() {
  if ((CI)); then
    printf '::error::%s\n' "$1"
  else
    printf '[FAIL] %s\n' "$1"
  fi
}

write_hits() {
  local title=$1
  shift || true
  if ((QUIET || CI)); then
    return
  fi
  if (($# == 0)); then
    return
  fi
  printf '\n----- %s -----\n' "$title"
  printf '%s\n' "$@"
  printf '%s\n' '-------------------'
}

emit_summary() {
  local exit_code=$1
  local i
  if ((CI)); then
    printf '\n=== rg_verify_v0_6_6 summary ===\n'
    for i in "${!RESULTS_CHECKS[@]}"; do
      printf '%-40s %-6s %s\n' "${RESULTS_CHECKS[$i]}" "${RESULTS_STATUS[$i]}" "${RESULTS_DETAIL[$i]}"
    done
    printf 'exit_code=%s\n' "$exit_code"
    printf '================================\n'
  else
    printf '\n=== Verification summary ===\n'
    printf '%-40s %-6s %s\n' 'check' 'status' 'detail'
    for i in "${!RESULTS_CHECKS[@]}"; do
      printf '%-40s %-6s %s\n' "${RESULTS_CHECKS[$i]}" "${RESULTS_STATUS[$i]}" "${RESULTS_DETAIL[$i]}"
    done
    if ((exit_code == EXIT_OK)); then
      printf 'Result: PASS (exit %s)\n' "$exit_code"
    else
      printf 'Result: FAIL (exit %s)\n' "$exit_code"
    fi
  fi
}

exit_with() {
  local code=$1
  local reason=${2-}
  if [[ -n "$reason" ]]; then
    write_fail_line "$reason"
  fi
  emit_summary "$code"
  exit "$code"
}

if ! command -v rg >/dev/null 2>&1; then
  add_result "ripgrep availability" "FAIL" "rg not found in PATH"
  exit_with "$EXIT_TOOLING" "ripgrep (rg) was not found in PATH. Install ripgrep before running this verifier."
fi

RG_BIN=$(command -v rg)
add_result "ripgrep availability" "PASS" "$RG_BIN"

if ! ROOT=$(cd "$PROJECT_ROOT" 2>/dev/null && pwd -P); then
  exit_with "$EXIT_TOOLING" "Could not resolve project root: $PROJECT_ROOT"
fi
cd "$ROOT" || exit_with "$EXIT_TOOLING" "Could not enter project root: $ROOT"
write_info "Project root: $ROOT"

COMMON_ARGS=(
  -n
  --hidden
  --glob '!.git/'
  --glob '!**/.gradle/**'
  --glob '!**/build/**'
  --glob '!*.jar'
  --glob '!*.bin'
  --glob '!docs/release_notes_v0_6_6_short.md'
  --glob '!docs/INDEX.md'
  --glob '!README.md'
  --glob '!tools/rg_verify_v0_6_6.ps1'
  --glob '!tools/rg_verify_v0_6_6.sh'
)

RG_EXIT_CODE=0
RG_ERROR=''
declare -a RG_LINES=()

invoke_rg() {
  local pattern=$1
  local out_file err_file
  out_file=$(mktemp)
  err_file=$(mktemp)

  rg "${COMMON_ARGS[@]}" "$pattern" . >"$out_file" 2>"$err_file"
  RG_EXIT_CODE=$?
  RG_ERROR=$(cat "$err_file")
  mapfile -t RG_LINES <"$out_file" || true

  rm -f "$out_file" "$err_file"
}

invoke_rg 'v6\.5|0\.6\.5|v0\.6\.5'
if ((RG_EXIT_CODE >= 2)); then
  add_result "no-legacy-version-strings" "FAIL" "rg internal error (code $RG_EXIT_CODE)"
  exit_with "$EXIT_TOOLING" "ripgrep failed while scanning for legacy version strings.${RG_ERROR:+ $RG_ERROR}"
fi
if ((RG_EXIT_CODE == 0 && ${#RG_LINES[@]} > 0)); then
  write_hits "Legacy version strings" "${RG_LINES[@]}"
  add_result "no-legacy-version-strings" "FAIL" "${#RG_LINES[@]} hit(s)"
  exit_with "$EXIT_LEGACY_FOUND" "Legacy version strings (v6.5 / 0.6.5 / v0.6.5) are still present."
fi
write_pass "No legacy version strings found."
add_result "no-legacy-version-strings" "PASS" "0 hits"

invoke_rg '0\.6\.6'
if ((RG_EXIT_CODE >= 2)); then
  add_result "required-0.6.6-present" "FAIL" "rg internal error (code $RG_EXIT_CODE)"
  exit_with "$EXIT_TOOLING" "ripgrep failed while scanning for 0.6.6.${RG_ERROR:+ $RG_ERROR}"
fi
if ((RG_EXIT_CODE != 0 || ${#RG_LINES[@]} == 0)); then
  add_result "required-0.6.6-present" "FAIL" "0 hits"
  exit_with "$EXIT_MISSING_066" "Required version string 0.6.6 was not found anywhere in the repo."
fi
write_hits "0.6.6 occurrences" "${RG_LINES[@]}"
write_pass "Required version string 0.6.6 is present (${#RG_LINES[@]} hit(s))."
add_result "required-0.6.6-present" "PASS" "${#RG_LINES[@]} hits"

invoke_rg 'v6\.6'
if ((RG_EXIT_CODE >= 2)); then
  add_result "compat-v6.6-present" "FAIL" "rg internal error (code $RG_EXIT_CODE)"
  exit_with "$EXIT_TOOLING" "ripgrep failed while scanning for v6.6.${RG_ERROR:+ $RG_ERROR}"
fi
if ((RG_EXIT_CODE != 0 || ${#RG_LINES[@]} == 0)); then
  add_result "compat-v6.6-present" "FAIL" "0 hits"
  exit_with "$EXIT_MISSING_V66" "Expected compatibility label v6.6 was not found."
fi
write_hits "v6.6 occurrences" "${RG_LINES[@]}"
add_result "compat-v6.6-present" "PASS" "${#RG_LINES[@]} hits"

declare -a invalid_v66=()
for line in "${RG_LINES[@]}"; do
  normalized=${line#./}
  normalized=${normalized//\\//}
  case "$normalized" in
    app/src/main/assets/model_manifest.json:*|\
    app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/provider/AiCoreTodoProvider.kt:*|\
    app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/sessionfactory/AiCoreTodoSessionFactory.kt:*)
      ;;
    *)
      invalid_v66+=("$line")
      ;;
  esac
done

if ((${#invalid_v66[@]} > 0)); then
  write_hits "v6.6 outside allowed scope" "${invalid_v66[@]}"
  add_result "compat-v6.6-scope" "FAIL" "${#invalid_v66[@]} out-of-scope hit(s)"
  exit_with "$EXIT_V66_OUT_OF_SCOPE" "Unexpected v6.6 usage found outside the allowed compatibility scope."
fi

write_pass "v6.6 usage is limited to the allowed compatibility scope."
add_result "compat-v6.6-scope" "PASS" "all hits inside allowed files"

write_info "Verification completed successfully."
emit_summary "$EXIT_OK"
exit "$EXIT_OK"
