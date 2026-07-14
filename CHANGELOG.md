
## [0.6.10]
* Add Swift Package Manager support for iOS while retaining CocoaPods compatibility.
* Handle Android-only scan period and tracking-cache settings as successful no-ops on iOS so they remain safe in cross-platform code.

## [0.6.9]
* Fix Android broadcasting when Bluetooth is disabled at app launch: recreate the AltBeacon `BeaconTransmitter` before starting an idle advertisement so enabling Bluetooth later no longer leaves the process stuck with a cached null advertiser and a swallowed `NullPointerException`.
* Update the Android example and plugin Gradle setup for Flutter's upcoming dependency validation: Gradle 8.14.3, Android Gradle Plugin 8.11.1, Kotlin 2.2.20, and Built-in Kotlin-compatible build files.

## [0.6.8]
* Fix Android plugin registration for Flutter headless engines by setting up method and event channels in `onAttachedToEngine`
* Use the application context for Android beacon scanning, Bluetooth/location checks, and AltBeacon service binding
* Keep Activity-dependent Android flows scoped to Activity attachment, returning explicit errors when headless engines request Activity-only operations
* Improve Android scanning lifecycle so one Flutter engine does not remove AltBeacon range or monitor callbacks owned by another engine
* Avoid clearing process-wide AltBeacon parser state when registering the iBeacon parser
* Complete pending Android Activity-dependent method calls with an error if the Activity detaches before completion
* Support foreground-service/headless-engine scanning scenarios where the main app UI is closed but scanning remains active

## [0.6.7]
* Update Android-Beacon-Library to version [2.21.2](https://github.com/AltBeacon/android-beacon-library/tree/2.21.2)
* Fix Android `checkLocationServicesPermission()` behavior compared to version `0.6.6`
* Fix Android `startBroadcast()` permission flow on Android 12+ by handling `BLUETOOTH_ADVERTISE` callback correctly
* Fix `onActivityResult()` so the plugin only handles its own Bluetooth enable request and ignores unrelated activity results
* Align Android permission requests with the permissions actually required by each OS version for BLE scanning
* Improve Android permission result handling so `ACCESS_FINE_LOCATION` and `BLUETOOTH_SCAN` are evaluated coherently
* Improve Android lifecycle cleanup by stopping ranging/monitoring and unbinding AltBeacon on activity detach
* Remove deprecated `package` attribute from the Android plugin manifest to avoid AGP namespace warnings
* Clean dead/commented Android plugin code and small receiver robustness issues
* Replace the obsolete Android unit test with utility tests that cover current plugin code paths
* Make Android region parsing more fail-safe when invalid beacon identifiers are passed from Flutter
* Refresh package and example README files so setup instructions match the current Android permission and initialization flow

## [0.6.6]
* Update Java compatibility from VERSION_11 to VERSION_17 for modern development standards

## [0.6.5]
* Updated
    * Android Dependencies:
        * Update Android-Beacon-Library to version [2.21.1](https://github.com/AltBeacon/android-beacon-library/tree/2.21.1)
        * Update androidx.core:core-ktx from 1.9.0 to 1.13.1 for Android 15 support
        * Update androidx.appcompat:appcompat from 1.5.1 to 1.7.0 for enhanced Material Design 3 support
        * Update androidx.annotation:annotation from 1.2.0 to 1.8.2 for improved null safety
        * Update org.mockito:mockito-core from 5.0.0 to 5.12.0 for better testing compatibility
    * Build Tools:
        * Update Kotlin version from 1.8.22 to 2.1.0 for latest language features and performance improvements
        * Update Android Gradle Plugin from 8.1.0 to 8.6.0 for better Android 15 compatibility
        * Update Gradle wrapper from 8.3 to 8.10.2 for improved build performance
        * Update compileSdk to 35 (Android 15) for latest API support
        * Update Java compatibility from VERSION_1_8 to VERSION_11 for modern development standards
    * Compatibility:
        * Full Android 15 (API 35) support
        * Enhanced compatibility with latest AndroidX libraries
        * Improved build performance and compilation times
        * Future-proof setup with latest toolchain

## [0.6.4]
* Added
    * New Methods:
        * setUseTrackingCache() and setMaxTrackingAge() for enhanced beacon persistence.
    * Example Updates:
        * Demonstrated the usage of setUseTrackingCache() and setMaxTrackingAge() in the example.
        * Included the usage of setScanPeriod(1000) and setBetweenScanPeriod(500) in the example.

* Updated
    * Documentation:
        * Enhanced the README to include details on the new methods.
        * Added troubleshooting tips for beacon detection issues, particularly for Android 13+.

## [0.6.3]
* Fixed bugs in the Android Beacon Scanner and improved exception handling.
* Added BlueUP beacon proximityUUID

## [0.6.2]
* Breaking changes. You have to use dchs_flutter_beacon instead of flutter_beacon.
* Rewrite plugin (Android part) with Kotlin and rename to dchs_flutter_beacon 
* Update Android-Beacon-Library to version [2.20.7]
* Update README.md with new Android permission requirement for broadcast (from Android 12)
* Recreated the example with latest flutter environment

## [0.5.1]
* Migration the example to null safety and to Android v2 embedding
* Update Android-Beacon-Library to version [2.19] with added support to Android 12 (https://github.com/AltBeacon/android-beacon-library/tree/2.19)
* Update README.md with new Android permission requirement for location (SDK 29+, Android 10, 11)

## [0.5.0]
* Migration to null safety

## [0.4.0]
* Update to Android v2 embedding
* Add broadcast Beacon feature
* Improve example app

## [0.3.0]
* Update Android-Beacon-Library to version [2.16.3](https://github.com/AltBeacon/android-beacon-library/tree/2.16.3)
* Add `BluetoothState` event channel
* Add `AuthorizationStatus` event channel [iOS only]
* Add manual check for `AuthorizationStatus`, `BluetoothState`, Location Service.
* Add opener settings for Bluetooth [Android], Location [Android] and Application [iOS].
* A lot of improvements

## [0.2.4]

* Fix bugs
* Add `close` scanning API

## [0.2.3]

* Update gradle build to latest version
* Update stop 

## [0.2.2]

* Update gradle to latest version
* Update Android-Beacon-Library to version [2.16.1](https://github.com/AltBeacon/android-beacon-library/tree/2.16.1)

## [0.2.1]

* Fix crash ranging when region not set for Android

## [0.2.0]

* Migrate to AndroidX

## [0.1.1]

* Fix region ranging for Android

## [0.1.0]

* Updating library docs
* Adding example docs

## [0.0.2]

* Adding Dart Docs

## [0.0.1]

* Initial release
* Ranging iBeacons for iOS and Android
