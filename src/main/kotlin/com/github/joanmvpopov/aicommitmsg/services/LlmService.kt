package com.github.joanmvpopov.aicommitmsg.services

import com.github.joanmvpopov.aicommitmsg.settings.PluginSettings
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.components.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service(Service.Level.APP)
class LlmService {
    private val httpClient = HttpClient.newHttpClient()
    private val settings = service<PluginSettings>()

    fun generateCommitMessage(diff: String) : String {
        /**
         * Checks for API key, gets request body based on diff,
         * Creates an HTTP POST request and sends it to the OpenRouter endpoint
         * Source: https://openrouter.ai/docs/api/reference/overview#requests
         */
        val apiKey = settings.cachedApiKey
        if (apiKey.isNullOrEmpty()) {
            throw IllegalStateException("API key not specified")
        }

        val requestBody = buildRequestBody(diff)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw RuntimeException("OpenRouter API error (${response.statusCode()}): ${response.body()}")
        }

        return parseResponse(response.body())

    }

    internal fun parseResponse(responseBody: String): String {
        /**
         * Receives the response body from the Open Router endpoint
         * Parses the object using Gson and checks if the required fields are present
         * Source: https://openrouter.ai/docs/api/reference/overview#requests
         */
        val json = Gson().fromJson(responseBody, JsonObject::class.java)

        val choices = json.getAsJsonArray("choices")
            ?: throw RuntimeException("OpenRouter response missing 'choices' field")

        if (choices.isEmpty) {
            throw RuntimeException("OpenRouter returned empty choices")
        }

        val message = choices[0].asJsonObject
            .getAsJsonObject("message")
            ?: throw RuntimeException("OpenRouter response missing 'message' field")

        return message.get("content").asString.trim()
    }

    internal fun buildRequestBody(diff: String) : String {
        /**
         * Builds the request body which the Open Router endpoint expects
         * Creates a JsonObject with the (primitive) fields required by the endpoint
         * Also, sets temperature to 0 to ensure consitent results
         * Source: https://openrouter.ai/docs/api/reference/overview#requests
         */
        val model = settings.state.modelName.takeUnless { it.isNullOrEmpty() } ?: "openrouter/free"
        val gson = Gson()

        val body = JsonObject().apply {
            addProperty("model", model)
            addProperty("temperature", 0)
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", "You are a commit message generator. Given a git diff, write a clear, concise commit message. Use imperative mood. No quotes or markdown. Single line")
                })
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", diff)
                })
            })
        }

        return gson.toJson(body)
    }
}
