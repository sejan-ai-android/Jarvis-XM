# 🤖 J.A.R.V.I.S. Android AI Assistant

An intelligent, proactive personal AI assistant for Android inspired by Tony Stark's J.A.R.V.I.S., built with modern Kotlin, Jetpack Compose, Clean Architecture, and on-device hardware-backed cryptographic security.

---

## ⚡ Architecture & Tech Stack

- **Platform:** Android (minSdk 26, targetSdk 34)
- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Clean Architecture (Domain / Data / UI)
- **AI Core:** Google Gemini (1.5 Flash + Pro, 2.0/2.5) with local fallback
- **Hotword Engine:** Porcupine Wake Word (Picovoice)
- **Local Persistence:** Room Database (Conversations & Message Logs)
- **Preferences:** Jetpack DataStore (Preferences)
- **Hardware-Backed Key Vault:** `EncryptedSharedPreferences` (Jetpack Security Crypto AES-256-GCM)
- **Networking:** Retrofit + OkHttp + Moshi

---

## 🔒 Security Architecture: Zero-Knowledge Key Management

> **CRITICAL SECURITY RULE:** No API keys, tokens, or secrets are ever hardcoded in source code, baked into `BuildConfig`, stored in `gradle.properties`, or committed to version control.

All runtime keys are entered by the user through the in-app **Core Configuration / Settings** screen (or during initial onboarding) and stored exclusively in **hardware-backed `EncryptedSharedPreferences`** (`MasterKey.KeyScheme.AES256_GCM`). If a key is missing at runtime, Jarvis gracefully transitions to standby mode with an interactive prompt directing the user to configure the key.

---

## 🔑 API Keys — Where to Get Them

Use this comprehensive reference guide to obtain all credentials for full system functionality.

### Quick Reference Table

| Key Name | Provider | Free Tier Available? | Free Tier Limits | Where to Get | Required? |
|---|---|---|---|---|---|
| **Gemini API Key** | Google AI Studio | ✅ Yes | 15 RPM / 1M TPM | [aistudio.google.com](https://aistudio.google.com/) | **Yes (Core)** |
| **Porcupine Access Key** | Picovoice Console | ✅ Yes | Up to 3 active devices | [console.picovoice.ai](https://console.picovoice.ai/) | Optional |
| **OpenWeather API Key** | OpenWeatherMap | ✅ Yes | 1,000 calls / day | [openweathermap.org/api_keys](https://home.openweathermap.org/api_keys) | Optional |
| **Spotify Client ID & Secret** | Spotify Developer | ✅ Yes | Non-commercial developer access | [developer.spotify.com](https://developer.spotify.com/dashboard) | Optional |
| **ElevenLabs API Key** | ElevenLabs | ✅ Yes | 10,000 characters / month | [elevenlabs.io](https://elevenlabs.io/app/settings/api-keys) | Optional |
| **Google OAuth Client ID** | Google Cloud Console | ✅ Yes | Standard Cloud APIs tier | [console.cloud.google.com](https://console.cloud.google.com/) | Optional |
| **SerpAPI Key** | SerpAPI | ✅ Yes | 250 searches / month | [serpapi.com](https://serpapi.com/manage-api-key) | Optional |
| **IFTTT Webhook Key** | IFTTT Webhooks | ✅ Yes | Free applets & webhook triggers | [ifttt.com](https://ifttt.com/maker_webhooks/settings) | Optional |
| **Custom Endpoint URL** | Self-Hosted / Ollama | ✅ Yes | Unlimited local inference | Local / Private server | Optional |

---

### Step-by-Step Setup Guide

#### 1. Google Gemini API Key (Required for Core Intelligence)
1. Navigate to [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google account.
3. Click **"Get API key"** in the top-left sidebar.
4. Click **"Create API key"** (select a new or existing Google Cloud project).
5. Copy the generated key (starts with `AIza...`).
6. Paste into Jarvis during first launch or in **Settings → Gemini API Key**.
7. Tap **"Test Connection"** to verify the handshake.

#### 2. Picovoice Porcupine Access Key (Hands-free "Hey Jarvis")
1. Go to [Picovoice Console](https://console.picovoice.ai/).
2. Create a free account.
3. Copy your unique **AccessKey** from the dashboard.
4. In Jarvis Settings, paste into **Porcupine Access Key** and tap Save.

#### 3. OpenWeather API Key (Atmospheric Telemetry)
1. Register an account at [OpenWeatherMap](https://openweathermap.org/).
2. Head to the **API keys** tab under your profile.
3. Generate a default API key.
4. Note: New OpenWeather keys can take 10–30 minutes to activate globally.
5. Paste into Jarvis Settings and tap **"Test Connection"**.

#### 4. ElevenLabs Voice Synthesis (Ultra-Realistic Audio)
1. Sign up at [ElevenLabs](https://elevenlabs.io/).
2. Click your profile picture → **API Keys** → **Create Key**.
3. Paste into Jarvis Settings to enable custom voice synthesis.

---

## 🛠️ Building and Running

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or JDK 21
- Android SDK 34 (compileSdk 36, targetSdk 34, minSdk 26)

### Clone & Build
```bash
git clone <your-repo-url>
cd jarvis-android
./gradlew assembleDebug
```

### Run Tests
```bash
./gradlew :app:testDebugUnitTest
```
