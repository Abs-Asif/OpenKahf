# OpenKahf

OpenKahf is an open-source Android application designed to provide a safe and private browsing experience. It serves as a transparent and privacy-focused alternative to other solutions that may compromise user data.

**Current Status:** Alpha Version (v1.0.0.04.10.1)

## Motive
The primary motive behind OpenKahf is to offer a better, truly private alternative to services like Kahfguard. We believe that security and filtering should not come at the cost of your personal data. OpenKahf is built on the principles of transparency and user empowerment.

## Features
- **DNS for Family Integration**: Easily configure your device to use `dns-dot.dnsforfamily.com` for filtered, safe browsing.
- **DNS Status Tracking**: Real-time monitoring to ensure your device is actively protected.
- **Prevent Change**: (Accessibility Required) A security feature that prevents unauthorized or accidental changes to your Private DNS settings.
- **Prevent Uninstall**: (Accessibility Required) Stops the app from being uninstalled without a cooldown period.
- **5-Minute Cooldown**: To disable security features, users must wait for 5 minutes, adding an extra layer of protection against impulsive changes.
- **API Documentation**: Built-in documentation for developers to interact with the DNS for Family blocklist API.

## How It Works
OpenKahf uses Android's **Accessibility Service** to monitor system settings. If "Prevent Change" or "Prevent Uninstall" is enabled, the service will detect when a user attempts to modify DNS settings or uninstall the app and will automatically navigate the user away from those screens.

## Privacy
- **No Data Selling**: We do not collect, store, or sell your personal data.
- **Transparency**: OpenKahf is open-source. You can inspect the code to see exactly how it works.
- **Lightweight**: Built with Kotlin and Jetpack Compose for optimal performance and minimal battery impact.

## Development
This project is built using:
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Networking**: OkHttp
- **Storage**: Jetpack DataStore

### Building from Source
1. Clone the repository.
2. Open in Android Studio (Jellyfish or newer recommended).
3. Ensure you have JDK 21 configured.
4. Build using `./gradlew assembleDebug`.

## Roadmap
- [ ] Beta release with UI enhancements.
- [ ] Support for multiple safe DNS providers.
- [ ] Improved heuristic detection for more device types.
- [ ] Full production release on public platforms.

## Contributing
We welcome contributions! Please feel free to submit issues or pull requests.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
