package com.github.joanmvpopov.aicommitmsg.settings

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PluginSettingsTest : BasePlatformTestCase() {

    private lateinit var settings: PluginSettings

    override fun setUp() {
        super.setUp()
        settings = service<PluginSettings>()
    }

    fun testDefaultModelName() {
        assertEquals("openrouter/free", settings.state.modelName)
    }

    fun testCachedApiKeyIsNullByDefault() {
        assertNull(settings.cachedApiKey)
    }

    fun testSetApiKeyUpdatesCachedKey() {
        settings.setApiKey("test-key-123")
        assertEquals("test-key-123", settings.cachedApiKey)
    }

    fun testLoadApiKeyPopulatesCache() {
        settings.setApiKey("loaded-key")
        settings.loadApiKey()
        assertEquals("loaded-key", settings.cachedApiKey)
    }
}