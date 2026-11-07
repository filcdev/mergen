This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  gradle :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  gradle :composeApp:assembleDebug
  ```

Both require mise (`mise install` in the repo root), which pins Java, Gradle,
Ruby, ktlint and gh from `mise.toml`. Run `mise run sdk-components` once to install the Android platform AGP
compiles against.

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

### Android push notifications (FCM)

Mergen uses the existing Chronos notification API. Configure these public Android
Firebase parameters as GitHub repository Variables (or Secrets):
`MERGEN_FCM_APP_ID`, `MERGEN_FCM_SENDER_ID`, `MERGEN_FCM_API_KEY`,
`MERGEN_FCM_PROJECT_ID`. The values must match the backend Firebase project.
Do not put Firebase service-account credentials in the Android application.

For local Android builds, export those four environment variables or use
`-PmergenFcmAppId`, `-PmergenFcmSenderId`, `-PmergenFcmApiKey`,
`-PmergenFcmProjectId`. Debug builds work without them, but push is disabled.
Android release workflows fail when Firebase configuration is missing.

Chronos currently sends `data.type=notification`, with no notification category
or ID. Push taps open the inbox, where users can select substitutions. Android
13+ requires notification permission.
