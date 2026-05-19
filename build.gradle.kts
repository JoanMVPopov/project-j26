import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

// load .env file
// source: https://stackoverflow.com/questions/50509084/gradle-environment-variables-load-from-file
val dotenv = file(".env")
    .takeIf { it.exists() }
    ?.readLines()
    ?.filter { it.isNotBlank() && !it.startsWith("#") }
    ?.associate {
        val pos = it.indexOf("=")
        it.substring(0, pos).trim() to it.substring(pos + 1).trim()
    }
    ?: emptyMap()


// source: https://plugins.jetbrains.com/docs/intellij/integration-tests-intro.html#adding-dependencies
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.2.6.2")
        // including it to make use of its functionalities (e.g. diff generation)
        bundledPlugin("Git4Idea")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Starter, configurationName = "integrationTestImplementation")
    }

    integrationTestImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    integrationTestImplementation("org.kodein.di:kodein-di-jvm:7.20.2")
    integrationTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1")
    integrationTestImplementation("org.junit.platform:junit-platform-launcher")
}

val integrationTest by intellijPlatformTesting.testIdeUi.registering {
    task {
        // load the api key env
        // look for a .env config, otherwise look for exported env variable
        systemProperty("test.openrouter.api.key",
            dotenv["OPENROUTER_API_KEY"] ?: System.getenv("OPENROUTER_API_KEY") ?: "")

        val integrationTestSourceSet = sourceSets.getByName("integrationTest")
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed", "standardOut", "standardError")
        }
    }
}
