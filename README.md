# Tella Android (fork)

This repository is a **fork of [Horizontal-org/Tella-Android](https://github.com/Horizontal-org/Tella-Android)** with additional privacy and documentation features on top of upstream Tella. See [Fork additions](#fork-additions) below.

## Table of Contents

1. [Overview](#about)

2. [Why Tella?](#why-tella)

3. [Detailed list of features](#features)

4. [Fork additions](#fork-additions)

5. [How to get Tella and start using it?](#use-tella)

6. [Tech & frameworks used](#tech-used)

7. [Contributing to the code](#contributing)

8. [Translating the app](#translating)

9. [Contact us](#contact)

## About Tella <a id="about"></a>

In challenging environments, with limited or no internet connectivity or in the face of repression, Tella is an app that makes it easier and safer to document human rights violations and collect data. Tella is available Android and iOS. 

More information about how to get Tella --including user guides-- can be found on our [documentation platform](https://tella-app.org/docs).

Tella:
- encrypts photo, video, documents and audio files in a separate gallery so it cannot be accessed from the phone's regular gallery or file explorer.
- hides itself by changing its name and icon in the list of apps.
- captures metadata when taking photos, videos, and audio recordings to verify the origin of the files.
- allows users to quickly delete all files in Tella's encrypted Gallery.
- enables users working with a group or organization to collect and send data to a server without relying on third-party apps or servers.

You can watch a [short video demonstrating Tella's main features here](https://www.youtube.com/watch?v=aJIyWESxM_o&t=1s)


## Why Tella? <a id="why-tella"></a>

Tella's goal is to protect at-risk individuals and groups--advocates, journalists, human rights defenders--from repressive surveillance, whether physical or digital. Tella aims to provide a highly usable solution, accessible to all with minimal or no training, to collect, safeguard, and communicate sensitive information in highly repressive environments.

Tella has three main objectives:

- Protecting users who engage in documentation from physical and digital repression
- Protecting the data they collect from censorship, tampering, interception, and destruction
- Empowering individuals and groups to easily, quickly, and effectively collect data and produce high quality documentation that can be used for research, advocacy, or transitional justice

Tella is used by:
- Activists, organizers and human rights defenders to safely document events in their communities, produce reliable and verifiable evidence, and store data encrypted on their mobile devices.
- Media, professional reporters and citizen journalists to store sensitive media files encrypted as they travel, particularly as they cross borders.
- Civil society professionals and humanitarian workers to conduct interviews and collect data in poorly connected environments or in conflict areas.
- Electoral observation and monitoring organizations to monitor elections from inside and outside polling stations in real time and expose electoral fraud.
- Research institutions and international organizations to conduct research, interviews or surveys in challenging environments, particularly in conflict areas.

You can read usage stories [here](https://tella-app.org/user-stories).

## Detailed list of features <a id="features"></a>

A detailed list of all Tella features can be found here: https://tella-app.org/features. 

## Fork additions <a id="fork-additions"></a>

The following features are additions made in this fork and are not part of upstream Tella:

### Enhanced PDF Reader
The built-in PDF reader (`pdfviewer` module) has been extended into an annotation-capable reader:
- **Reading state persistence** — the last read page (and scroll position) is remembered per document, so you resume where you left off.
- **Go to page** — jump directly to any page via a page-number input.
- **Text-to-Speech (read aloud)** — have the document's text read aloud with Android's TTS engine.
- **Text highlighting** — highlight passages of text; highlights are saved per document.
- **Sticky notes** — attach sticky notes to any point in a document.

Annotations are stored encrypted alongside the vault file and are flattened into the document when exporting/sharing.

### Secure Wipe on Import
Optional setting (*Settings → Security → "Use secure wipe on import"*). When enabled, after importing a file into Tella's encrypted vault, you are prompted to **securely wipe the original**: the original unencrypted file is overwritten with random data before deletion, so it is forensically unrecoverable from the device storage — instead of a standard deletion that leaves the bytes intact.

### Quick Delete PIN (duress PIN)
A dedicated secondary PIN that can be set in *Settings → Security*. Entering this PIN at any lock screen (PIN, password, pattern, or calculator camouflage) instantly triggers Quick Delete — wiping all Tella data. This lets you comply under duress while protecting the contents of the vault. The PIN is stored only as a SHA-256 hash.

> This fork previously shipped an experimental brute-force auto-trigger (auto Quick Delete after X wrong attempts in Y minutes). It was removed because it duplicated the existing **"Delete after failed unlock"** protection, which deletes all data after a configurable number of failed unlock attempts.

### In-App Secure Browser
A sandboxed, minimalist browser built into Tella (accessible from the vault attachments screen) designed to leave no trace outside Tella:
- Runs in a heavily restricted `WebView`: no file/content access to the device, third-party cookies blocked, all SSL certificate errors cancelled for security.
- On close, a forensic cleanup wipes the WebView's history, cache, form data, cookies, and DOM storage — nothing leaks to the device's regular browser.
- Downloads are intercepted and imported **directly into Tella's encrypted vault** (never the public Downloads folder); the temporary plaintext copy is securely deleted after encryption.



## How to get Tella and start using it? <a id="use-tella"></a>

Tella is currently available on Android, iOS and the F-Droid store. We also share the Tella .apk to be installed manually. [Here there is more information](https://tella-app.org/faq#general). A step-by-step guide on how to use Tella can be found [here](https://tella-app.org/get-started-android).


## Detailed list of features <a id="features"></a>

A detailed list of features for Tella Android, Tella Android FOSS and Tella iOS can be found [on the documentation](https://tella-app.org/features).

## How to get Tella and start using it? <a id="use-tella"></a>

### Tella for Android
Tella for Android can be downloaded:
- directly from the [Google Play Store](https://play.google.com/store/apps/details?id=org.hzontal.tella).
- from [this folder](https://web.tresorit.com/l/JgMjK#FV9IoIZdDxwAUPqtupJzsQ) or from our [Telegram channel](https://t.me/tellaapp), as an APK, to be installed manually.

### Tella Android FOSS (F-Droid)
We also maintain **Tella Android FOSS** in the [F-Droid store](https://f-droid.org/en/packages/org.hzontal.tellaFOSS/) (`org.hzontal.tellaFOSS`). It is built from **this repository** using the `fdroid` product flavor (see below). The former [Tella-Android-FOSS](https://github.com/Horizontal-org/Tella-Android-FOSS) codebase has been merged here.

### Play Store vs F-Droid (build flavors)
**Tella Android** and **Tella Android FOSS** share one codebase. Two **`distribution`** flavors in `mobile/build.gradle`:

| Flavor | Name | Application id | Notes |
|--------|------|----------------|-------|
| **`playstore`** | Tella Android | `org.hzontal.tella` | Includes Firebase Crashlytics, Google Drive, Dropbox, and Google sign-in |
| **`fdroid`** | Tella Android FOSS | `org.hzontal.tellaFOSS` | Excludes proprietary Google and Dropbox SDKs; published on F-Droid |

Details about features available in each version can be found on the [Tella documentation](https://tella-app.org/features).

F-Droid release builds pass `-Pfdroid` (see `fastlane/README.md` and `docs/fdroid-release-verification.md`).


### Get started on Tella Android
A get started guide for Tella Android is available [here](https://tella-app.org/get-started-android).


## Tech & frameworks used <a id="tech-used"></a>

This software uses the following open source packages:
- [SQLCipher](https://github.com/sqlcipher/sqlcipher) for our encrypted database.
- [CacheWord](https://guardianproject.info/code/cacheword/) for passphrase caching and management.
- [ODK JavaRosa](https://github.com/getodk/javarosa) to work with XForms.
- [CameraX](https://developer.android.com/jetpack/androidx/releases/camera), [ExoPlayer](https://github.com/google/ExoPlayer), [RxJava](https://github.com/ReactiveX/RxJava), [OkHttp](https://github.com/square/okhttp), [Retrofit](https://github.com/square/retrofit), [Glide](https://github.com/bumptech/glide), [Hilt](https://dagger.dev/hilt/), [Ktor](https://ktor.io/), [WorkManager](https://developer.android.com/jetpack/androidx/releases/work), [Mapsforge](https://github.com/mapsforge/mapsforge), [Nextcloud Android library](https://github.com/nextcloud/android), [PermissionDispatcher](https://github.com/permissions-dispatcher/PermissionsDispatcher), [PatternLock](https://github.com/zhanghai/PatternLock) (vendored in `tella-locking-ui`), and other libraries listed in [mobile/build.gradle](mobile/build.gradle).

## Contributing to the code <a id="contributing"></a>

**Step 1: Get familiar with Tella.** The best way is simply to download Tella play with it and try the different features, or [read our documentation here](https://docs.tella-app.org).

**Step 2: Find an issue to work on.** Please find an issue that you would like to take on and comment to assign yourself if no one else has done so already.  Also, feel free to ask us about priorities, general guidance, and we will get back to you ASAP!

**Step 3: Fork the repo** Click the "fork" button in the upper right of the Github repo page. A fork is a copy of the repository that allows you to freely explore & experiment without changing the original project. You can learn more about forking a repo [in this article](https://help.github.com/articles/fork-a-repo/).

**Step 4: Create a branch** Create a new branch for your issue from `develop` branch. You can name it anything, but we encourage you to use the format `XXX-brief-description-of-feature` where XXX is the issue number.

**Step 5: Code away!** Feel free to discuss any questions on the issues as needed, and we will get back to you! Don't forget to write some tests to verify your code. Commit your changes locally, using descriptive messages and please be sure to note the parts of the app that are affected by this commit.

**Step 6: Pushing your branch and creating a pull request** Push your branch up and create a pull request. Please indicate which issue your PR addresses in the title.

## Translating the app <a id="translating"></a>
Tella is currently available in [more than 25 languages](https://tella-app.org/translating-tella). We are always looking to translate Tella into more languages. 

If you are interested in adding a new language, or if you noticed a mistake or a missing translation, you can join [follow our contributing guidelines](https://tella-app.org/translating-tella/#how-do-i-become-a-translator). 

## Contact us <a id="contact"></a>
We love hearing from users, designers, and developers!

We offer different ways to [contact us](https://tella-app.org/contact-us). 

 If you have any question, ideas or suggestions on how we can improve or what new features we should add, or if you need support deploying Tella, don't hesitate to reach out!


