# Documentation Index — v0.6.6

This index lists every document shipped inside `docs/` for the **On-device Privacy First App v0.6.6 (versionCode 16)** release, together with what each one is for and who should read it.

## Overview

| Document | Purpose | Audience |
|---|---|---|
| [`release_notes_v0_6_6_short.md`](release_notes_v0_6_6_short.md) | Short, store-ready release note. Paste-friendly for app store / distribution channels. | Release manager, PM |
| [`export_sample_json_criteria.md`](export_sample_json_criteria.md) | Acceptance criteria for the on-device export JSON payload (schema, required keys, PII rules). | QA, backend integration |
| [`privacy_first_ai_log_sample_v6.json`](privacy_first_ai_log_sample_v6.json) | Reference sample of an export JSON payload, matching the criteria above. | QA, integration partners |
| [`../README.md`](../README.md) | Project overview, layout, verification instructions. | Everyone (start here) |
| [`../tools/rg_verify_v0_6_6.ps1`](../tools/rg_verify_v0_6_6.ps1) | CI-friendly ripgrep verifier for v0.6.6 label policy in PowerShell. | CI, developers |
| [`../tools/rg_verify_v0_6_6.sh`](../tools/rg_verify_v0_6_6.sh) | CI-friendly ripgrep verifier for the same rules in bash. | Linux/macOS CI, developers |
| [`../.github/workflows/verify.yml`](../.github/workflows/verify.yml) | Pull-request / push-to-main / manual workflow that runs the verification scripts in CI mode. | CI maintainers, reviewers |

## Version label policy for this release

- **Normalized to `0.6.6`**: app version display, diagnostics, and cleanup-scope UI/comments.
- **Kept as `v6.6` on purpose** (compatibility handshake — do not rename):
  - `app/src/main/assets/model_manifest.json`
  - `app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/provider/AiCoreTodoProvider.kt`
  - `app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/sessionfactory/AiCoreTodoSessionFactory.kt`
- **Removed from cleanup scope**: `v6.5`, `0.6.5`, `v0.6.5`.

## How to verify locally

```powershell
pwsh -File ..\tools\rg_verify_v0_6_6.ps1
```

```bash
bash ../tools/rg_verify_v0_6_6.sh
```

For CI, use `-CI` / `--ci` for quiet, machine-parseable output. Exit codes are documented in [`../README.md`](../README.md). The GitHub Actions workflow is at [`../.github/workflows/verify.yml`](../.github/workflows/verify.yml).
