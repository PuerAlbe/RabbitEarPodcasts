# RabbitEar 📻

A modern, distraction-free Android podcast player built with Jetpack Compose, Material 3, and Media3 ExoPlayer, featuring automated syndication schedules and a late 1970s Gruvbox retro aesthetic.

---

## 🌟 Key Features

### 📡 Automated Syndication Schedules
- **Oldest Mode**: Plays episodes chronologically from beginning to end, rotating across all your subscribed podcasts.
- **Latest Mode**: Plays the newest episodes working backwards, rotating across all subscribed podcasts.
- **Isolated Schedule Memory**: Both modes maintain their own independent rotation position and persistence state while sharing played/progress data.
- **Background Downloads**: WorkManager automatically pre-downloads upcoming syndicated episodes for offline listening.

### 🎧 Fullscreen Player & Playback Control
- **Expandable Fullscreen Player**: Displays high-resolution episode artwork (or channel artwork fallback), episode title, and show details.
- **Playback Controls**: Play, Pause, Replay 30s, Forward 30s, and Next Episode buttons.
- **Progress Tracking**: Interactive seek bar with exact timestamps; position progress is saved to the local database upon play/pause and background intervals.
- **Background Service**: Built on Jetpack Media3 (`MediaSessionService`) with system notifications and audio focus handling.

### ⏱️ In-Memory Sleep Timer
- **Quick Setup**: Set a sleep timer up to 60 minutes using a numeric keypad interface.
- **Dynamic Badge**: Replaces the top-bar timer button with a live badge showing remaining minutes/seconds.
- **In-Memory Lifetime**: Resets to zero when paused or closed — never persisted across sessions.

### 🔍 Independent Search & Discovery
- **iTunes Directory & Direct RSS**: Search for podcasts via iTunes directory or by pasting direct RSS feed URLs.
- **Standalone Navigation**: Search page remains active with query results intact when navigating to episode details and back.

### 💾 Backup & Restore
- **Direct Backup**: Single-tap export and import of all subscriptions and played episode states to `Documents/rabbitear_settings.json`.
- **Auto-Backup**: Automatically backs up settings to the `Documents` directory every 5 minutes in the background.

### 🎨 Retro 1970s Gruvbox Aesthetic
- Styled in a late 1970s **Gruvbox** palette featuring warm creams, deep vintage oranges, forest greens, and dark charcoal surfaces.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose, Material 3, Material Icons Extended
- **Audio Engine**: Jetpack Media3 ExoPlayer & MediaSessionService
- **Database**: Room Database with Coroutines Flow
- **Background Tasks**: AndroidX WorkManager & Coroutines
- **Image Loading**: Coil
- **Network**: OkHttp3 & iTunes Search API

---

## 📄 License

RabbitEar is free and open-source software released under the **[GNU General Public License v3.0 (GPLv3)](LICENSE)**.
