# F-Droid Preparation Notes

These notes describe the source tree; they are not an F-Droid submission or build recipe.

## Application

- Application ID: `com.sal.privacykit.lite`
- Name: Privacy Kit Lite
- Version name: `0.3.0`
- Version code: `3`
- Minimum SDK: 26
- Target SDK: 37
- Compile SDK: 37
- License: GPL-3.0-or-later

The app requires a compatible LSPosed framework to perform its primary function. Any eventual F-Droid metadata should declare the applicable framework/dependency anti-feature or requirement according to the policy in effect when submitted.

## Build Inputs

- Gradle Wrapper 9.4.1
- Android Gradle Plugin 9.2.1
- JDK 17
- libxposed API/service 102.0.0 from Maven Central
- Android SDK Platform 37

No private signing material, proprietary Maven repository, opaque prebuilt application binary, or machine-specific SDK path is committed. The Gradle release build uses Android's standard debug signing configuration only to make GitHub APK assets installable; F-Droid should ignore that trust model and sign its own reproducible build.

## Privacy And Network

- The merged application manifest does not request `android.permission.INTERNET`.
- The app contains no analytics, ads, telemetry, crash reporter, update checker, remote configuration, account system, or backend client.
- Profile generation and persistence are local.
- Android system backup is disabled.

## Suggested Pre-Submission Checks

```bash
./gradlew clean testDebugUnitTest lint assembleRelease
./gradlew :app:dependencies
```

Inspect the merged release manifest and APK permissions, then create the corresponding `fdroiddata` metadata only when the first tagged source release is ready. F-Droid's build infrastructure should build and sign the APK directly from that tag.
