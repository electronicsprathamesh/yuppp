# BLE Scan - Android App

A Kotlin Android application that continuously scans for BLE (Bluetooth Low Energy) beacons and sends push notifications when registered campaigns are detected.

## Features

- **User Login**: Simple login with name and phone number (stored locally)
- **Continuous BLE Scanning**: Scans for beacons in the background using a foreground service
- **Firebase Integration**: Checks detected beacon UUIDs against Firestore campaigns
- **Push Notifications**: Sends notifications with campaign details when a registered beacon is detected
- **Background Operation**: Works continuously even when the app is closed
- **Boot Receiver**: Automatically restarts scanning after device reboot

## Requirements

- Android 8.0 (API 26) or higher
- Bluetooth Low Energy (BLE) support
- Internet connection for Firebase
- Required permissions:
  - Bluetooth permissions (varies by Android version)
  - Location permission (required for BLE scanning)
  - Notification permission (Android 13+)

## Setup Instructions

### 1. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (`blefinal`)
3. Add an Android app with package name: `com.ble.scan`
4. Download `google-services.json` and place it in the `app/` directory
5. Make sure Firestore is enabled in your Firebase project

### 2. Build the Project

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on a device with BLE support

### 3. Admin Panel

The admin panel (web) is used to:
- Create campaigns with beacon UUIDs
- View logs of notifications sent
- Manage active campaigns

## Project Structure

```
app/
├── src/main/
│   ├── java/com/ble/scan/
│   │   ├── data/          # Data models (User, Campaign)
│   │   ├── service/        # BLE Scanning Service
│   │   ├── ui/             # Activities (Login, Main)
│   │   ├── util/           # Helpers (Firebase, Notification, Preferences)
│   │   └── receiver/       # Boot Receiver
│   ├── res/                # Resources (layouts, strings, colors)
│   └── AndroidManifest.xml
└── google-services.json     # Firebase config (add your own)
```

## How It Works

1. **Login**: User enters name and phone number, saved locally
2. **Service Start**: BLE scanning service starts as foreground service
3. **Beacon Detection**: Service continuously scans for BLE beacons
4. **UUID Extraction**: Extracts UUID from detected beacon
5. **Firebase Check**: Queries Firestore for active campaign with matching UUID
6. **Notification**: If campaign found, sends notification with campaign details
7. **Logging**: Logs the notification event to Firestore

## Firebase Collections

### Campaigns Collection
```json
{
  "name": "Campaign Name",
  "description": "Campaign Description",
  "website": "https://example.com",
  "uuid": "beacon-uuid-lowercase",
  "isActive": true,
  "createdAt": "timestamp",
  "createdBy": "user-id"
}
```

### Logs Collection
```json
{
  "userName": "User Name",
  "userPhone": "Phone Number",
  "beaconName": "Beacon Name",
  "campaignName": "Campaign Name",
  "timestamp": "timestamp",
  "distance": 0.0
}
```

## Permissions

The app requires the following permissions:
- `BLUETOOTH_SCAN` (Android 12+)
- `BLUETOOTH_CONNECT` (Android 12+)
- `BLUETOOTH` (Android 11 and below)
- `BLUETOOTH_ADMIN` (Android 11 and below)
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+)
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`

## Notes

- The app uses proximity advertisement (iBeacon/Eddystone) format
- UUIDs are normalized to lowercase for matching
- Duplicate notifications are prevented for 5 minutes per UUID
- Service automatically restarts if killed by the system
- Works on all Android versions from API 26 (Android 8.0) to latest

## License

This project is created for the BLE Ads system.
