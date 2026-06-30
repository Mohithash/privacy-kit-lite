# Privacy Kit Lite

Privacy Kit Lite is a small, local-only Android module that uses LSPosed to replace a conservative set of identifiers on a per-app basis. It is designed as a transparent, buildable open-source edition with no accounts, remote services, or proprietary runtime SDKs.

## Features

- Create, rename, duplicate, delete, and enable or disable reusable local profiles.
- Assign one or more launchable installed apps to a profile and request LSPosed scope for each app.
- Use the real value, a locally generated value, or a manually entered value for each supported identifier.
- Regenerate an entire profile without contacting a server.
- Store profiles on-device and export them through libxposed RemotePreferences.
- Scroll through long app pickers, profile selectors, and identifier lists.

Supported hooks include:

- Android ID and hostname/device name.
- Advertising ID Binder replies, without depending on Google Play Services libraries.
- Selected `android.os.Build` fields, serial, and radio version.
- Wi-Fi MAC, BSSID, and optional SSID.
- Bluetooth MAC address.
- MediaDrm device identifier.
- IMEI, MEID, subscriber ID, SIM serial number, and phone number.

## Requirements

- Android 8.0 (API 26) or newer.
- A rooted device with an LSPosed-compatible framework that supports modern libxposed API 102.
- The target app enabled in this module's LSPosed scope.

## Install And Use

1. Build and install the APK.
2. Enable Privacy Kit Lite in LSPosed.
3. Open Privacy Kit Lite and create or select a profile.
4. Choose **Add app** to assign an installed app to that profile.
5. Approve the scope request. If the request is unavailable, add the target app to the module scope in LSPosed manually.
6. Select generated or custom values, then restart the target app process.

The connection label reports only whether the app can currently reach the LSPosed service. Privacy Kit Lite does not collect runtime hook history and does not claim that a hook succeeded until the target app calls that API and you verify its result.

## Privacy

- No `INTERNET` permission.
- No telemetry, analytics, ads, crash reporting, update checks, or remote configuration.
- No account, license server, premium key, AI service, or browser component.
- Android backup is disabled so profile data is not sent through system backup transports.

Profiles remain on the device and are shared only through LSPosed's local RemotePreferences mechanism.

## Limitations

- Identifier replacement is best-effort. Apps can use native code, hardware-backed attestation, server-side correlation, undocumented APIs, or other signals that this module does not intercept.
- Some hooks depend on Android version and vendor implementation.
- This app is not a malware sandbox and does not provide complete device isolation.
- Each installed app has one active profile assignment at a time. Backup/restore, custom hook definitions, location simulation, browser isolation, and runtime history are outside the Lite scope.

## Build

Prerequisites:

- JDK 17. Android Studio's bundled JBR is suitable.
- Android SDK Platform 37 and a configured `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or local `sdk.dir`.

```bash
./gradlew testDebugUnitTest lint assembleDebug assembleRelease
```

On Windows, use `gradlew.bat`. APKs are written beneath `app/build/outputs/apk/`.

The public GitHub APK is signed with the standard Android debug signing configuration so it can be installed directly for testing. This is not a private production key and should not be treated as an authenticity guarantee. F-Droid or another distributor should build and sign releases from source with its own key.

## Source And Dependencies

The Android framework provides the UI and local storage. Runtime dependencies are Kotlin's standard library and libxposed service/interface 102.0.0 from Maven Central; the libxposed API is compile-only. See [NOTICE](NOTICE) for attribution.

## Contributing

Bug reports and focused patches are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## License

Privacy Kit Lite is licensed under GPL-3.0-or-later. See [LICENSE](LICENSE).

This project is not affiliated with or endorsed by the Android Open Source Project, Google, LSPosed, or libxposed.
