package com.github.joanmvpopov.aicommitmsg.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class LlmServiceTest : BasePlatformTestCase() {
    // lateinit otherwise it might crash, set it up later
    private lateinit var llmService: LlmService

    override fun setUp(){
        super.setUp()
        llmService = service<LlmService>()
    }

    fun testParseResponseExtractsMessage() {
        val json = """
          {
              "choices": [
                  {
                      "message": {
                          "content": "Add user authentication module"
                      }
                  }
              ]
          }
      """.trimIndent()

        assertEquals("Add user authentication module", llmService.parseResponse(json))
    }

    fun testParseResponseTrimsWhitespace() {
        val json = """
          {
              "choices": [
                  {
                      "message": {
                          "content": "  Fix login bug  "
                      }
                  }
              ]
          }
      """.trimIndent()

        // expect the service to trim the content
        assertEquals("Fix login bug", llmService.parseResponse(json))
    }

    fun testParseResponseThrowsOnMissingChoices() {
        assertThrows(RuntimeException::class.java) {
            llmService.parseResponse("{}")
        }
    }

    fun testParseResponseThrowsOnEmptyChoices() {
        assertThrows(RuntimeException::class.java) {
            llmService.parseResponse("""{"choices": []}""")
        }
    }

    fun testParseResponseThrowsOnMissingMessage() {
        assertThrows(RuntimeException::class.java) {
            llmService.parseResponse("""{"choices": [{"index": 0}]}""")
        }
    }

    fun testBuildRequestBodyContainsModel() {
        val json = llmService.buildRequestBody("some diff")
        val body = Gson().fromJson(json, JsonObject::class.java)

        // expect the default value
        assertEquals("openrouter/free", body.get("model").asString)
    }

    fun testBuildRequestBodyContainsTemperatureZero() {
        val json = llmService.buildRequestBody("some diff")
        val body = Gson().fromJson(json, JsonObject::class.java)

        // expect temperature to always be 0
        assertEquals(0, body.get("temperature").asInt)
    }

    fun testBuildRequestBodyContainsDiffInUserMessage() {
        val diff = "+ added line\n- removed line"
        val json = llmService.buildRequestBody(diff)
        val body = Gson().fromJson(json, JsonObject::class.java)

        val messages = body.getAsJsonArray("messages")
        // 0 is the system prompt, 1 is the user msg
        val userMessage = messages[1].asJsonObject

        assertEquals("user", userMessage.get("role").asString)
        assertEquals(diff, userMessage.get("content").asString)
    }

    fun testBuildRequestBodyContainsSystemMessage() {
        val json = llmService.buildRequestBody("some diff")
        val body = Gson().fromJson(json, JsonObject::class.java)

        val messages = body.getAsJsonArray("messages")
        // 0 is the system prompt, 1 is the user msg
        val systemMessage = messages[0].asJsonObject

        val systemPrompt = "You are a commit message generator. Given a git diff, write a clear, concise commit message. " +
                "Use imperative mood. No quotes or markdown. Single line"

        assertEquals("system", systemMessage.get("role").asString)
        assertEquals(systemPrompt, systemMessage.get("content").asString)
    }

    fun testGenerateCommitMessageThrowsWithoutApiKey() {
        // for unit tests, no key is set, so we exit during the first if
        // the wiring within this method will be tested via integration tests
        assertThrows(IllegalStateException::class.java) {
            llmService.generateCommitMessage("some diff")
        }
    }
}