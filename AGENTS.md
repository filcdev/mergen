# Mergen Agent Guidance

## Working mode

- Read and apply [Ponytail](.agents/skills/ponytail/SKILL.md) in `full` mode for coding tasks unless the user selects another level or turns it off. Do not silently re-enable it later in the same session.
- Use [Ponytail review](.agents/skills/ponytail-review/SKILL.md) for requested reviews and to review your final diff before marking a coding PR ready. Review the connected callers, not only changed lines. The review itself reports findings without editing code.
- These Mergen-specific rules refine the generic skills. Preserve the existing architecture, required validation, and the user's full requested scope. Reply in the user's language; the review skill's English labels are examples, not a language requirement.
- Prefer the smallest complete root-cause fix. Reuse nearby components and helpers before adding abstractions, dependencies, or new configuration. Do not turn a focused fix into a broad cleanup.

## Project map

Mergen is a Kotlin Multiplatform / Compose Multiplatform app for Android and iOS.
Kotlin paths below are relative to `composeApp/src/<sourceSet>/kotlin/hu/petrik/filcapp/`.

- `commonMain`: shared UI and logic. `App.kt` wires the app; `screens/` holds screens; `components/` and `theme/` hold reusable UI and styling.
- `commonMain/auth/`: authentication and HTTP/session integration. `data/`, `admin/`, `calendar/`, and `news/` hold feature-specific API access and logic.
- `commonMain/api/`: generated OpenAPI client, models, and infrastructure.
- `commonMain/widgets/`: shared widget logic and navigation; `androidMain/widgets/`: Android providers, configuration, storage, and background work.
- `androidMain` and `iosMain`: platform entry points and implementations. Keep platform-only APIs out of `commonMain`; update both implementations when changing an `expect` declaration.
- `composeApp/src/commonTest/kotlin/hu/petrik/filcapp/`: shared tests, including permissions, authentication, selection search, and widgets.
- `iosApp/`: native iOS host. iOS validation needs macOS and Xcode.

Follow the patterns already used by the touched feature. Reuse the existing Compose components, theme, Voyager navigation, Ktor clients, and coroutine scopes rather than introducing parallel frameworks. Preserve cancellation and screen lifecycle behavior.

## Tools and validation

Use the versions pinned in [mise.toml](mise.toml); dependency/plugin versions live in [gradle/libs.versions.toml](gradle/libs.versions.toml). Run from the repository root with the mise-managed tools available on PATH.

- Setup: `mise install`; for a local SDK, `mise run sdk-components` as documented in [README.md](README.md).
- Use `gradle`, as the repository and CI do; do not invent a wrapper command or upgrade the toolchain to work around a local setup issue.
- For Kotlin, build, resource, or API-client changes, run the checks used by [.github/workflows/checks.yml](.github/workflows/checks.yml), plus the ktlint check from [.github/workflows/kotlin-quality-check.yml](.github/workflows/kotlin-quality-check.yml):

```sh
gradle :composeApp:compileDebugKotlinAndroid
gradle :composeApp:testDebugUnitTest
gradle :composeApp:lintDebug
ktlint "**/*.kt" "**/*.kts" "!**/build/**" "!composeApp/src/commonMain/kotlin/hu/petrik/filcapp/api/**"
```

- Add a focused regression test for a bug fix or non-trivial behavior change, preferably beside the existing common tests. Assert observable behavior rather than copying the implementation.
- For UI changes, also inspect the affected screen or widget on the relevant platform and provide screenshots when available. Android checks do not prove iOS compatibility; validate iOS changes on macOS/Xcode or explicitly report that limitation.
- For documentation-only changes, check paths, links, examples, and the diff; an Android build is not required.
- Report what actually ran, failures, and checks blocked by missing tools or SDKs. Do not claim a check passed if it was skipped, and do not disable checks to make a PR green.

## API contracts and generated files

- [openapi/filc-openapi.json](openapi/filc-openapi.json) is the local input to the OpenAPI Generator configured in [composeApp/build.gradle.kts](composeApp/build.gradle.kts). Keep it aligned with the intended FILC/Chronos contract.
- When the contract changes, regenerate with `gradle :composeApp:generateFilcApiClient` and review the resulting diff. The generated Kotlin client is committed; update affected handwritten callers and tests in the same change.
- Do not hand-patch `commonMain/api/` to hide a contract mismatch. Fix the contract or generator configuration as appropriate, then regenerate. Do not guess endpoint paths, payloads, authentication, or permission semantics.
- The generator writes into `composeApp/`; inspect every changed file and exclude unrelated generated scaffolding. Preserve intentional handwritten build configuration.
- Do not edit or commit build outputs, generated Compose resources, or `MergenEnvironment.kt` under `build/`.

## Authentication, environments, and releases

- Reuse `auth/` and its platform secure-storage implementations. Preserve session handling, logout cleanup, PKCE, and server-side authorization assumptions. UI visibility checks are not server authorization.
- Never log or commit credentials, session tokens, signing material, or local environment files. Use the tracked `.env.example` and the profile definitions in `composeApp/build.gradle.kts` as setup references.
- Keep local and production profiles separate as enforced by `composeApp/build.gradle.kts`. Do not bypass production-origin/profile checks or enable release cleartext traffic to get a build working.
- Preserve the existing `mergenVersionCode` / `mergenVersionName` properties and release workflow version resolution. Do not hardcode a release version as a build fix.
- Release publication and signing changes need explicit task scope; routine validation uses the debug checks above.

## Before handing off

- Inspect the complete diff, including generated files and all affected Android/iOS callers.
- Run the applicable checks and the Ponytail review; fix confirmed issues within the task scope.
- Follow [.github/pull_request_template.md](.github/pull_request_template.md): explain the problem, resulting behavior, and actual verification. Mark irrelevant checks as not applicable rather than passed.
