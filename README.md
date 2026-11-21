# Folio — Private Local-First Android eBook Reader

<p align="center">
  <img src="src/assets/images/folio_app_icon_1795182333982.jpg" alt="Folio App Icon" width="96" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 7.0+" />
  <img src="https://img.shields.io/badge/Network-100%25%20Offline-4CAF50" alt="100% Offline" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License" />
  <a href="https://github.com/sigma67auralpha-spec/Folio-eBook-Reader/releases/latest">
    <img src="https://img.shields.io/github/v/release/sigma67auralpha-spec/Folio-eBook-Reader?label=Download%20APK&color=orange" alt="Download Latest APK" />
  </a>
</p>

**Folio** is a personal, 100% offline Android eBook reader and library built with **Kotlin** and **Jetpack Compose (Material Design 3)**. There are no accounts, no ads, no analytics, and no internet permissions — everything stays on your device.

## Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/sigma67auralpha-spec/Folio-eBook-Reader/main/screenshots/library.jpg" alt="Folio Library Screen" width="30%" />
  <img src="https://raw.githubusercontent.com/sigma67auralpha-spec/Folio-eBook-Reader/main/screenshots/stats.jpg" alt="Folio Reading Journal & Streak Heatmap" width="30%" />
  <img src="https://raw.githubusercontent.com/sigma67auralpha-spec/Folio-eBook-Reader/main/screenshots/settings.jpg" alt="Folio Personal Settings & Modular Toggles" width="30%" />
</p>

## Supported Formats (12)

- **Reflowable eBooks & Documents:** `EPUB`, `MOBI`, `AZW3`, `FB2` (`.fb2` & `.fb2.zip`), `DOCX`, `RTF`, `TXT`, `HTML`, `Markdown` (`.md`)
- **Fixed-Layout & Comics:** `PDF` (native `PdfRenderer`), `CBZ`, `CBR` (comic archive reader with natural page ordering)

## Key Features

- **Personal Library & Shelves:** Custom collections/shelves, tags, search, sorting, layout density controls (`Compact`, `Comfortable`, `Large`), and watched local folders via Storage Access Framework.
- **Customizable Reader:** 5 reading themes (`Paper`, `Sepia`, `Slate`, `Night`, `AMOLED`), 7 app accent palettes, adjustable font size, line height, letter spacing, margins, paragraph spacing, alignment, bundled Google Fonts + custom `.ttf`/`.otf` font import, brightness slider, and screen-awake toggle.
- **Modular On/Off Controls:** Toggle individual UI & reader features on or off (`Continue Reading` shelf, progress bars, format badges, reader footer, highlights & notes, optional on-device Text-to-Speech, tap zones, volume-key page turns).
- **Highlights, Bookmarks & Markdown Export:** Colored highlights with notes and one-tap Markdown export.
- **Reading Journal & Heatmap Streak:** Multi-month calendar contribution heatmap (`60+` min/day intensity levels), daily goals, active/best streaks, and local reading session tracking.
- **Local Backup & Restore:** Full `.zip` library backup (metadata, shelves, highlights, reading sessions, and covers) exported to user-chosen storage.

## Tech Stack

- **Language:** Kotlin (JVM 11)
- **UI:** Jetpack Compose + Material 3 (`enableEdgeToEdge()`)
- **Persistence:** Room (SQLite) + Jetpack DataStore Preferences
- **Images:** Coil Compose
- **Architecture:** MVVM with `ViewModel`, `StateFlow`, and type-safe Navigation Compose

## Privacy & Security

- **Zero network permissions** (`INTERNET` is not requested in `AndroidManifest.xml`).
- **Cloud Backup disabled** (`android:allowBackup="false"`, plus explicit `<exclude>` rules in `backup_rules.xml` and `data_extraction_rules.xml`).
- **Hardened archive & document parsers** with path-traversal confinement, bounded archive decompression, safe XML parser flags, and font magic-header checks. See [`SECURITY.md`](SECURITY.md) for full details.

## Building & Testing

### Prerequisites

- JDK 17+
- Android SDK (API 36)
- Gradle `9.3.1` (or Android Studio)

### Local Build

1. Generate a local debug keystore (ignored by Git) if you do not already have `debug.keystore` in the project root:

   ```bash
   keytool -genkeypair -v -keystore debug.keystore \
     -storepass android -alias androiddebugkey -keypass android \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=Android Debug,O=Android,C=US"
   ```

2. Run unit & Robolectric tests and assemble the debug APK:

   ```bash
   gradle :app:testDebugUnitTest :app:assembleDebug
   ```

### Release Signing (Environment Variables Only)

Release signing reads credentials exclusively from environment variables — never commit keystores or `.env` files:

- `KEYSTORE_PATH` (defaults to `./my-upload-key.jks`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`

## License

Licensed under the [MIT License](LICENSE).
