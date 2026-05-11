<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# AI Commit Message Generator Changelog

## [Unreleased]
### Added
- AI-powered commit message generation from staged git diffs using OpenRouter LLMs
- Plugin settings page for configuring OpenRouter API key and model name
- First-run API key dialog when generating without a key configured
- Progress bar during commit message generation
- Commit panel button to trigger generation
- Unit tests for LlmService and PluginSettings
- Integration tests using IntelliJ Starter/Driver framework
- CI workflow for integration tests with xvfb

### Changed
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
