// Temporary security overrides for the Gradle plugin classpath only (not Android runtime).
// Keep Netty and Bouncy Castle modules aligned; remove these overrides when
// upstream AGP / OpenAPI Generator select fixed versions themselves.
buildscript {
    configurations.classpath {
        resolutionStrategy.eachDependency {
            val fixedVersion =
                when {
                    requested.group == "io.netty" && requested.name.startsWith("netty-") -> "4.1.140.Final"
                    requested.group == "org.bouncycastle" && requested.name.endsWith("-jdk18on") -> "1.85"
                    requested.group == "com.fasterxml.jackson.core" &&
                        requested.name in setOf("jackson-core", "jackson-databind") -> "2.22.3"
                    requested.group == "com.google.guava" && requested.name == "guava" -> "33.7.2-jre"
                    requested.group == "org.apache.commons" && requested.name == "commons-compress" -> "1.28.0"
                    requested.group == "com.github.jknack" && requested.name == "handlebars" -> "4.5.2"
                    requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" -> "0.9.6"
                    requested.group == "org.jdom" && requested.name == "jdom2" -> "2.0.6.1"
                    requested.group == "io.opentelemetry" &&
                        requested.name in setOf("opentelemetry-api", "opentelemetry-context") -> "1.67.0"
                    requested.group == "com.google.protobuf" &&
                        requested.name in setOf("protobuf-java", "protobuf-kotlin") -> "3.25.5"
                    else -> null
                }
            if (fixedVersion != null) {
                useVersion(fixedVersion)
                because("Security advisory mitigation for Gradle build plugins")
            }
        }
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.openApiGenerator) apply false
}
