package com.janumedia.ane.gcm;

import android.content.Context;

import com.google.android.gcm.GCMBroadcastReceiver;

public class GCMPushBroadcastReceiver extends GCMBroadcastReceiver {

	@Override
	protected String getGCMIntentServiceClassName(Context context)
	{
		String intentClassName = super.getGCMIntentServiceClassName(context);
		intentClassName = "com.janumedia.ane.gcm.GCMPushIntentService";
		
		return intentClassName;
	}
}
