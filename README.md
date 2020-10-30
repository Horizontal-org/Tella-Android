<img src="https://github.com/Horizontal-org/Tella-Android/blob/master/Tella-02-feature-B.png" alt="Tella" width="350"/>

## Table of Contents

1. [Overview](#overview)

2. [Why Tella?](#why-tella)

3. [Detailed list of features](#features)

4. [How to get Tella and start using it?](#use-tella)

5. [Tech & frameworks used](#tech-used)

6. [Contributing to the code](#contributing)

7. [Translating the app](#translating)

8. [Contact us](#contact)

## Overview <a id="overview"></a>

Tella is a documentation app for Android. In challenging environments--with limited or no internet connectivity or in the face of repression--Tella makes it easier and safer to document events, whether that’s violence, human rights violations, corruption, or electoral fraud.

| [![Camouflage](docs/Camouflage.gif)](https://hzontal.org/tella)  | [![Encrypting](docs/Encrypting.gif)](https://hzontal.org/tella) | [![Collecting data](docs/data_collection.gif)](https://hzontal.org/tella) |
|:---:|:---:|:---:|
| Tella camouflaged as a calculator | Taking and encrypting a photo | Collecting data |

Tella:
- encrypts photo, video, and audio files in a separate gallery so it cannot be accessed from the phone's regular gallery or file explorer
- hides itself by changing its name and icon in the list of apps
- captures metadata when taking photos, videos, and audio recordings to verify the origin of the files
- allows users to quickly delete all files in Tella's encrypted Gallery
- enables users working with a group or organization to collect and send data to a server without relying on third-party apps or servers

You can watch a [short video demonstrating Tella's main features here](https://vimeo.com/344220220/8c7f2fba67)

## Why Tella? <a id="why-tella"></a>

Across the world, journalists and human rights defenders are facing increasing levels of physical repression, with mobile devices searched or seized at border crossings and airports, checkpoints, in the street, or in targeted raids. At the same time, digital surveillance and censorship threaten the flow of information out of repressive areas, particularly on violence, human rights abuse, or corruption. 

Tella's goal is to protect at-risk individuals and groups--advocates, journalists, human rights defenders--from repressive surveillance, whether physical or digital. Tella aims to provide a highly usable solution, accessible to all with minimal or no training, to collect, safeguard, and communicate sensitive information in highly repressive environments. 

Tella has three main objectives:
- Protecting users who engage in documentation from physical and digital repression
- Protecting the data they collect from censorship, tampering, interception, and destruction
- Empowering individuals and groups to easily, quickly, and effectively collect data and produce high quality documentation that can be used for research, advocacy, or transitional justice

Tella is used by:
- Activists, organizers and human rights defenders to safely document events in their communities, produce reliable and verifiable evidence, and store data encrypted on their mobile devices
- Media, professional reporters and citizen journalists to store sensitive media files encrypted as they travel, particularly as they cross borders
- Civil society professionals and humanitarian workers to conduct interviews and collect data in poorly connected environments or in conflict areas
- Electoral observation and monitoring organizations to monitor elections from inside and outside polling stations in real time and expose electoral fraud
- Research institutions and international organizations to conduct research, interviews or surveys in challenging environments, particularly in conflict areas

## Detailed list of features <a id="features"></a>

### Encrypted container
All of the content and data stored in Tella are encrypted. This means that unless the app is unlocked (using the 6-point pattern set up by the user upon installation), all the data will remain inaccessible to someone seizing or searching the device. Even if it is plugged into a computer and all of the device's data is extracted to be analyzed, all Tella content and data will look like gibberish and will be useless. Unlocking the app by entering the correct pattern is the only way to decrypt, and therefore read, the content stored in Tella.

### Camouflage
In order to protect the data produced and shared through Tella, the application and its content can be hidden on the user's device.
The app's icon can be changed to a seemingly harmless one, such as a calcultor or a camera. This means that an individual searching the device will not see the Tella name and icon, and instead see an app that doesn't raise suspicions.
WARNING: the name and icon of Tella will remain visible in the Android settings; this means that this camouflage will not protect against an individual actively looking for Tella or conducting an in-depth analysis of the device. This is a good protection against superficial searches of the user's device, avoiding to raise suspicions if an individual is quickly searching for incriminating evidence.

### Data collection
Users working with an organization can upload photos, videos, and audio files, or fill and submit forms, to document events they are witnessing. The organization decides which method is most appropriate to their needs and capacity: receiving the files through a third-party app like WhatsApp or Signal; or using forms get ask questions about the specific data they need. 

Previous Tella deployments have ranged between 10 and 2,000 users. 
Tella includes an "Offline Mode" for users who are collecting data in remote areas with limited or no internet connection. When Offline Mode is activated, all data is saved on the app and and users can submit it easily when they reach an internet connection.

### Wipe data
A Quick Delete button allows users, in a just a few seconds, to delete sensitive data within Tella. The button can also be set to delete the app itself.

### Verification
In the Tella settings, users can activate "Verification Mode". When activated, every time a user takes a photo or a video, or records audio, Tella automatically captures metadata about the file. This metadata can be used to corroborate evidence, cross check with other facts known about the event or about the area where it was captured.

## How to get Tella and start using it? <a id="use-tella"></a>

Tella is currently available only on Android. You can [download it](https://play.google.com/store/apps/details?id=org.hzontal.tella) from the Google Play Store or [get the APK from here](https://web.tresorit.com/l/7737s#WKoGVN82C0lQK8KSL30CuA) to install manually on your device.

## Tech & frameworks used <a id="tech-used"></a>

This software uses the following open source packages:
- [SQLCipher](https://github.com/sqlcipher/sqlcipher) for our encrypted database.
- [CacheWord](https://guardianproject.info/code/cacheword/) for passphrase caching and management.
- [ODK JavaRosa](https://github.com/getodk/javarosa) to work with XForms.
- [CameraView](https://github.com/natario1/CameraView), [ExoPlayer](https://github.com/google/ExoPlayer), [RxJava](https://github.com/ReactiveX/RxJava), [OkHttp](https://github.com/square/okhttp), [Retrofit](https://github.com/square/retrofit), [PermissionDispatcher](https://github.com/permissions-dispatcher/PermissionsDispatcher), [PatternLock](https://github.com/zhanghai/PatternLock) and a lot of other excellent [libraries](https://github.com/Horizontal-org/Tella-Android/blob/master/mobile/build.gradle) helping all of us in Android application development.

## Contributing to the code <a id="contributing"></a>

**Step 1: Get familiar with Tella.** The best way is simply to download Tella play with it and try the different features, or [read our documentation here](https://docs.tella-app.org).

**Step 2: Find an issue to work on.** Please find an issue that you would like to take on and comment to assign yourself if no one else has done so already. [All issues with the label `good first issue`](https://github.com/Horizontal-org/Tella-Android/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) are good ways to get started. Also, feel free to ask questions in the issues, and we will get back to you ASAP!

**Step 3: Fork the repo** Click the "fork" button in the upper right of the Github repo page. A fork is a copy of the repository that allows you to freely explore & experiment without changing the original project. You can learn more about forking a repo [in this article](https://help.github.com/articles/fork-a-repo/).

**Step 4: Create a branch** Create a new branch for your issue from `develop` branch. You can name it anything, but we encourage you to use the format `XXX-brief-description-of-feature` where XXX is the issue number.

**Step 5: Code away!** Feel free to discuss any questions on the issues as needed, and we will get back to you! Don't forget to write some tests to verify your code. Commit your changes locally, using descriptive messages and please be sure to note the parts of the app that are affected by this commit.

**Step 6: Pushing your branch and creating a pull request** Push your branch up and create a pull request. Please indicate which issue your PR addresses in the title.

## Translating the app <a id="translating"></a>
Tella is currently available in Belarusian, English, French, Portuguese, Russian, and Spanish. We are always looking to translate Tella into more languages. 

If you are interested in adding a new language, or if you noticed a mistake in our translation, you can join [the Tella project on Transifex](https://www.transifex.com/otf/tella/dashboard/) and contribute from there. 

## Contact us <a id="contact"></a>
We love hearing from users, designers, and developers! If you have any question, ideas or suggestions on how we can improve or what new features we should add, or if you need support deploying Tella, don't hesitate to reach out!

You can create an issue [here on our Github](https://github.com/H0rizontal/Tella/issues) or email us at contact@tella-app.org. 

