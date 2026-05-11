# AI Commit Message

An IntelliJ IDEA plugin that generates commit messages from staged git diffs using OpenRouter's free LLM models.

The plugin is not available through the Marketplace, but you can build it yourself using:
```sh
./gradlew runIde
```

Check the `.run/` folder, alongside the `build.gradle.kts` settings in order to understand the setup

## Example Flow



https://github.com/user-attachments/assets/0353c733-3cd7-430d-ade4-e53169fa8145



## Features

- **One-click commit message generation** - click the AI button in the commit panel to generate a message from your staged changes
- **First-run API key prompt** - if no API key is configured, a dialog prompts you to enter one before generating
- **Settings page** - configure your OpenRouter API key and model under Settings > Tools > AI Commit Message
- **Progress indicator** - a background progress bar shows while the LLM generates your message
- **Free by default** - uses `openrouter/free`, which routes to the best available free model

## Configuration

Go to **Settings > Tools > AI Commit Message** to configure:

- **OpenRouter API Key** - stored securely via IntelliJ's PasswordSafe
- **Model Name** - defaults to `openrouter/free`. You can set a specific model (e.g. `meta-llama/llama-4-scout:free`)

## Architecture

| Component | Description |
|---|---|
| `GenerateCommitAction` | Action registered in `Vcs.MessageActionGroup`. Computes staged diff via Git4Idea, sends to LLM, sets commit message |
| `LlmService` | Application-level service. Sends diffs to OpenRouter's API using JDK 21 HttpClient + Gson |
| `PluginSettings` | Application-level `SimplePersistentStateComponent` for model name, PasswordSafe for API key |
| `PluginSettingsConfigurable` | Settings UI under Tools > AI Commit Message |
| `ApiKeyPreloader` | `ProjectActivity` that loads the API key from PasswordSafe on a background thread at startup |
| `ApiKeyDialog` | `DialogWrapper` prompting for API key when not configured |

## Testing

### Unit Tests

```sh
./gradlew check
```

Tests cover LlmService (response parsing, request body construction) and PluginSettings (defaults, API key caching).

Integration Tests

```sh
./gradlew integrationTest
```

Requires an OPENROUTER_API_KEY - set it in a .env file at the project root or export it as an environment variable.

Integration tests use the IntelliJ Starter/Driver framework (https://plugins.jetbrains.com/docs/intellij/integration-tests-intro.html) to launch a real IDE instance with the plugin installed and interact with the UI.

Test coverage:

- Commit message generation with API key pre-configured
- "No changes staged" error dialog when no files are staged
- API key prompt dialog on first use, followed by generation
- Invalid API key error handling 
- Settings-based key configuration followed by generation

▎ Note: Integration tests run in a two-process architecture (test JVM + IDE JVM), so only unit tests contribute to code coverage reports.

Building

```ss
./gradlew buildPlugin
```

The plugin ZIP is output to build/distributions/.

Requirements

- IntelliJ IDEA 2025.2+
- Java 21+
