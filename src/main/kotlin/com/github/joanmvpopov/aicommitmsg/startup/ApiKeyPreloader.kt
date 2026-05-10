package com.github.joanmvpopov.aicommitmsg.startup

import com.github.joanmvpopov.aicommitmsg.settings.PluginSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ApiKeyPreloader : ProjectActivity {
    /**
     * ProjectActivity's execution runs on a background thread automatically
     * By the time the user opens Settings, the key is already cached.
     */
    override suspend fun execute(project: Project) {
        service<PluginSettings>().loadApiKey()
    }
}