# Telesam
Simple app that sends all SMS messages to selected Telegram chat

## Getting Started

- Install Kotlin plugin if you are using Android Studio prior to 3.0:
> Android Studio Pref > Plugins > Browse Repositories > search for “Kotlin” > Restart IDE

- Clone this repo:

```sh
git clone git@github.com:J-rooft/Telesam.git
```

- Open the project in Android Studio.
- Create your Telegram application as described [here](https://core.telegram.org/api/obtaining_api_id) and copy your app api_id and api_hash to your projects local.properties file:
```
...
apiKey=your_api_key
apiHash=your_api_hash
...
```

## Doze mode

This app asks a user to run in background to be able to send SMS messages to Telegram at the time they received. If this permission is not granted and device in doze mode then messages will be sent not immediately but instead at next doze mode's maintenance window.

Please note that Google may remove your app from play store if they decide that your app should not ask for such permission: [link](https://developer.android.com/training/monitoring-device-state/doze-standby.html#whitelisting-cases)