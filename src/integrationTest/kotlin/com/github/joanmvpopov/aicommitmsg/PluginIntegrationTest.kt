package com.github.joanmvpopov.aicommitmsg

import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes
import org.junit.jupiter.api.fail

@Remote("com.github.joanmvpopov.aicommitmsg.settings.PluginSettings",
    plugin = "com.github.joanmvpopov.aicommitmsg")
interface PluginSettingsRemote {
    fun setApiKey(key: String)
    fun getCachedApiKey(): String?
}


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

    @Test
    fun generateCommitMessageKeyAlreadySetUpFlow() {
        // derive test name from the class name
        val testName = CurrentTestMethod.hyphenateWithClass()

        val apiKey = System.getProperty("test.openrouter.api.key")
        assertFalse(apiKey.isNullOrEmpty(), "OPENROUTER_API_KEY not set")

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

                val settings = driver.service<PluginSettingsRemote>()
                settings.setApiKey(apiKey)

                val stored = settings.getCachedApiKey()
                assertEquals(apiKey, stored, "API key was not set in the IDE process")

                // add a file, stage it with git
                val projectPath = testContext.resolvedProjectHome
                projectPath.resolve("test-change.txt").toFile().writeText("integration test change")
                ProcessBuilder("git", "add", "test-change.txt")
                    .directory(projectPath.toFile())
                    .start()
                    .waitFor()

                // open commit panel
                val commitToolWindowButton = x("//div[@myaction='Commit (null)']")
                commitToolWindowButton.waitFound()
                commitToolWindowButton.click()

                // refresh VCS changes so the IDE picks up the staged file
                val refreshButton = x("//div[@myaction='Refresh (Refresh VCS changes)']")
                refreshButton.waitFound()
                refreshButton.click()

                // wait for refresh to complete
                waitForIndicators(1.minutes)

                // wait for commit message field to appear
                val commitMessage = x("//div[@class='CommitMessage']")
                commitMessage.waitFound()

                // click generate button
                val generateButton = x("//div[@myicon='toolWindowAskAI.svg']")
                generateButton.waitFound()
                generateButton.click()

                // wait for the LLM background task to finish
                waitForIndicators(3.minutes)

                // keep checking until the placeholder text is gone
                commitMessage.waitAnyTexts(timeout = 3.minutes) {
                    it.text.isNotBlank() && it.text != "Commit Message"
                }
            }
        }
    }

    @Test
    fun generateCommitMessageNoStagedFilesPopUp() {
        // derive test name from the class name
        val testName = CurrentTestMethod.hyphenateWithClass()

        val apiKey = System.getProperty("test.openrouter.api.key")
        assertFalse(apiKey.isNullOrEmpty(), "OPENROUTER_API_KEY not set")

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

                val settings = driver.service<PluginSettingsRemote>()
                settings.setApiKey(apiKey)

                val stored = settings.getCachedApiKey()
                assertEquals(apiKey, stored, "API key was not set in the IDE process")

                // open commit panel
                val commitToolWindowButton = x("//div[@myaction='Commit (null)']")
                commitToolWindowButton.waitFound()
                commitToolWindowButton.click()

                // refresh VCS changes so the IDE picks up the staged file
                val refreshButton = x("//div[@myaction='Refresh (Refresh VCS changes)']")
                refreshButton.waitFound()
                refreshButton.click()

                // wait for refresh to complete
                waitForIndicators(1.minutes)

                // wait for commit message field to appear
                val commitMessage = x("//div[@class='CommitMessage']")
                commitMessage.waitFound()

                // click generate button
                val generateButton = x("//div[@myicon='toolWindowAskAI.svg']")
                generateButton.waitFound()
                generateButton.click()

                val noChangesDialog = x("//div[@title='AI Commit Message']")
                noChangesDialog.waitFound()

                val dialogMessage = x("//div[@class='JEditorPane' and @visible_text='No changes staged for commit.']")
                dialogMessage.waitFound()

                // close the dialog
                val okButton = x("//div[@class='JButton' and @text='OK']")
                okButton.waitFound()
                okButton.click()
            }
        }
    }

    @Test
    fun generateCommitMessageKeyNotSetUpPopUp() {
        // derive test name from the class name
        val testName = CurrentTestMethod.hyphenateWithClass()

        val apiKey = System.getProperty("test.openrouter.api.key")
        assertFalse(apiKey.isNullOrEmpty(), "OPENROUTER_API_KEY not set")

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

                // add a file, stage it with git
                val projectPath = testContext.resolvedProjectHome
                projectPath.resolve("test-change.txt").toFile().writeText("integration test change")
                ProcessBuilder("git", "add", "test-change.txt")
                    .directory(projectPath.toFile())
                    .start()
                    .waitFor()

                // open commit panel
                val commitToolWindowButton = x("//div[@myaction='Commit (null)']")
                commitToolWindowButton.waitFound()
                commitToolWindowButton.click()

                // refresh VCS changes so the IDE picks up the staged file
                val refreshButton = x("//div[@myaction='Refresh (Refresh VCS changes)']")
                refreshButton.waitFound()
                refreshButton.click()

                // wait for refresh to complete
                waitForIndicators(1.minutes)

                // wait for commit message field to appear
                val commitMessage = x("//div[@class='CommitMessage']")
                commitMessage.waitFound()

                // click generate button
                val generateButton = x("//div[@myicon='toolWindowAskAI.svg']")
                generateButton.waitFound()
                generateButton.click()

                val apiKeyDialog = x("//div[@title='OpenRouter API Key Required']")
                apiKeyDialog.waitFound()

                // enter the API key
                val apiKeyField = x("//div[@class='JPasswordField']")
                apiKeyField.waitFound()
                apiKeyField.click()
                apiKeyField.keyboard { typeText(apiKey) }

                // save and trigger generation
                val saveButton = x("//div[@class='JButton' and @text='Save and Generate']")
                saveButton.waitFound()
                saveButton.click()

                // wait for the LLM background task to finish
                waitForIndicators(3.minutes)

                // keep checking until the placeholder text is gone
                commitMessage.waitAnyTexts(timeout = 3.minutes) {
                    it.text.isNotBlank() && it.text != "Commit Message"
                }
            }
        }
    }

    @Test
    fun generateCommitMessageKeyNotSetUpPopUpInputMalformedKeyExpectError() {
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

                // add a file, stage it with git
                val projectPath = testContext.resolvedProjectHome
                projectPath.resolve("test-change.txt").toFile().writeText("integration test change")
                ProcessBuilder("git", "add", "test-change.txt")
                    .directory(projectPath.toFile())
                    .start()
                    .waitFor()

                // open commit panel
                val commitToolWindowButton = x("//div[@myaction='Commit (null)']")
                commitToolWindowButton.waitFound()
                commitToolWindowButton.click()

                // refresh VCS changes so the IDE picks up the staged file
                val refreshButton = x("//div[@myaction='Refresh (Refresh VCS changes)']")
                refreshButton.waitFound()
                refreshButton.click()

                // wait for refresh to complete
                waitForIndicators(1.minutes)

                // wait for commit message field to appear
                val commitMessage = x("//div[@class='CommitMessage']")
                commitMessage.waitFound()

                // click generate button
                val generateButton = x("//div[@myicon='toolWindowAskAI.svg']")
                generateButton.waitFound()
                generateButton.click()

                val apiKeyDialog = x("//div[@title='OpenRouter API Key Required']")
                apiKeyDialog.waitFound()

                // enter the API key
                val apiKeyField = x("//div[@class='JPasswordField']")
                apiKeyField.waitFound()
                apiKeyField.click()
                apiKeyField.keyboard { typeText("malformedkey") }

                // save and trigger generation
                val saveButton = x("//div[@class='JButton' and @text='Save and Generate']")
                saveButton.waitFound()
                saveButton.click()

                val errorDialog = x("//div[@title='AI Commit Message']")
                errorDialog.waitFound()

                val errorMessage = x("//div[@class='JEditorPane' and contains(@visible_text, 'Failed to generate commit message')]")
                errorMessage.waitFound()

                val okButton = x("//div[@class='JButton' and @text='OK']")
                okButton.waitFound()
                okButton.click()

                // commit message should still be the placeholder
                commitMessage.waitAnyTexts(timeout = 1.minutes) {
                    it.text.isBlank() || it.text == "Commit Message"
                }
            }
        }
    }

    @Test
    fun generateCommitMessageGoToSettingsSetUpKeyGoToCommitGenerateFlow() {
        // derive test name from the class name
        val testName = CurrentTestMethod.hyphenateWithClass()

        val apiKey = System.getProperty("test.openrouter.api.key")
        assertFalse(apiKey.isNullOrEmpty(), "OPENROUTER_API_KEY not set")

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

                // add a file, stage it with git
                val projectPath = testContext.resolvedProjectHome
                projectPath.resolve("test-change.txt").toFile().writeText("integration test change")
                ProcessBuilder("git", "add", "test-change.txt")
                    .directory(projectPath.toFile())
                    .start()
                    .waitFor()

                openSettingsDialog()

                val settings = x("//div[@title='Settings' and @class='MyDialog']")
                settings.waitFound()

                val tree = settings.x("//div[@class='MyTree']")
                tree.waitFound()

                val toolsText = tree.waitOneText("Tools")
                toolsText.click()

                val aiCommitLink = settings.x("//div[@class='ActionLink' and @text='AI Commit Message']")
                aiCommitLink.waitFound()
                aiCommitLink.click()

                val apiKeyField = settings.x("//div[@class='JPasswordField']")
                apiKeyField.waitFound()
                apiKeyField.click()
                apiKeyField.keyboard { typeText(apiKey) }

                val southPanel = settings.x("//div[@class='SouthPanel']")
                southPanel.waitFound()
                southPanel.x("//div[@text='Apply']").click()
                southPanel.x("//div[@text='OK']").click()

                val commitToolWindowButton = x("//div[@myaction='Commit (null)']")
                commitToolWindowButton.waitFound()
                commitToolWindowButton.click()

                val refreshButton = x("//div[@myaction='Refresh (Refresh VCS changes)']")
                refreshButton.waitFound()
                refreshButton.click()

                waitForIndicators(1.minutes)

                val commitMessage = x("//div[@class='CommitMessage']")
                commitMessage.waitFound()

                val generateButton = x("//div[@myicon='toolWindowAskAI.svg']")
                generateButton.waitFound()
                generateButton.click()

                waitForIndicators(3.minutes)

                commitMessage.waitAnyTexts(timeout = 3.minutes) {
                    it.text.isNotBlank() && it.text != "Commit Message"
                }
            }
        }
    }
}