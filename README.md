GCM / Push Notification ANE for Android
======================================

GCM_ANE - [GCM (Google Cloud Messaging)] / Push Notification Native Extension for Android applications

This ANE based on [Afterick's idea](http://afterisk.wordpress.com/2012/09/22/the-only-free-and-fully-functional-android-gcm-native-extension-for-adobe-air/).

Version
---------

This is version 1.0 of this extension.

Extension ID
---------
```
<extensionID>com.janumedia.ane.gcm</extensionID>
```

Usage
---------
```
var gcm:GCMExtension; = new GCMExtension();
//trace (GCMExtension.VERSION);
gcm.addEventListener(GCMEvent.REGISTERED, handleRegistered, false, 0, true);
gcm.addEventListener(GCMEvent.UNREGISTERED, handleUnregistered, false, 0, true);
gcm.addEventListener(GCMEvent.MESSAGE, handleMessage, false, 0, true);
gcm.addEventListener(GCMEvent.ERROR, handleError, false, 0, true);
gcm.addEventListener(GCMEvent.RECOVERABLE_ERROR, handleError, false, 0, true);
gcm.register(GCM_SENDER_ID);
```

Make sure to add this permission on your manifest file:

```
<!-- App receives GCM messages. -->
<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
<!-- GCM connects to Google Services. -->
<uses-permission android:name="android.permission.INTERNET" />
<!-- GCM requires a Google account. -->
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
<permission android:name="air.YOUR_APP_BUNDLE_ID_HERE.permission.C2D_MESSAGE" android:protectionLevel="signature" />
<uses-permission android:name="air.YOUR_APP_BUNDLE_ID_HERE.permission.C2D_MESSAGE" />
```
Add inside Application tag, update YOUR_APP_BUNDLE_ID_HERE.

```
<service android:name="com.janumedia.ane.gcm.GCMPushIntentService" />
<receiver android:name="com.janumedia.ane.gcm.GCMPushBroadcastReceiver" android:permission="com.google.android.c2dm.permission.SEND" >
	<intent-filter>
		<action android:name="com.google.android.c2dm.intent.RECEIVE" />
		<action android:name="com.google.android.c2dm.intent.REGISTRATION" />
		<category android:name="YOUR_APP_BUNDLE_ID_HERE" />
	</intent-filter>
</receiver>
```

More information about GCM you can start from [here](http://developer.android.com/guide/google/gcm/index.html)

Author
---------

This ANE has been writen by [I Nengah Januartha](https://github.com/janumedia). It belongs to [JanuMedia Inc.](http://www.janumedia.com) and is distributed under the [Apache Licence, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).