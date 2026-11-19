# Debout

Android Wake on LAN app. Beautiful, ergonomic, free and open source.

## Repository

| Folder | Content |
|---|---|
| [`android/`](android) | The Android app (Kotlin, Jetpack Compose, Material 3) |
| [`web/`](web) | The pairing website, served at [debout.sybers.fr](https://debout.sybers.fr) |
| [`scripts/`](scripts) | The pairing scripts run on the computer to wake |
| [`docs/`](docs) | Product documentation ([V1 spec](docs/spec.md)) |

## Development

Open `android/` in Android Studio, or from the command line:

```sh
cd android
./gradlew assembleDebug        # build
./gradlew lint testDebugUnitTest  # checks run by CI
```

The site in `web/` is static: serve the folder (for instance `python3 -m http.server -d web`) and open it in a browser.

The pairing scripts have tests, run by CI: [Pester](https://pester.dev) for Windows, plain sh for macOS and Linux.

```sh
scripts/test.sh          # macOS and Linux scripts, with sh (or: scripts/test.sh dash)
```

```powershell
Invoke-Pester -Path scripts
```

## Releases

Pushing a `v*` tag builds a signed APK and publishes it on GitHub Releases, with the English changelog as release notes. Running the Android workflow by hand (`gh workflow run android.yml`) builds the signed APK as a workflow artifact without publishing anything: use it to check signing before a tag.

To release:

1. Bump `versionCode` (+1) and `versionName` in `android/app/build.gradle.kts`.
2. Write `fastlane/metadata/android/{en-US,fr-FR}/changelogs/<versionCode>.txt`, short and for users.
3. Tag `v<versionName>` and push the tag. CI refuses a tag that does not match `versionName`, or a missing changelog.

App store metadata (title, descriptions, changelogs) lives in [`fastlane/metadata/android`](fastlane/metadata/android), the layout F-Droid reads.

### Signing key

Release APKs are signed with this certificate (SHA-256), which you can check with `apksigner verify --print-certs` or AppVerifier:

```
E8:02:90:FA:C7:74:1D:B9:37:09:3D:62:EA:A4:D2:F9:1B:0C:58:44:CB:26:9A:10:28:E1:F1:CD:F7:90:A2:56
```

The key signs every future release: losing it means users must uninstall to update, so keep it in a password manager with an offline copy. Create it once:

```sh
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair -v \
  -keystore debout-release.jks -alias debout -keyalg RSA -keysize 4096 -validity 10950 -dname "CN=Debout"
```

Then set the repository secrets (each command without `--body` prompts for the value):

```sh
base64 -i debout-release.jks | gh secret set DEBOUT_KEYSTORE_BASE64
gh secret set DEBOUT_KEYSTORE_PASSWORD
gh secret set DEBOUT_KEY_ALIAS --body debout
gh secret set DEBOUT_KEY_PASSWORD   # the keystore password: PKCS12 keystores use one password for both
```

## License

[GPL-3.0](LICENSE)
