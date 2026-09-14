# Developer distribution

The developer distribution is built and published by **GitHub Actions**.

There is no local Android/iOS build-and-upload pipeline anymore.

## Recommended: run it from the browser

Open:

`https://github.com/filcdev/mergen/actions/workflows/developer-release.yml`

Then:

1. click **Run workflow**
2. select `main`
3. click the green **Run workflow** button
4. wait for the workflow to finish

The workflow builds on GitHub-hosted runners.

## Output

The workflow replaces the moving prerelease:

`https://github.com/filcdev/mergen/releases/tag/dev-latest`

Stable Android URL:

`https://github.com/filcdev/mergen/releases/download/dev-latest/Mergen-android-dev.apk`

### Android

GitHub builds:

`Mergen-android-dev.apk`

No local Java, Android SDK, Git Bash or Gradle build is needed to publish it.

### iOS

A macOS GitHub runner attempts to build:

`Mergen-ios-simulator.zip`

This is a **Simulator build** for developers. It is not installable on a physical
iPhone.

For physical iPhone/iPad testing we still need one of Apple's signed
distribution paths, for example a registered-device development/ad-hoc build,
TestFlight, or later an eligible alternative distribution setup.

## Optional command-line launchers

The repository still contains convenience launchers:

Windows CMD:

```cmd
release.cmd
```

Git Bash/macOS/Linux:

```bash
./release.sh
```

These launchers **do not build or upload anything locally**. They only ask
GitHub Actions to start the `Developer Release` workflow.

They require GitHub CLI authentication:

```bash
gh auth login
```

If GitHub CLI is not available, use the browser method instead.
