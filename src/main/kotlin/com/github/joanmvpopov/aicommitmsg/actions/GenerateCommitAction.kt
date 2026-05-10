package com.github.joanmvpopov.aicommitmsg.actions

import com.github.joanmvpopov.aicommitmsg.services.LlmService
import com.github.joanmvpopov.aicommitmsg.settings.PluginSettings
import com.github.joanmvpopov.aicommitmsg.ui.ApiKeyDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.ChangeListManager
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepositoryManager

class GenerateCommitAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        //enables the button only when a project is open
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = service<PluginSettings>()

        // if the API key is null or empty
        // show the dialog ui which prompts for key
        if (settings.cachedApiKey.isNullOrEmpty()) {
            val dialog = ApiKeyDialog(project)
            // showAndGet returns true if user clicked save & generate
            if (!dialog.showAndGet()) return
        }

        val changeListManager = ChangeListManager.getInstance(project)
        val changes = changeListManager.defaultChangeList.changes

        // check if there are any changes
        if (changes.isEmpty()) {
            Messages.showInfoMessage(project, "No changes staged for commit.", "AI Commit Message")
            return
        }

        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull()
        if (repository == null) {
            Messages.showErrorDialog(project, "No Git Repository Found", "AI Commit Message")
            return
        }

        val commitMessageInterface = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating commit message...", false) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                indicator.isIndeterminate = true

                // repository!! because we know it is not null, assert it
                val handler = GitLineHandler(project, repository!!.root, GitCommand.DIFF)
                // only show staged changes with --cached
                handler.addParameters("--cached")

                // app level service so no need to pass project inside getInstance()
                val result = Git.getInstance().runCommand(handler)
                val diff = result.getOutputOrThrow()

                if (diff.isBlank()) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(project, "Staged changes produced an empty diff.", "AI Commit Message")
                    }
                    return
                }

                val llmService = service<LlmService>()
                val message = llmService.generateCommitMessage(diff)

                ApplicationManager.getApplication().invokeLater {
                    commitMessageInterface?.setCommitMessage(message)
                }
            }

            override fun onThrowable(error: Throwable) {
                Messages.showErrorDialog(project, "Failed to generate commit message: ${error.message}", "AI Commit Message")
            }
        })
    }
}