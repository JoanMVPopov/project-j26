package com.github.joanmvpopov.aicommitmsg.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*

// source: https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html#defining-the-storage-location
// Application-level, an API key and model preference aren't project-specific
@Service(Service.Level.APP)
@State(
    name = "AiCommitSettings",
    storages = [Storage("AiCommitSettings.xml")]
)
class PluginSettings : SimplePersistentStateComponent<PluginSettings.State>(State()) {
    class State : BaseState() {
        var modelName by string("openrouter/free")
    }

    // make sure it's visible across threads
    @Volatile
    var cachedApiKey: String? = null
        // read from anywhere, set from PluginSettings
        private set

    fun loadApiKey() {
        cachedApiKey = getApiKey()
    }

    /**
     * Using PasswordSafe below for the API Key, as recommended in the docs
     * source: https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html#how-to-use
     */
    fun getApiKey(): String? {
        val attributes = CredentialAttributes(
            generateServiceName("AiCommitMessage", "openRouterApiKey")
        )
        return PasswordSafe.instance.getPassword(attributes)
    }

    fun setApiKey(key: String) {
        val attributes = CredentialAttributes(
            generateServiceName("AiCommitMessage", "openRouterApiKey")
        )
        cachedApiKey = key
        return PasswordSafe.instance.setPassword(attributes, key)
    }
}