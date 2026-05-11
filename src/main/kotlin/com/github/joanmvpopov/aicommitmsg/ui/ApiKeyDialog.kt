package com.github.joanmvpopov.aicommitmsg.ui

import com.github.joanmvpopov.aicommitmsg.settings.PluginSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPasswordField

class ApiKeyDialog(project: Project) : DialogWrapper(project) {
    private val apiKeyField = JPasswordField(40)
    private val settings = service<PluginSettings>()

    init {
        title = "OpenRouter API Key Required"
        setOKButtonText("Save and Generate")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val openRouterLink = "https://openrouter.ai/openrouter/free/api"

        val explanation = JBLabel(
            "<html>To generate commit messages, you need a free OpenRouter API key.<br>" +
                    "Visit <a href=$openRouterLink>openrouter.ai/openrouter/free/api</a> to create one.</html>"
        )
        explanation.setCopyable(true)

        return FormBuilder.createFormBuilder()
            .addComponent(explanation)
            .addVerticalGap(10)
            .addLabeledComponent("API Key:", apiKeyField)
            .panel
    }

    override fun doOKAction() {
        // key is not nullable here, no need for such checks,
        // but it could be blank
        val key = String(apiKeyField.password)
        if (key.isBlank()) return
        settings.setApiKey(key)
        super.doOKAction()
    }
}