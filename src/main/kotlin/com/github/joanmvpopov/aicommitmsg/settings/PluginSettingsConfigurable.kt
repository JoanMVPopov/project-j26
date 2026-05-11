package com.github.joanmvpopov.aicommitmsg.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
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

//    override fun createComponent(): JComponent? {
//        panel = JPanel().apply {
//            layout = BoxLayout(this, BoxLayout.Y_AXIS)
//
//            add(JLabel("OpenRouter API Key:"))
//            apiKeyField = JPasswordField(40).also {
//                it.maximumSize = Dimension(Int.MAX_VALUE, it.preferredSize.height)
//                it.document.addDocumentListener(object : DocumentListener {
//                    override fun insertUpdate(e: DocumentEvent) { apiKeyModified = true }
//                    override fun removeUpdate(e: DocumentEvent) { apiKeyModified = true }
//                    override fun changedUpdate(e: DocumentEvent) { apiKeyModified = true }
//                })
//                add(it)
//            }
//
//            add(Box.createVerticalStrut(10))
//
//            add(JLabel("Model Name:"))
//            modelNameField = JTextField(40).also {
//                it.maximumSize = Dimension(Int.MAX_VALUE, it.preferredSize.height)
//                add(it)
//            }
//        }
//
//        return panel
//    }

    override fun createComponent(): JComponent? {
        apiKeyField = JPasswordField(40).apply {
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) { apiKeyModified = true }
                override fun removeUpdate(e: DocumentEvent) { apiKeyModified = true }
                override fun changedUpdate(e: DocumentEvent) { apiKeyModified = true }
            })
        }

        modelNameField = JTextField(40)

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("OpenRouter API Key:", apiKeyField!!)
            .addComponentToRightColumn(
                JBLabel("<html><small>Create a free key at <a href='https://openrouter.ai/openrouter/free/api'>openrouter.ai/openrouter/free/api</a></small></html>").apply {
                    setCopyable(true)
                }
            )
            .addVerticalGap(10)
            .addLabeledComponent("Model Name:", modelNameField!!)
            .addComponentToRightColumn(
                JBLabel("<html><small>The OpenRouter model to use. Leave empty for default (openrouter/free),<br>which reroutes your request to the best available model, without incurring costs.<br>" +
                        "You can also view all models <a href='https://openrouter.ai/models'>here</a></small></html>")
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel

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

        val modelName = modelNameField?.text
        if (modelName.isNullOrEmpty()) {
            settings.state.modelName = "openrouter/free"
            modelNameField?.text = "openrouter/free"
            Messages.showInfoMessage("Model name was empty. Reset to default: openrouter/free", "AI Commit Message")
        }
        else {
            settings.state.modelName = modelName
        }
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