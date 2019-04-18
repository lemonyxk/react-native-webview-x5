# react-native-x5
X5's WebView for React Native on Android (Interfaces based on ReactNative's WebView Component)

### Dependencies

* 0.0.1： `react-native >= 0.33.0`, `react = 15.3.1`

### Installation

#### Install and Link

* Install from npm

```bash
npm i react-native-x5 --save

```

* Link native library

You can use react-native-cli:
```bash
react-native link react-native-x5
```

Or rnpm:
```bash
rnpm link react-native-x5
```

#### Add uses-permission

Add those lines to AndroidManifest.xml in your project.

```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<uses-permission android:name="android.permission.INTERNET" />

<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```


### Usage

#### Import

```
import WebView from 'react-native-x5';
```

#### Props

Totally same as [WebView](http://facebook.github.io/react-native/docs/webview.html) for react-native

Additional methods:

* WebView.getX5CoreVersion(callback) `static`
get X5's core version through callback


```
WebView.getX5CoreVersion(function callback (version) {
    console.log(version); // get `0` if X5 is not installed correctly
});
```


### Detail

[腾讯浏览器服务](http://x5.tencent.com/index)

