# Debout

Android Wake-on-LAN app. Product spec: [docs/spec.md](docs/spec.md). Implementation plan: [docs/plan.md](docs/plan.md) — read both before starting work.

## Build

Use Android Studio's bundled JDK and the user's SDK:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME="$HOME/Library/Android/sdk"
cd android && ./gradlew assembleDebug lint testDebugUnitTest
```

AGP 9 has built-in Kotlin support: do not add the `org.jetbrains.kotlin.android` plugin or a `kotlinOptions` block.

## Conventions

- New dependencies: latest stable version published at least two weeks ago.
- Every user-facing string goes in `values/strings.xml` (English) and `values-fr/strings.xml` (French).
- No proprietary dependency (no Google Play Services): the app must stay F-Droid compatible.
