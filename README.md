# On-device Privacy First App


> [![verify-version-policy](https://github.com/thakion5-hue/ondevice_privacy_first_app/actions/workflows/verify.yml/badge.svg)](https://github.com/thakion5-hue/ondevice_privacy_first_app/actions/workflows/verify.yml)

An Android app that runs privacy-sensitive AI features **fully on-device** (Gemini Nano / AICore-backed runtime, with graceful fallbacks). All personal data — messages, photos, journal drafts, receipts — is inspected locally and never uploaded.

- **Current version**: `0.6.6` (versionCode `16`)
- **Compatibility label kept**: `v6.6` — retained only in `model_manifest.json` and the AICore TODO provider/session-factory pair for runtime handshake compatibility.
- **Legacy strings cleaned**: `v6.5`, `0.6.5`, `v0.6.5` have been fully removed from cleanup scope.

## Highlights of v0.6.6
- Version strings normalized to `0.6.6` across UI + diagnostics
- 2 settings-screen labels and 2 app-settings descriptions cleaned up
- 2 diagnostic strings and 5 design comments refined
- Export-facing `providerLabel` / `factoryLabel` and `v6.6` manifest labels kept intact for downstream compatibility

See [`docs/release_notes_v0_6_6_short.md`](docs/release_notes_v0_6_6_short.md) for the store-ready short note.

## Repository layout

```
ondevice_privacy_first_app/
├── app/                          Android app module (Compose + Room + AICore bridge)
│   └── src/main/
│       ├── assets/model_manifest.json      Runtime manifest (keeps `v6.6` label)
│       ├── java/.../ai/gemininano/         Gemini Nano runtime bridge + providers
│       └── res/values/strings.xml          UI labels (normalized to 0.6.6)
├── .github/workflows/            PR / manual CI automation
│   └── verify.yml                           Runs version-policy verification in CI
├── docs/                         Release notes, sample export JSON, criteria
│   ├── INDEX.md                            Documentation index (this release)
│   ├── release_notes_v0_6_6_short.md       Short store-ready release note
│   ├── export_sample_json_criteria.md      Export JSON acceptance criteria
│   └── privacy_first_ai_log_sample_v6.json Sample export payload
├── tools/                        Repo-wide verification / maintenance scripts
│   ├── rg_verify_v0_6_6.ps1                CI-friendly ripgrep verifier for PowerShell
│   └── rg_verify_v0_6_6.sh                 CI-friendly ripgrep verifier for bash
├── build.gradle.kts / settings.gradle.kts
└── gradlew / gradlew.bat
```

## Verification (CI + local)

The release ships with a ripgrep-based verifier that guards the v0.6.6 label policy:

- No legacy `v6.5` / `0.6.5` / `v0.6.5` strings remain in cleanup scope.
- `0.6.6` is present at least once.
- `v6.6` appears **only** in the allowed compatibility files.

### Local (developer, PowerShell)

```powershell
pwsh -File .\tools\rg_verify_v0_6_6.ps1
```

### Local (developer, bash)

```bash
bash ./tools/rg_verify_v0_6_6.sh
```

### CI (silent, machine-parseable)

```powershell
pwsh -File .\tools\rg_verify_v0_6_6.ps1 -CI
```

```bash
./tools/rg_verify_v0_6_6.sh --ci
```

GitHub Actions automation is included at [`.github/workflows/verify.yml`](.github/workflows/verify.yml). It runs automatically on pull requests and pushes to `main`, and can also be triggered manually via `workflow_dispatch`.

Exit codes:

| Code | Meaning                                             |
|-----:|-----------------------------------------------------|
| `0`  | All checks passed                                    |
| `2`  | Legacy version strings still present (`v6.5` etc.)   |
| `3`  | Required `0.6.6` string missing                      |
| `4`  | Required compatibility `v6.6` label missing          |
| `5`  | `v6.6` used outside the allowed compatibility scope  |
| `10` | Tooling error (e.g. `rg` not installed)              |

## Documentation

See [`docs/INDEX.md`](docs/INDEX.md) for the full documentation index.
