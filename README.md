# RabbitEar 📻

A modern, full-featured Android podcast player built with Jetpack Compose, Material 3, and Media3 ExoPlayer, featuring automated Cereal Mode syndication schedules, Podcasting 2.0 namespace support, real-time RMS vacuum tube level metering, and customizable themes.

---

## 🌟 Key Features & Capabilities

### 🥣 Cereal Mode Syndication Schedules
- **Oldest Mode**: Plays episodes chronologically from backlog to end, rotating across all subscribed podcasts. For serial shows (`itunes:episodeType="serial"`), respects season and episode ordering.
- **Latest Mode**: Plays the newest episode drops working backwards across subscribed feeds.
- **New Drop Sewing**: Periodically sews newly released episodes into Oldest backlog rotation without breaking chronological backlog continuity.
- **Coming Up Preview**: Displays a live preview card on `Home` of upcoming scheduled episodes in rotation.

### 📜 Slate Queue & Reruns
- **Slate Queue**: Dedicated priority queue with drag-to-reorder, "Play Next", "Add to End", and "Clear Played" actions.
- **Reruns**: Save favorite episodes for re-listening.

### 🎙️ Podcasting 2.0 Namespace & Transcripts
- **Podcasting 2.0 Tags**: Supports `<podcast:guid>`, `<podcast:funding>`, `<podcast:transcript>`, `<podcast:chapters>`, `<podcast:person>`, and `<podcast:location>`.
- **Native Transcripts**: Parses WebVTT, SRT, and TXT transcripts with active line highlighting during playback. Tapping any line seeks playback directly.
- **Exhaustive Artwork Resolver**: Multi-level fallback resolution chain trying feed/episode iTunes images, RSS `<image>`, media thumbnails, enclosures, and OpenGraph HTML scraping.

### 🎛️ Audio Processing & Stereo VU Metering
- **Real-Time RMS Vacuum Tube Meter**: Built-in 12-segment stereo level meter with decaying ghost trails and 3D glowing vacuum tube filaments, driven by 16-bit/Float PCM RMS audio sampling.
- **Voice Boost & Normalization**: Integrated `LoudnessEnhancer` audio processing for speech frequency enhancement and volume spike smoothing.
- **Configurable Skip Intervals & Skip Silence**: Configurable back/forward skip increments and silence skipping.

### 💾 Backup, Restore & Storage Access Framework (SAF)
- **SAF Storage Folder Picker**: Native Android directory chooser (`OpenDocumentTree`) for storage selection.
- **Multi-Format Backup**: Exports and auto-backs up JSON, OPML, HTML library catalog, and SQLite database files to `RabbitEarData`.
- **Atomic Import**: Instant state restoration for subscriptions and progress.

### 🎨 Themes & Design Tokens
- **Material You Dynamic Colors**: Supports Android 12+ dynamic color schemes with dark/light fallbacks.
- **Selectable Themes**: System Dynamic, Gruvbox Retro, Material Light, and Material Dark.

---

## 🛠️ Tech Stack & Architecture

- **Package Name**: `com.rabbitear.app`
- **Language & Gradle**: Kotlin 2.0.21, Gradle Version Catalog (`libs.versions.toml`), AGP 8.7.3
- **UI Framework**: Jetpack Compose, Material 3, Material Icons Extended
- **Audio Engine**: Jetpack Media3 (`MediaLibraryService`, `ExoPlayer`, `ForwardingPlayer`)
- **Database**: Room Database (Schema Export v10 with tested migrations)
- **Background Work**: WorkManager (Conditional GET feed refreshes, background downloads, daily backups)
- **Networking & Image**: OkHttp3 (User-Agent header, 50MB HTTP disk cache) & Coil

---

## 📄 License

RabbitEar is free and open-source software released under the **GNU General Public License v3.0 (GPLv3)**.
