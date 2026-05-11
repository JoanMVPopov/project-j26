package com.github.joanmvpopov.aicommitmsg

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.junit5.hyphenateWithClass
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.CurrentTestMethod
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

class PluginIntegrationTest {

    @Test
    fun pluginLoadsWithoutErrors() {
        // derive test name from the class name
        val testName = CurrentTestMethod.hyphenateWithClass()

        // match gradle build settings
        val ideVersion = "2025.2.6.2"

        val ide = IdeInfo(
            productCode = "IC",
            platformPrefix = "Idea",
            executableFileName = "idea",
            fullName = "IntelliJ IDEA Community Edition",
            version = ideVersion
        )

        val testContext = Starter
            .newContext(testName, TestCase(ide, projectInfo = NoProject).withVersion(ideVersion))
            .apply {
                // install the plugin
                // source: https://plugins.jetbrains.com/docs/intellij/integration-tests-intro.html#creating-the-first-integration-test
                val pathToPlugin = System.getProperty("path.to.build.plugin")
                PluginConfigurator(this).installPluginFromPath(Path.of(pathToPlugin))
            }

        testContext.runIdeWithDriver().useDriverAndCloseIde {
            ideFrame {
                waitForIndicators(3.minutes)
            }
        }
    }
}