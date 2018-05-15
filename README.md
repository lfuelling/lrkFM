# lrkFM ![badge](https://travis-ci.org/lfuelling/lrkFM.svg?branch=master)

File manager for Android. It has the following features:

- set the directory the app starts in
- full access to filesystem (if permissions are right)
- can create zip archives
- can extract zip, rar, 7zip, tar, tar.gz archives
- can explore all of the above archives
- file operations (move and so on)
- shows free space on filesystem
- add bookmarks to the sidebar
- share files from app
- ad free
- open source

## Building

### Preparation

When building for the first time, gradle will probably complain about a missing `google-services.json` file. 
To solve this, simply go to https://console.firebase.google.com and create a new Android project there to download the needed file.

Alternatively you can try to build the `noogle`-branch where I removed all teh Google stuff. But please note that the branch is based on 1.6.2 and probably won't be updated.

## Usage

### Permissions

Obviously this app needs full access to the filesystem to work. This is checked and prompted if needed when starting the app.

You also will notice that the app needs network permissions and the permission to prevent the phone from sleeping. Those permissions are only needed by Firebase (Google stuff) which I use to collect crash dumps and performance metrics, if the submission of those is enabled in the app's settings. Because those permissions are not needed by lrkFM directly, you also won't find them in the local manifest. But when you build the master branch yourself, you'll see that they are added to the manifest at compile time.

### Operation Modes

There are two main types of operation. The first one will prompt for a destination path when triggering a file operation. The second one will remember the file you want to copy, move, etc. and let's you navigate to the target folder (or create it) and then trigger execution of the operation.

## Screenshots

![Screenshot](screenshots/Screenshot_1.png)
![Screenshot](screenshots/Screenshot_2.png)
![Screenshot](screenshots/Screenshot_3.png)
![Screenshot](screenshots/Screenshot_4.png)

[![Get it on GooglePlay](https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=io.lerk.lrkFM&utm_source=repo_link&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)

You can join the beta program through [here.](https://play.google.com/apps/testing/io.lerk.lrkfm)

**Please note:** This project was moved from my personal GitLab instance to GitHub. Issue mentions in commits might be misleading!

Logo generated with software by [Philipp Eichhorn](https://android-material-icon-generator.bitdroid.de)

Google Play and the Google Play logo are trademarks of Google Inc.
