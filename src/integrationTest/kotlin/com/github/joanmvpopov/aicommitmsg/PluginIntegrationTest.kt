package com.github.joanmvpopov.aicommitmsg

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.junit5.hyphenateWithClass
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.runner.CurrentTestMethod
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes
import org.junit.jupiter.api.fail

class PluginIntegrationTest {

    // source: https://plugins.jetbrains.com/docs/intellij/integration-tests-intro.html#catching-exceptions-from-ide
    init {
        di = DI {
            extend(di)
            bindSingleton<CIServer>(overrides = true) {
                object : CIServer by NoCIServer {
                    override fun reportTestFailure(
                        testName: String,
                        message: String,
                        details: String,
                        linkToLogs: String?
                    ) {
                        fail("$testName fails: $message.\n$details")
                    }
                }
            }
        }
    }

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
            .newContext(testName, TestCase(ide, projectInfo = GitHubProject.fromGithub(
                branchName = "main",
                repoRelativeUrl = "TeamPraxidike/CAIT.git",
                commitHash = "55f28ffb3dcc340679fa3eec4e87412b4446abce"
            )).withVersion(ideVersion))
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