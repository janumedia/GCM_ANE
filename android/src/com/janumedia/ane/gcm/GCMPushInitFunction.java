package com.janumedia.ane.gcm;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;

public class GCMPushInitFunction implements FREFunction {

	@Override
	public FREObject call(FREContext context, FREObject[] args) {
		
		String message = "false";
		
		Intent launchIntent = context.getActivity().getIntent();
		
		if(launchIntent != null)
		{
			Bundle ex = launchIntent.getExtras();
			if (ex != null)
			{
				message = launchIntent.getStringExtra("data");
				
				// remove pending after loaded
				launchIntent.removeExtra("data");
				
				// clear notification
				int notifyID = 1;
				Context appContext = context.getActivity();
				
				cancelNotification(appContext, notifyID);
			}
		}
		
		try
		{
			return FREObject.newObject(message);
		} catch (FREWrongThreadException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void cancelNotification(Context context, int notifyID) {
		String ns = Context.NOTIFICATION_SERVICE;
	    NotificationManager nMgr = (NotificationManager) context.getSystemService(ns);
	    nMgr.cancel(notifyID);
	}
	

}
