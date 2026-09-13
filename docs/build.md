# Hermes Android Build Documentation

This document explains how to build, test, and deploy the Hermes Android client using GitHub Actions.

## Prerequisites

### For Development on Termux

The Hermes Android project is designed to be developed **entirely from Termux** without requiring:
- Android Studio
- Local Android SDK
- Local Gradle installation

**Required on Termux:**
- Git
- Termux API (for file access)
- Text editor (nano, vim, etc.)
- SSH client (for Git operations)

### For GitHub Actions

No local setup is required. All builds run on GitHub Actions runners with:
- Ubuntu Linux
- JDK 17 (Temurin)
- Android SDK (automatically downloaded)
- Gradle 8.4

## Project Structure

```
hermes-android/
├── app/                          # Android app module
│   ├── src/                      # Source code
│   │   └── main/                 # Main source set
│   │       ├── java/             # Kotlin/Java code
│   │       └── res/              # Resources
│   ├── build.gradle.kts          # App module build config
│   └── proguard-rules.pro        # ProGuard rules
├── build.gradle.kts              # Root build config
├── settings.gradle.kts          # Project settings
├── gradle.properties            # Gradle properties
└── .github/
    └── workflows/                # GitHub Actions workflows
        ├── android-ci.yml        # CI workflow (auto-triggered)
        └── android-debug.yml     # Debug build workflow (manual)
```

## GitHub Actions Workflows

### 1. Android CI (`android-ci.yml`)

**Purpose:** Automatically build, test, and lint on every push and pull request.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` branch

**Steps:**
1. Checkout repository
2. Set up JDK 17 (Temurin)
3. Set up Gradle 8.4
4. Cache Gradle dependencies
5. Run Android lint
6. Run unit tests
7. Build debug APK
8. Build release APK (unsigned)
9. Upload artifacts:
   - `lint-report`: Lint HTML report
   - `test-results`: Unit test results
   - `hermes-android-debug`: Debug APK
   - `hermes-android-release`: Release APK (unsigned)

**Artifact Retention:** 30 days

### 2. Android Debug (`android-debug.yml`)

**Purpose:** Manually trigger a debug APK build.

**Triggers:**
- Manual workflow dispatch via GitHub UI or API

**Steps:**
1. Checkout repository
2. Set up JDK 17 (Temurin)
3. Set up Gradle 8.4
4. Cache Gradle dependencies
5. Build debug APK
6. Rename APK with version info
7. Upload artifacts:
   - `hermes-android-debug`: Versioned debug APK + version info
   - `release-notes`: Build information

**Artifact Retention:** 90 days

## How to Use

### Pushing Code from Termux

1. **Clone the repository (if not already cloned):**
   ```bash
   git clone https://github.com/xeriomy/Hermez.git
   cd Hermez
   ```

2. **Make changes to the Android app:**
   - Edit files in the `app/` directory
   - Use a text editor (nano, vim, etc.)

3. **Commit changes:**
   ```bash
   git add .
   git commit -m "Your commit message"
   ```

4. **Push to GitHub:**
   ```bash
   git push origin main
   ```

5. **CI will automatically run:**
   - The `android-ci.yml` workflow will trigger
   - Build, test, and lint will execute
   - Results will be available in GitHub Actions

### Checking CI Results

1. **Go to GitHub Actions:**
   - Navigate to: `https://github.com/xeriomy/Hermez/actions`
   - Or: Repository -> Actions tab

2. **View workflow runs:**
   - Find the latest workflow run
   - Click on it to see details

3. **Check build status:**
   - Green checkmark: Success
   - Red X: Failure
   - Yellow circle: Running

4. **View logs:**
   - Click on each step to expand
   - View stdout/stderr for each command

5. **Download artifacts:**
   - Scroll to the bottom of the workflow run
   - Click on "Artifacts" section
   - Download any available artifacts

### Manually Triggering a Debug Build

1. **Go to GitHub Actions:**
   - Navigate to: `https://github.com/xeriomy/Hermez/actions`

2. **Select the debug workflow:**
   - Click on "Android Debug Build" in the left sidebar

3. **Run workflow:**
   - Click "Run workflow" dropdown
   - Optionally add a reason
   - Click "Run workflow"

4. **Wait for completion:**
   - The workflow will take ~5-10 minutes
   - Monitor progress in the Actions tab

5. **Download the APK:**
   - After completion, scroll to the bottom
   - Download the `hermes-android-debug` artifact
   - The APK is named: `hermes-android-<version>-<versionCode>-debug.apk`

### Downloading the Debug APK

**From GitHub Actions:**

1. Go to Actions tab
2. Select the workflow run (either CI or manual debug build)
3. Scroll to the "Artifacts" section
4. Click on `hermes-android-debug`
5. Download the APK file

**From GitHub Releases (future):**

Once releases are set up:
1. Go to Releases tab
2. Find the latest release
3. Download the APK from the assets

### Installing on Android Phone

1. **Download the APK** from GitHub Actions
2. **Transfer to your phone:**
   - Email it to yourself
   - Use a file sharing service
   - Use a USB cable
   - Use adb: `adb push hermes-android-debug.apk /sdcard/`
3. **Install the APK:**
   - Open the file on your phone
   - Allow installation from unknown sources if prompted
   - Follow the installation prompts

### Inspecting Build/Test/Lint Logs

**Viewing logs in GitHub Actions:**

1. Go to the specific workflow run
2. Click on the step you want to inspect (e.g., "Run lint", "Run unit tests")
3. Expand the step to see the full log output

**Log retention:**
- Logs are available for 90 days on GitHub
- You can download logs as text files

**Common log locations:**
- Lint: `app/build/reports/lint-results-*.html`
- Tests: `app/build/reports/tests/testDebugUnitTest/`
- Build: Standard Gradle output

## Tool Versions

The project pins specific versions for reproducibility:

| Tool | Version | Configuration Location |
|------|---------|------------------------|
| JDK | 17 | `.github/workflows/*.yml` |
| Android Gradle Plugin | 8.4.0 | `build.gradle.kts` |
| Gradle | 8.4 | `.github/workflows/*.yml` |
| Kotlin | 1.9.22 | `build.gradle.kts` |
| Compose | 1.5.4 | `app/build.gradle.kts` |
| compileSdk | 34 | `app/build.gradle.kts` |
| targetSdk | 34 | `app/build.gradle.kts` |
| minSdk | 24 | `app/build.gradle.kts` |

## Commands for Local Testing (Optional)

While the project is designed for GitHub Actions, you can also test locally if you have Android SDK installed:

```bash
# Build debug APK
./gradlew assembleDebug

# Run lint
./gradlew lintDebug

# Run unit tests
./gradlew testDebugUnitTest

# Clean build
./gradlew clean

# Build with stacktrace
./gradlew assembleDebug --stacktrace
```

## Troubleshooting

### Build Failures

1. **Check the error message** in the workflow logs
2. **Look at the specific step** that failed
3. **Common issues:**
   - Missing dependencies: Check `app/build.gradle.kts`
   - Version conflicts: Check version pins
   - Network issues: Retry the workflow
   - Gradle cache issues: Clear cache and retry

### Test Failures

1. Check the test report in the artifacts
2. Look for failing test cases
3. Fix the test or the code
4. Push changes to trigger CI again

### Lint Failures

1. Check the lint HTML report in the artifacts
2. Look for specific lint warnings/errors
3. Fix the issues in the code
4. Push changes to trigger CI again

### APK Not Generated

1. Check if the build step completed successfully
2. Look for errors in the build logs
3. Check if the APK exists in the artifacts
4. If using manual workflow, ensure it ran to completion

## Known Limitations

1. **No signing for release builds:**
   - The CI workflow generates unsigned release APKs
   - For production, you'll need to set up signing
   - See: `app/proguard-rules.pro` for ProGuard config

2. **No instrumentation tests:**
   - Currently only unit tests are configured
   - Instrumentation tests require an emulator
   - GitHub Actions supports Android emulators but adds complexity

3. **No device testing:**
   - The CI only builds, doesn't test on real devices
   - Manual testing on physical devices is recommended

4. **Debug keystore:**
   - Uses standard Android debug keystore
   - Not suitable for production releases

5. **Network dependencies:**
   - Build requires internet for Gradle dependencies
   - GitHub Actions provides internet access

## Best Practices

1. **Small, focused commits:**
   - Easier to review
   - Easier to debug if CI fails

2. **Test locally first:**
   - If you have Android SDK, test locally before pushing
   - Reduces CI cycle time

3. **Monitor CI:**
   - Check CI results after every push
   - Fix failures immediately

4. **Use artifacts:**
   - Download and test APKs from CI
   - Verify functionality before merging

5. **Keep dependencies updated:**
   - Regularly update Gradle, Kotlin, Compose versions
   - Check for security vulnerabilities

## Future Enhancements

1. **Signed release builds:**
   - Configure GitHub Actions secrets for signing
   - Generate signed APKs for distribution

2. **App distribution:**
   - Automatically upload to Google Play
   - Or distribute via GitHub Releases

3. **Instrumentation tests:**
   - Add Android emulator support to CI
   - Run UI tests on emulated devices

4. **Code coverage:**
   - Add JaCoCo or similar for code coverage
   - Upload coverage reports to Codecov

5. **Performance testing:**
   - Add benchmark tests
   - Track performance metrics

## Contact

For issues with the build process:
- Check the GitHub Actions logs first
- Review this documentation
- Open an issue in the repository

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-XX-XX | Initial CI setup |
