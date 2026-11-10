# SDA Mobile

[![Android CI](https://github.com/chief-leeb/SDA-Mobile/actions/workflows/android.yml/badge.svg)](https://github.com/chief-leeb/SDA-Mobile/actions/workflows/android.yml)

[Русский](README.md) · **English**

An Android client inspired by Steam Desktop Authenticator: Steam Guard codes, trade and market confirmations, authenticator enrollment, and maFile transfer.

> The in-app interface is currently in Russian.

[Download APK](https://github.com/chief-leeb/SDA-Mobile/releases/latest) · [Report an issue](https://github.com/chief-leeb/SDA-Mobile/issues)

## Status

Early 1.0.0 release. The build and lint pass, but the app has not been tested on a physical device yet. Try it with an alt account before using it with your main account.

## Features

- Offline Steam Guard code generation with Steam time offset sync.
- Trade and market confirmations: list, accept, and decline manually.
- Mobile authenticator enrollment: sign in, Steam Guard, phone linking, SMS activation.
- Import and export maFile in SDA format (unencrypted JSON).
- Multiple accounts, active account selection, time sync.
- Revoke Steam Guard with a revocation code and remove an account from the device.

## Security

- `vault.bin` is encrypted with AES-256-GCM using an Android Keystore key (alias `sda_mobile_vault_v1`).
- `android:allowBackup="false"`, and the app window is protected with `FLAG_SECURE`.
- Secrets are sent only to official Steam servers.
- No cloud backend, analytics, or auto-confirmations.
- The Steam password is used for sign-in only and is not stored.
- The signing key, `keystore.properties`, and local artifacts are excluded from Git via `.gitignore`.

## Building

Requirements:

- JDK 17
- Android SDK: platform 35, build-tools 35.0.0
- Android SDK path in `local.properties` or the `ANDROID_HOME` environment variable

Debug APK:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:ANDROID_HOME = "C:\path\to\android-sdk"
.\gradlew.bat assembleDebug
```

Release APK:

1. Create `keystore.properties` from `keystore.properties.example`.
2. Set your keystore path, alias, and passwords.
3. Build the APK:

```powershell
.\gradlew.bat assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`.

`keystore.properties` and the keystore itself are not committed. Use your own key for publishing and never add it to the repository.

## Authors

- **chief-leeb** - project owner, requirements, and acceptance.
- **DeepSeek V4.1 Flash** (OpenCode) - architecture, code, documentation, and release build.

## Limitations

- Only unencrypted maFile files are supported. Encrypted SDA files must be decrypted in SDA first.
- There are no auto-confirmations: every action is confirmed manually.
- This app is not affiliated with Valve. Steam and Steam Guard are trademarks of Valve Corporation.

## License

MIT. See [LICENSE](LICENSE).
