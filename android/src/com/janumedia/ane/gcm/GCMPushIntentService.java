package com.janumedia.ane.gcm;

import java.util.List;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.adobe.fre.FREContext;
import com.distriqt.extension.util.Resources;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMPushIntentService extends GCMBaseIntentService {

	@Override
	protected void onRegistered(Context context, String regID) {
		FREContext freContext = GCMPushExtension.context;
		freContext.dispatchStatusEventAsync("registered", regID);
	}
	
	@Override
	protected void onUnregistered(Context context, String regID) {
		FREContext freContext = GCMPushExtension.context;
		freContext.dispatchStatusEventAsync("unregistered", regID);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Bundle ex = intent.getExtras();
		String title = ex.getString("title");
		String alert = ex.getString("alert");
		String type = ex.getString("type");
		String id = ex.getString("id");
		String payload = "type:" + type + ", id:" + id;
		
		FREContext freContext = GCMPushExtension.context;
		Context appContext;
		
		if (freContext != null)
		{
			//freContext.dispatchStatusEventAsync("foregroundMessage", payload);
			freContext.dispatchStatusEventAsync("message", payload);
			appContext = freContext.getActivity();
		} else 
		{
			appContext = context.getApplicationContext();
		}
		
		if (!isAppInForeground(appContext))
		{
			String ns = NOTIFICATION_SERVICE;
			
			NotificationManager mNotificationManager = (NotificationManager)appContext.getSystemService(ns);
			
			int icon = Resources.getResourseIdByName(appContext.getPackageName(), "drawable", "notify");
			CharSequence tickerText = alert;
			long when = System.currentTimeMillis();
			
			Notification notification = new Notification(icon, tickerText, when);
			notification.defaults |= Notification.DEFAULT_SOUND;
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			//notification.flags |= 0x10;
			
			CharSequence contentTitle = title == null ? "Incoming Notification" : title;
			CharSequence contentText = alert;
			
			try
			{
				Intent notificationIntent = new Intent(appContext, Class.forName(appContext.getPackageName() + ".AppEntry"));
				//notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				notificationIntent.putExtra("data", payload);
				notificationIntent.setAction(Long.toString(System.currentTimeMillis()));
				
				PendingIntent contentIntent = PendingIntent.getActivity(appContext, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
				//PendingIntent contentIntent = PendingIntent.getActivity(appContext, 0, notificationIntent, 134217728);
				
				notification.setLatestEventInfo(appContext, contentTitle, contentText, contentIntent);
				
				int HELLO_ID = 1;
				
				mNotificationManager.notify(HELLO_ID, notification);
				
			} catch (IllegalStateException e)
			{
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
	}

	@Override
	protected void onError(Context context, String errorID) {
		FREContext freContext = GCMPushExtension.context;
		freContext.dispatchStatusEventAsync("error", errorID);
	}
	
	@Override
	protected boolean onRecoverableError(Context context, String errorID)
	{
		FREContext freContext = GCMPushExtension.context;
		freContext.dispatchStatusEventAsync("recoverableError", errorID);
		
		return super.onRecoverableError(context, errorID);
	}
	
	private boolean isAppInForeground (Context context)
	{
		ActivityManager activityManager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		
		if (appProcesses != null)
		{
			String packageName = context.getPackageName();
			for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses)
			{
				if((appProcess.importance == 100) && (appProcess.processName.equals(packageName)))
				{
					return true;
				}
			}
		}
		
		return false;
	}

	

}
