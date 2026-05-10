package com.github.joanmvpopov.aicommitmsg.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// Implement Configurable interface
// source: https://plugins.jetbrains.com/docs/intellij/settings-guide.html#the-configurable-interface
class PluginSettingsConfigurable : Configurable {
    private var panel: JPanel? = null
    private var apiKeyField: JPasswordField? = null
    private var modelNameField: JTextField? = null
    private var apiKeyModified = false
    private var settings = service<PluginSettings>()

    override fun getDisplayName() = "AI Commit Message"

    override fun createComponent(): JComponent? {
        panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(JLabel("OpenRouter API Key:"))
            apiKeyField = JPasswordField(40).also {
                it.maximumSize = Dimension(Int.MAX_VALUE, it.preferredSize.height)
                it.document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent) { apiKeyModified = true }
                    override fun removeUpdate(e: DocumentEvent) { apiKeyModified = true }
                    override fun changedUpdate(e: DocumentEvent) { apiKeyModified = true }
                })
                add(it)
            }

            add(Box.createVerticalStrut(10))

            add(JLabel("Model Name:"))
            modelNameField = JTextField(40).also {
                it.maximumSize = Dimension(Int.MAX_VALUE, it.preferredSize.height)
                add(it)
            }
        }

        return panel
    }

    override fun isModified(): Boolean {
        val currentModel = modelNameField?.text ?: ""
        val storedModel = settings.state.modelName ?: ""
        return apiKeyModified || currentModel != storedModel
    }

    override fun apply() {
        if (apiKeyModified) {
            val key = String(apiKeyField?.password ?: charArrayOf())
            settings.setApiKey(key)
            apiKeyModified = false
        }
//        settings.state.modelName = modelNameField?.text ?: ""
        val modelName = modelNameField?.text
        if (modelName.isNullOrEmpty()) {
            settings.state.modelName = "openrouter/free"
            modelNameField?.text = "openrouter/free"
            Messages.showInfoMessage("Model name was empty. Reset to default: openrouter/free", "AI Commit Message")
        }
        settings.state.modelName = modelName
    }

    override fun reset() {
        apiKeyModified = false
        apiKeyField?.text = settings.cachedApiKey ?: ""
        apiKeyModified = false
        modelNameField?.text = settings.state.modelName ?: ""
    }

    override fun disposeUIResources() {
        panel = null
        apiKeyField = null
        modelNameField = null
    }
}