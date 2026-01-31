# Fix Gradle Cache Error

## Error
```
C:\Users\Admin\.gradle\caches\9.0-milestone-1\transforms\91390a755de3bae06091df03631beac8\metadata.bin (The system cannot find the file specified)
```

This error occurs when the Gradle cache is corrupted or incomplete.

## Solutions (Try in order)

### Solution 1: Clean Gradle Cache (Recommended)

**In Android Studio:**
1. Go to **File → Invalidate Caches / Restart**
2. Select **Invalidate and Restart**
3. Wait for Android Studio to restart
4. Go to **File → Sync Project with Gradle Files**

**Or manually (Windows):**
```cmd
# Close Android Studio first, then run:
rmdir /s /q C:\Users\Admin\.gradle\caches
```

**Or manually (Mac/Linux):**
```bash
rm -rf ~/.gradle/caches
```

### Solution 2: Clean Project Build

**In Android Studio:**
1. **Build → Clean Project**
2. Wait for it to complete
3. **Build → Rebuild Project**

**Or via command line:**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Solution 3: Delete .gradle Folder in Project

1. Close Android Studio
2. Delete the `.gradle` folder in your project root
3. Delete the `build` folders in project root and `app/` directory
4. Reopen Android Studio
5. Sync Gradle files

### Solution 4: Update Gradle Wrapper

The error mentions `9.0-milestone-1` which is a milestone version. Let's use a stable version.

Create or update `gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### Solution 5: Use Gradle Daemon

Add to `gradle.properties`:
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

## Quick Fix Script (Windows)

Create a file `fix-gradle.bat` in your project root:

```batch
@echo off
echo Cleaning Gradle cache...
rmdir /s /q "%USERPROFILE%\.gradle\caches" 2>nul
echo Cleaning project build folders...
if exist .gradle rmdir /s /q .gradle
if exist build rmdir /s /q build
if exist app\build rmdir /s /q app\build
echo Done! Now reopen Android Studio and sync.
pause
```

Run it as Administrator if needed.

## After Fixing

1. **Restart Android Studio**
2. **File → Sync Project with Gradle Files**
3. **Build → Clean Project**
4. **Build → Rebuild Project**

## Prevention

- Don't interrupt Gradle syncs
- Keep Android Studio and Gradle updated
- Use stable Gradle versions (not milestones)
- Regularly clean project builds
