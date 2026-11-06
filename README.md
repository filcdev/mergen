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


### AI-assisted development

Start with [AGENTS.md](AGENTS.md) for Mergen's architecture, generated API rules,
and validation commands. It asks agents to use [Ponytail](.agents/skills/ponytail/SKILL.md)
in `full` mode and [Ponytail review](.agents/skills/ponytail-review/SKILL.md) before
handing off a coding change.

For example, ask: "Use Ponytail full to fix this bug" or
"Use ponytail-review to review this branch against main."
You can request `lite`, `ultra`, or `off`; the default is `full`.
Skill discovery and command syntax depend on the agent. If your tool does not
discover `.agents/skills` or read `AGENTS.md` automatically, explicitly ask it to
read those files. These are repository instructions, not an automatically
enforced CI review or an installed lifecycle hook.

The two skill files are vendored from
[DietrichGebert/ponytail at `96608e0`](https://github.com/DietrichGebert/ponytail/tree/96608e078b2d5ea607c5c3b8b5a54d1c1fba6846/skills).
Each includes the upstream MIT license alongside `SKILL.md`. The only local
skill-file adaptation is removing Ponytail's `argument-hint` metadata for
compatibility with skill validators; the instruction bodies are unchanged.
Mergen-specific behavior, including response language and validation, lives in
`AGENTS.md`. To update the vendored skills, review the upstream diff, replace
both skills and their licenses together, and update this pinned source reference.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
