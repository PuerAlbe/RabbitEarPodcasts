# Changelog

## [1.0.0] - 2026-03-31

### Added
- **Package Migration**: Migrated package namespace to `com.rabbitear.app`.
- **Gradle Version Catalog**: Migrated build dependencies to `gradle/libs.versions.toml`.
- **Material You & Themes**: Added dynamic color schemes on Android 12+, Gruvbox retro theme, Material Light, and Material Dark settings options.
- **Podcasting 2.0 Support**: Added support for `<podcast:guid>`, `<podcast:funding>`, `<podcast:transcript>`, `<podcast:chapters>`, `<podcast:person>`, and `<podcast:location>`.
- **Native Transcripts**: Interactive WebVTT, SRT, and TXT transcript reader with real-time word/line highlights and seek-on-tap.
- **Vacuum Tube RMS Stereo Meter**: Added a 12-segment vacuum tube stereo level meter with decaying ghost trails driven by PCM RMS audio sampling.
- **Storage Access Framework (SAF)**: Native folder picker, JSON/OPML/HTML/DB export, and import file picker.
- **WorkManager Auto-Backup**: Periodic 24-hour backup worker saving state to `RabbitEarData`.
- **Cereal Mode & Slate Queue**: Serial show ordering support, Slate queue reordering, "Play Next", "Add to End", and "Clear Played".
- **Skip Confirmation**: Added "Mark as played?" confirmation dialog with Yes/No buttons when skipping episodes.
- **R8 Release Optimization**: Release build configuration with R8 code minification, resource shrinking, and ProGuard keep rules.
