# Developer distribution

Mergen uses a moving GitHub prerelease called:

`dev-latest`

Release page:

`https://github.com/filcdev/mergen/releases/tag/dev-latest`

Because the repository is public, developers/testers can download the release
assets without being added to a private file server.

## Stable Android URL

`https://github.com/filcdev/mergen/releases/download/dev-latest/Mergen-android-dev.apk`

The file at that URL is replaced whenever `release.sh` is run successfully.

## Build and publish

From the repository root:

```bash
./release.sh
```

Requirements for publishing:

```bash
gh auth login
```

### Windows / Linux

The script builds and publishes Android.

Run it from Git Bash / WSL / a Unix-like shell.

### macOS

The script builds:

- Android APK
- iOS Simulator app ZIP
- signed iOS device IPA when `APPLE_TEAM_ID` is configured

Copy:

`.release.env.example`

to:

`.release.env`

and set your Apple Team ID if you want a signed IPA.

`.release.env` is ignored by Git.

## iOS limitations

An iOS `.ipa` is not equivalent to an Android `.apk`.

For ordinary development/ad-hoc distribution, the target iPhone/iPad must be
included in the Apple provisioning setup. Apple also requires Developer Mode
for locally installed development apps.

Without a paid Apple Developer team, other developers can still use the
`Mergen-ios-simulator.zip` artifact on an iOS Simulator, or clone the repo and
sign the app locally with their own Xcode team.

Apple's EU Web Distribution is a different, notarized distribution mechanism.
It requires Apple authorization and App Store Connect setup; simply hosting an
IPA on GitHub does not make it web-installable on arbitrary iPhones.

## Useful commands

Local build without publishing:

```bash
./release.sh --local
```

Android only:

```bash
./release.sh --android-only
```

Development IPA:

```bash
./release.sh --ios-method development
```

Ad Hoc IPA:

```bash
./release.sh --ios-method ad-hoc
```
