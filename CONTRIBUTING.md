# Contributing

Thank you for helping improve Privacy Kit Lite.

## Scope

Keep changes aligned with the Lite edition: local-only identifier profiles, a small Android framework UI, and modern libxposed integration. Features that require accounts, proprietary SDKs, remote services, dynamic downloads, or hidden build inputs are out of scope.

## Development

1. Use JDK 17 and Android SDK Platform 37.
2. Do not commit `local.properties`, signing keys, APKs, IDE state, or generated build output.
3. Run `./gradlew testDebugUnitTest lint assembleDebug assembleRelease` before submitting a change.
4. Explain which Android versions and target apps were tested when changing hooks.

## Pull Requests

- Keep patches focused and include tests for pure Kotlin logic where practical.
- Document new hooks and their limitations.
- Do not report a hook as successful merely because it was configured; runtime behavior must be verified in the scoped target process.
- Confirm that the merged manifest still has no network permission.

By contributing, you agree that your contribution is licensed under GPL-3.0-or-later.
