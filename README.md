# Dchs Flutter Beacon


[![Pub](https://img.shields.io/pub/v/dchs_flutter_beacon.svg)](https://pub.dev/packages/dchs_flutter_beacon) 
[![GitHub](https://img.shields.io/github/license/dariocavada/dchs_flutter_beacon.svg?color=2196F3)](https://github.com/dariocavada/dchs_flutter_beacon/blob/master/LICENSE) 

[Flutter plugin](https://pub.dev/packages/dchs_flutter_beacon/) to work with iBeacons.  

Hybrid iBeacon scanner and transmitter plugin for Flutter. Supports Android API 21+ and iOS 13+.

Features:

* Automatic permission management
* Ranging iBeacons  
* Monitoring iBeacons
* Transmit as iBeacon

## Installation

Add to pubspec.yaml:

```yaml
dependencies:
  dchs_flutter_beacon: ^0.6.7
```

### Setup specific for Android

Add the following permissions to your `AndroidManifest.xml`:

```xml
<!-- Required for BLE scanning on Android 6+ -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

<!-- Required for BLE scanning on Android 12+ (API 31+) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- Recommended when checking Bluetooth state or prompting the user to enable Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Optional: background scanning -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<!-- Optional: transmitting as iBeacon -->
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
```

> **Important:** on Android 12+ the plugin currently expects both `ACCESS_FINE_LOCATION`
> and `BLUETOOTH_SCAN` for scanning. On Android 6 to 11 it expects `ACCESS_FINE_LOCATION`.

In addition to declaring permissions in the manifest you must also request them at runtime.
The plugin can handle this automatically with `initializeAndCheckScanning`, or you can call
`flutterBeacon.requestAuthorization` explicitly before starting a scan.

#### Location services must be enabled

Android requires device **Location Services** to be turned on for BLE scanning, even when
Bluetooth permissions are granted. You can check and prompt the user with:

```dart
final locationEnabled = await flutterBeacon.checkLocationServicesIfEnabled;
if (!locationEnabled) {
  await flutterBeacon.openLocationSettings;
}
```

Refer to the example app for a complete setup flow.

#### Android Troubleshooting

**Beacons not detected at all (especially from iOS simulator apps)**

1. Make sure `ACCESS_FINE_LOCATION` is declared in `AndroidManifest.xml` **and** granted at runtime.
2. Verify that device Location Services (GPS) are switched **on** — Bluetooth scanning on Android
   depends on Location Services being enabled.
3. On Android 12+ also ensure `BLUETOOTH_SCAN` is granted at runtime.
4. Try scanning with an **unfiltered region** first to confirm that iBeacon packets are visible
   at all, before filtering by UUID:

```dart
// Android: scan all iBeacons without UUID filter
final regions = [Region(identifier: 'all-beacons')];
_streamRanging = flutterBeacon.ranging(regions).listen((result) { ... });
```

5. Confirm that the iOS simulator app is actually advertising. Use a generic BLE scanner such as
   **nRF Connect** on the same Android device to check whether the BLE packets are visible.
6. If detection is inconsistent, try enabling the tracking cache so beacons are not immediately
   dropped between scan windows:

```dart
await flutterBeacon.setUseTrackingCache(true);
await flutterBeacon.setMaxTrackingAge(10000); // keep beacons for 10 s
```

7. On some devices, disabling **"Bluetooth A2DP Hardware Offload"** in Developer Options improves
   BLE advertisement detection reliability.

#### iOS 13+ Beacon Visibility Issue

On iOS 13 and later, beacons may only appear briefly before being lost. 
To mitigate this issue, try increasing the setBetweenScanPeriod parameter to a value greater than 0:

``` dart
await flutterBeacon.setScanPeriod(1000);
await flutterBeacon.setBetweenScanPeriod(500);
```

#### Persistent Beacon Detection Using Cache
If you want beacons to persistently appear in the results, you can enable the tracking cache and set a maximum tracking age. For example:

``` dart
await flutterBeacon.setUseTrackingCache(true);
await flutterBeacon.setMaxTrackingAge(10000);
```

### Setup specific for iOS

In order to use beacons related features, apps are required to ask the location permission. It's a two step process:

1. Declare the permission the app requires in configuration files
2. Request the permission to the user when app is running (the plugin can handle this automatically)

The needed permissions in iOS is `when in use`.

For more details about what you can do with each permission, see:  
https://developer.apple.com/documentation/corelocation/choosing_the_authorization_level_for_location_services

Permission must be declared in `ios/Runner/Info.plist`:

```xml
<dict>
  <!-- When in use -->
  <key>NSLocationWhenInUseUsageDescription</key>
  <string>Reason why app needs location</string>
  <!-- Always -->
  <!-- for iOS 11 + -->
  <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
  <string>Reason why app needs location</string>
  <!-- for iOS 9/10 -->
  <key>NSLocationAlwaysUsageDescription</key>
  <string>Reason why app needs location</string>
  <!-- Bluetooth Privacy -->
  <!-- for iOS 13 + -->
  <key>NSBluetoothAlwaysUsageDescription</key>
  <string>Reason why app needs bluetooth</string>
</dict>
```

## iOS Troubleshooting

* Example code works properly only on **physical device** (bluetooth on simulator is disabled)
* How to deploy flutter app on iOS device [Instruction](https://flutter.dev/docs/get-started/install/macos)
* If example code don't works on device (beacons not appear), please make sure that you have enabled <br/> Location and Bluetooth (Settings -> Flutter Beacon) 

## How-to

Ranging APIs are designed as reactive streams.  

* The first subscription to the stream will start the ranging

### Initializing Library

```dart
try {
  // Manual initialization only
  await flutterBeacon.initializeScanning;
  
  // Initialization with permission and settings checks
  await flutterBeacon.initializeAndCheckScanning;
} on PlatformException catch(e) {
  // library failed to initialize, check code and message
}
```

### Recommended Android startup flow

```dart
final initialized = await flutterBeacon.initializeAndCheckScanning;
if (!initialized) {
  return;
}

final locationEnabled = await flutterBeacon.checkLocationServicesIfEnabled;
if (!locationEnabled) {
  await flutterBeacon.openLocationSettings;
  return;
}
```

### Ranging beacons

```dart
final regions = <Region>[];

if (Platform.isIOS) {
  // iOS platform, at least set identifier and proximityUUID for region scanning
  regions.add(Region(
      identifier: 'Apple Airlocate',
      proximityUUID: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0'));
} else {
  // android platform, it can ranging out of beacon that filter all of Proximity UUID
  regions.add(Region(identifier: 'com.beacon'));
}

// to start ranging beacons
_streamRanging = flutterBeacon.ranging(regions).listen((RangingResult result) {
  // result contains a region and list of beacons found
  // list can be empty if no matching beacons were found in range
});

// to stop ranging beacons
_streamRanging.cancel();
```

### Monitoring beacons

```dart
final regions = <Region>[];

if (Platform.isIOS) {
  // iOS platform, at least set identifier and proximityUUID for region scanning
  regions.add(Region(
      identifier: 'Apple Airlocate',
      proximityUUID: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0'));
} else {
  // Android platform, it can ranging out of beacon that filter all of Proximity UUID
  regions.add(Region(identifier: 'com.beacon'));
}

// to start monitoring beacons
_streamMonitoring = flutterBeacon.monitoring(regions).listen((MonitoringResult result) {
  // result contains a region, event type and event state
});

// to stop monitoring beacons
_streamMonitoring.cancel();
```

### Broadcasting as iBeacon

```dart
if (await flutterBeacon.isBroadcastSupported()) {
  await flutterBeacon.startBroadcast(
    BeaconBroadcast(
      proximityUUID: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0',
      major: 1,
      minor: 1,
      txPower: -59,
      identifier: 'com.example.myBeacon',
    ),
  );
}

await flutterBeacon.stopBroadcast();
```

## Under the hood

* iOS uses native Framework [CoreLocation](https://developer.apple.com/documentation/corelocation/)
* Android uses the [Android-Beacon-Library](https://github.com/AltBeacon/android-beacon-library) ([Apache License 2.0](https://github.com/AltBeacon/android-beacon-library/blob/master/LICENSE))  

## Example app

The example app in [example](example) demonstrates:

* permission and settings checks
* ranging and monitoring
* beacon broadcasting

Run it with:

```bash
cd example
flutter pub get
flutter run
```

# Author

Flutter Beacon plugin originally was developed by Eyro Labs. 

DCHS Flutter Beacon is an updated version of the original plugin, now ported to Kotlin by Dario Cavada. 
For inquiries or support, feel free to reach out at dario.cavada.lab@gmail.com (https://www.suggesto.eu)
