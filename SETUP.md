# Setup Guide for BLE Scan App

## Prerequisites

1. Android Studio (latest version recommended)
2. Android device with BLE support (Android 8.0+)
3. Firebase project (`blefinal`) already set up
4. Physical BLE beacon for testing

## Step-by-Step Setup

### 1. Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **blefinal**
3. Click on "Add app" → Android
4. Enter package name: `com.ble.scan`
5. Download `google-services.json`
6. Place the file in: `app/google-services.json` (replace the placeholder)

### 2. Build Configuration

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. If you see any errors about `google-services.json`, make sure you've added your own file

### 3. Permissions Setup

The app will request permissions at runtime:
- **Bluetooth permissions** (varies by Android version)
- **Location permission** (required for BLE scanning)
- **Notification permission** (Android 13+)

### 4. Testing

1. **On a Physical Device** (BLE scanning doesn't work on emulator):
   - Connect your Android device via USB
   - Enable USB debugging
   - Run the app from Android Studio

2. **First Run**:
   - Enter your name and phone number
   - Grant all requested permissions
   - The app will start scanning automatically

3. **Test with Beacon**:
   - Make sure you have a BLE beacon configured
   - Create a campaign in the admin panel with the beacon's UUID
   - When the app detects the beacon, you should receive a notification

### 5. Admin Panel Integration

The app connects to the same Firebase project as your admin panel:
- **Campaigns Collection**: `campaigns`
- **Logs Collection**: `logs`

Make sure campaigns are created with:
- `uuid`: Beacon UUID (lowercase)
- `isActive`: `true`
- `name`, `description`, `website`: Campaign details

## Troubleshooting

### App doesn't detect beacons
- Check if Bluetooth is enabled
- Verify location permission is granted
- Check if the beacon is broadcasting
- Verify the UUID matches exactly (case-insensitive, but normalized to lowercase)

### Notifications not appearing
- Check notification permission (Android 13+)
- Verify the campaign is active in Firestore
- Check Firebase connection
- Look at Logcat for error messages

### Service stops running
- Check battery optimization settings
- Disable battery optimization for the app
- The service should restart automatically, but some devices may kill it

### Firebase errors
- Verify `google-services.json` is correct
- Check internet connection
- Verify Firestore is enabled in Firebase Console

## Beacon UUID Formats Supported

The app supports multiple beacon formats:
1. **Service UUIDs** (most common)
2. **iBeacon format** (Apple manufacturer data)
3. **Eddystone format** (service data)

The UUID will be normalized to lowercase before matching with Firestore campaigns.

## Notes

- The app uses a foreground service to ensure continuous scanning
- Duplicate notifications are prevented for 5 minutes per UUID
- The service automatically restarts after device reboot
- Works on Android 8.0 (API 26) to latest Android versions
