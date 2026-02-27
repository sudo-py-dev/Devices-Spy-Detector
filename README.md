# NoTrace 🛡️

![NoTrace Hero Banner](assets/images/hero_banner.png)

**NoTrace** is a powerful privacy protection tool for Android designed to detect hidden spy devices, trackers, and unauthorized Bluetooth devices in your vicinity. Using advanced signal analysis and a curated database of known tracker signatures, NoTrace helps you stay safe and informed about your surroundings.

[עברית](README_HE.md)

---

## ✨ Key Features

- **🔍 Smart Scanning**: Detects AirTags, Samsung SmartTags, Tile trackers, and other Bluetooth-enabled spy devices.
- **📡 Radar Visualization**: Real-time visual feedback of nearby devices with signal strength indicators.
- **🕰️ Detection History**: Keeps a secure, localized log of all detected devices for later review.
- **🌙 Multiple Themes**: Supports Light, Dark, and System-default themes with a premium Material 3 design.
- **🌍 Multilingual**: Fully localized in English, Hebrew (עברית), German (Deutsch), and Russian (Русский).
- **🔋 Battery Optimized**: Background scanning with minimal battery impact using foreground services.
- **🛡️ Privacy First**: All data is stored locally on your device using encrypted preferences. No data ever leaves your phone.

---

## 🚀 How It Works

NoTrace employs a multi-layered detection strategy:

1. **Manufacturer ID Matching**: Identifies devices by their registered Bluetooth SIG manufacturer IDs.
2. **Name Pattern Recognition**: Scans for specific naming conventions used by popular tracking devices.
3. **Signal Strength Analysis**: Monitors RSSI (Received Signal Strength Indicator) to help you locate the physical device.

---

## 🛠️ Technology Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Material Design 3](https://m3.material.io/)
- **Dependency Injection**: [Koin](https://insert-koin.io/)
- **Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **Scanning**: [Nordic Semiconductor Android Scanner Compat Library](https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library)
- **Security**: [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)

---

## 📥 Installation

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/sudo-py-dev/Devices-Spy-Detector.git
    ```
2.  **Build the Project**:
    Open the project in Android Studio or use Gradle:
    ```bash
    ./gradlew assembleDebug
    ```
3.  **Install on Device**:
    ```bash
    ./gradlew installDebug
    ```

---

## ⚠️ Disclaimer

NoTrace is a detection aid and may occasionally report false positives (e.g., VR headsets, smart home devices, or other Bluetooth peripherals). Users are expected to use the results responsibly and in accordance with local privacy laws.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">
  Developed with ❤️ by sudopdev
</p>
