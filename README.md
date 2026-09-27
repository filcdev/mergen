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
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
## Regenerating the Chronos API client

`composeApp/src/commonMain/kotlin/hu/petrik/filcapp/api` is generated from `openapi/filc-openapi.json`, a verbatim copy of the OpenAPI document Chronos serves (`/api/doc/openapi.json`). No per-consumer transform: generate it in the filc monorepo with `bun run openapi:generate` in `apps/chronos` and copy the file.

```bash
cp <filc-checkout>/apps/chronos/openapi/chronos-openapi.json openapi/filc-openapi.json
JAVA_HOME=<jdk-21> ./gradlew generateFilcApiClient
JAVA_HOME=<jdk-21> ./gradlew :composeApp:compileDebugKotlinAndroid
```

Generation needs JDK 17-21 (the plugin rejects JDK 8 and 25). The task normalises two openapi-generator template defects in a `doLast` step: models got a duplicated, non-repeatable `@Serializable`, and `kotlin.Any?` properties were emitted without the `@Contextual` serializer they need.
