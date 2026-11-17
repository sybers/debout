# Debout

Wake your computers from your Android phone. Beautiful, ergonomic, free and open source.

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

The pairing scripts have [Pester](https://pester.dev) tests, run by CI on Windows:

```powershell
Invoke-Pester -Path scripts
```

## Releases

Pushing a `v*` tag builds a signed APK and publishes it on GitHub Releases. Signing uses these repository secrets:

| Secret | Value |
|---|---|
| `DEBOUT_KEYSTORE_BASE64` | The release keystore, base64-encoded |
| `DEBOUT_KEYSTORE_PASSWORD` | Keystore password |
| `DEBOUT_KEY_ALIAS` | Key alias |
| `DEBOUT_KEY_PASSWORD` | Key password |

## License

[GPL-3.0](LICENSE)
