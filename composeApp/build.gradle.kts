import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.openApiGenerator)
}

// ---------------------------------------------------------------------------
// OpenAPI client generation
//
// Run `./gradlew generateFilcApiClient` to regenerate the API client
// whenever openapi/filc-openapi.json is updated. The generated sources are
// committed to version control so the project compiles without running the
// generator first.
// ---------------------------------------------------------------------------
val generateFilcApiClient by tasks.registering(GenerateTask::class) {
    generatorName.set("kotlin")
    library.set("multiplatform")
    inputSpec.set(rootProject.file("openapi/filc-openapi.json").absolutePath)
    outputDir.set(projectDir.absolutePath)
    packageName.set("hu.petrik.filcapp.api")
    apiPackage.set("hu.petrik.filcapp.api.client")
    modelPackage.set("hu.petrik.filcapp.api.model")
    configOptions.set(
        mapOf(
            "dateLibrary" to "string",
            "serializationLibrary" to "kotlinx_serialization",
            "omitGradleWrapper" to "true",
            "omitGradlePluginVersions" to "true",
        ),
    )
    notCompatibleWithConfigurationCache(
        "OpenAPI Generator Gradle plugin " +
            "(org.openapi.generator) does not yet support " +
            "Gradle configuration cache. " +
            "See https://github.com/OpenAPITools/openapi-generator/" +
            "issues/13113",
    )
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.play.app.update)
            // Chrome Custom Tabs, used to open the Entra sign-in page in the system browser.
            implementation(libs.androidx.customtabs)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation("cafe.adriel.voyager:voyager-navigator:1.0.0")
            implementation("cafe.adriel.voyager:voyager-tab-navigator:1.0.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

            // Ktor HTTP client (multiplatform)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // JSON serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
                // Keychain-backed credential storage (SecureStore.ios.kt).
                implementation(libs.multiplatform.settings)
            }
        }
    }
}

android {
    namespace = "hu.petrik.filcapp"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "hu.petrik.filcapp"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = providers.gradleProperty("mergenVersionCode").orElse("1").get().toInt()
        versionName = providers.gradleProperty("mergenVersionName").orElse("1.0").get()
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies { debugImplementation(compose.uiTooling) }

// Mergen environment profiles: local for development, production for release.
// The mobile application contains only public Entra identifiers (never a client secret).
// Shared Kotlin source is generated from the selected profile into build/.
// An Android release task always selects production; it cannot use local .env.
abstract class GenerateMergenEnvironment : DefaultTask() {
    @get:Input abstract val buildProfile: Property<String>

    @get:Input abstract val apiOrigin: Property<String>

    @get:Input abstract val entraClientId: Property<String>

    @get:Input abstract val entraTenantId: Property<String>

    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val profile = buildProfile.get()
        require(profile == "local" || profile == "production") {
            "Invalid -PmergenProfile value: $profile (allowed: local, production)"
        }

        val origin = apiOrigin.get().trim().trimEnd('/')
        val clientId = entraClientId.get().trim()
        val tenantId = entraTenantId.get().trim()
        require(origin.matches(Regex("https?://[^/]+"))) {
            "API origin must look like https://filc.petrik.hu or http://10.0.2.2:3001"
        }
        if (profile == "production") {
            require(origin == "https://filc.petrik.hu") {
                "Production build requires https://filc.petrik.hu (not a local backend)"
            }
            val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
            require(uuid.matches(clientId) && uuid.matches(tenantId)) {
                "Production Entra IDs are missing/invalid. " +
                    "Set MERGEN_RELEASE_ENTRA_CLIENT_ID and MERGEN_RELEASE_ENTRA_TENANT_ID " +
                    "in .env.production or CI environment."
            }
        }

        fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

        val packageDir = outputDirectory.get().asFile.resolve("hu/petrik/filcapp/config")
        packageDir.mkdirs()
        packageDir.resolve("MergenEnvironment.kt").writeText(
            """
            package hu.petrik.filcapp.config

            /** Generated build settings. Never edit or commit this file. */
            object MergenEnvironment {
                const val API_ORIGIN = ${quoted(origin)}
                const val API_BASE_URL = ${quoted("$origin/api")}
                const val ENTRA_CLIENT_ID = ${quoted(clientId)}
                const val ENTRA_TENANT_ID = ${quoted(tenantId)}
            }
            """.trimIndent() + "\n",
            Charsets.UTF_8,
        )
    }
}

// Android release tasks (including bundleRelease) force the production profile.
// For iOS release builds, explicitly pass -PmergenProfile=production.
val mergenReleaseRequested =
    gradle.startParameter.taskNames.any { requested ->
        val task = requested.substringAfterLast(':')
        task.contains("Release", ignoreCase = true) ||
            task in setOf("assemble", "bundle", "build", "publish")
    }
val mergenProfile =
    providers.gradleProperty("mergenProfile")
        .orElse(if (mergenReleaseRequested) "production" else "local").get()
require(mergenProfile == "local" || mergenProfile == "production") {
    "Invalid -PmergenProfile=$mergenProfile (allowed: local, production)"
}
if (mergenReleaseRequested && mergenProfile != "production") {
    throw GradleException("Refusing to build a release with -PmergenProfile=local")
}

fun mergenEnvFile(fileName: String): Provider<Map<String, String>> =
    providers.fileContents(rootProject.layout.projectDirectory.file(fileName))
        .asText.orElse("").map { content ->
            content.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
                .associate { line ->
                    val key = line.substringBefore('=').trim()
                    val value =
                        line.substringAfter('=').trim()
                            .removeSurrounding("\"").removeSurrounding("'")
                    key to value
                }
        }

// Never read the local .env when compiling a production variant.
val mergenActiveEnv = mergenEnvFile(if (mergenProfile == "production") ".env.production" else ".env")

fun mergenSetting(
    name: String,
    fallback: String,
): Provider<String> {
    val key = if (mergenProfile == "production") "MERGEN_RELEASE_$name" else "MERGEN_$name"
    return providers.environmentVariable(key).orElse(
        mergenActiveEnv.map { values -> values[key] ?: fallback },
    )
}

val generateMergenEnvironment by tasks.registering(GenerateMergenEnvironment::class) {
    buildProfile.set(mergenProfile)
    apiOrigin.set(mergenSetting("API_ORIGIN", "https://filc.petrik.hu"))
    entraClientId.set(mergenSetting("ENTRA_CLIENT_ID", ""))
    entraTenantId.set(mergenSetting("ENTRA_TENANT_ID", ""))
    outputDirectory.set(layout.buildDirectory.dir("generated/mergenEnvironment/commonMain/kotlin"))
}
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateMergenEnvironment)

// Safety net: a release task reached indirectly (e.g. through another task)
// must also fail rather than compile with the local profile.
val verifyMergenProductionProfile by tasks.registering {
    dependsOn(generateMergenEnvironment)
    doLast {
        require(mergenProfile == "production") {
            "A release task cannot use the local Mergen profile. Use -PmergenProfile=production."
        }
    }
}
tasks.matching { it.name.contains("Release", ignoreCase = true) }.configureEach {
    dependsOn(verifyMergenProductionProfile)
}

android {
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["mergenCleartext"] = "true"
        }
        getByName("release") {
            manifestPlaceholders["mergenCleartext"] = "false"
        }
    }
}
