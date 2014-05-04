package com.google.android.gcm;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GCMRegistrar
{
  public static final long DEFAULT_ON_SERVER_LIFESPAN_MS = 604800000L;
  private static final String TAG = "GCMRegistrar";
  private static final String BACKOFF_MS = "backoff_ms";
  private static final String GSF_PACKAGE = "com.google.android.gsf";
  private static final String PREFERENCES = "com.google.android.gcm";
  private static final int DEFAULT_BACKOFF_MS = 3000;
  private static final String PROPERTY_REG_ID = "regId";
  private static final String PROPERTY_APP_VERSION = "appVersion";
  private static final String PROPERTY_ON_SERVER = "onServer";
  private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTime";
  private static final String PROPERTY_ON_SERVER_LIFESPAN = "onServerLifeSpan";
  private static GCMBroadcastReceiver sRetryReceiver;
  private static String sRetryReceiverClassName;
  
  public static void checkDevice(Context context)
  {
    int version = Build.VERSION.SDK_INT;
    if (version < 8) {
      throw new UnsupportedOperationException("Device must be at least API Level 8 (instead of " + version + ")");
    }
    PackageManager packageManager = context.getPackageManager();
    try
    {
      packageManager.getPackageInfo("com.google.android.gsf", 0);
    }
    catch (PackageManager.NameNotFoundException e)
    {
      throw new UnsupportedOperationException("Device does not have package com.google.android.gsf");
    }
  }
  
  public static void checkManifest(Context context)
  {
    PackageManager packageManager = context.getPackageManager();
    String packageName = context.getPackageName();
    String permissionName = packageName + ".permission.C2D_MESSAGE";
    try
    {
      packageManager.getPermissionInfo(permissionName, 4096);
    }
    catch (PackageManager.NameNotFoundException e)
    {
      throw new IllegalStateException("Application does not define permission " + permissionName);
    }
    PackageInfo receiversInfo;
    try
    {
      receiversInfo = packageManager.getPackageInfo(packageName, 2);
    }
    catch (PackageManager.NameNotFoundException e)
    {
      throw new IllegalStateException("Could not get receivers for package " + packageName);
    }
    ActivityInfo[] receivers = receiversInfo.receivers;
    if ((receivers == null) || (receivers.length == 0)) {
      throw new IllegalStateException("No receiver for package " + packageName);
    }
    if (Log.isLoggable("GCMRegistrar", 2)) {
      Log.v("GCMRegistrar", "number of receivers for " + packageName + ": " + receivers.length);
    }
    Set<String> allowedReceivers = new HashSet();
    for (ActivityInfo receiver : receivers) {
      if ("com.google.android.c2dm.permission.SEND".equals(receiver.permission)) {
        allowedReceivers.add(receiver.name);
      }
    }
    if (allowedReceivers.isEmpty()) {
      throw new IllegalStateException("No receiver allowed to receive com.google.android.c2dm.permission.SEND");
    }
    checkReceiver(context, allowedReceivers, "com.google.android.c2dm.intent.REGISTRATION");
    
    checkReceiver(context, allowedReceivers, "com.google.android.c2dm.intent.RECEIVE");
  }
  
  private static void checkReceiver(Context context, Set<String> allowedReceivers, String action)
  {
    PackageManager pm = context.getPackageManager();
    String packageName = context.getPackageName();
    Intent intent = new Intent(action);
    intent.setPackage(packageName);
    List<ResolveInfo> receivers = pm.queryBroadcastReceivers(intent, 32);
    if (receivers.isEmpty()) {
      throw new IllegalStateException("No receivers for action " + action);
    }
    if (Log.isLoggable("GCMRegistrar", 2)) {
      Log.v("GCMRegistrar", "Found " + receivers.size() + " receivers for action " + action);
    }
    for (ResolveInfo receiver : receivers)
    {
      String name = receiver.activityInfo.name;
      if (!allowedReceivers.contains(name)) {
        throw new IllegalStateException("Receiver " + name + " is not set with permission " + "com.google.android.c2dm.permission.SEND");
      }
    }
  }
  
  public static void register(Context context, String... senderIds)
  {
    resetBackoff(context);
    internalRegister(context, senderIds);
  }
  
  static void internalRegister(Context context, String... senderIds)
  {
    String flatSenderIds = getFlatSenderIds(senderIds);
    Log.v("GCMRegistrar", "Registering app " + context.getPackageName() + " of senders " + flatSenderIds);
    
    Intent intent = new Intent("com.google.android.c2dm.intent.REGISTER");
    intent.setPackage("com.google.android.gsf");
    intent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
    
    intent.putExtra("sender", flatSenderIds);
    context.startService(intent);
  }
  
  static String getFlatSenderIds(String... senderIds)
  {
    if ((senderIds == null) || (senderIds.length == 0)) {
      throw new IllegalArgumentException("No senderIds");
    }
    StringBuilder builder = new StringBuilder(senderIds[0]);
    for (int i = 1; i < senderIds.length; i++) {
      builder.append(',').append(senderIds[i]);
    }
    return builder.toString();
  }
  
  public static void unregister(Context context)
  {
    resetBackoff(context);
    internalUnregister(context);
  }
  
  public static synchronized void onDestroy(Context context)
  {
    if (sRetryReceiver != null)
    {
      Log.v("GCMRegistrar", "Unregistering receiver");
      context.unregisterReceiver(sRetryReceiver);
      sRetryReceiver = null;
    }
  }
  
  static void internalUnregister(Context context)
  {
    Log.v("GCMRegistrar", "Unregistering app " + context.getPackageName());
    Intent intent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
    intent.setPackage("com.google.android.gsf");
    intent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
    
    context.startService(intent);
  }
  
  static synchronized void setRetryBroadcastReceiver(Context context)
  {
    if (sRetryReceiver == null)
    {
      if (sRetryReceiverClassName == null)
      {
        Log.e("GCMRegistrar", "internal error: retry receiver class not set yet");
        sRetryReceiver = new GCMBroadcastReceiver();
      }
      else
      {
        try
        {
          Class<?> clazz = Class.forName(sRetryReceiverClassName);
          sRetryReceiver = (GCMBroadcastReceiver)clazz.newInstance();
        }
        catch (Exception e)
        {
          Log.e("GCMRegistrar", "Could not create instance of " + sRetryReceiverClassName + ". Using " + GCMBroadcastReceiver.class.getName() + " directly.");
          


          sRetryReceiver = new GCMBroadcastReceiver();
        }
      }
      String category = context.getPackageName();
      IntentFilter filter = new IntentFilter("com.google.android.gcm.intent.RETRY");
      
      filter.addCategory(category);
      
      String permission = category + ".permission.C2D_MESSAGE";
      Log.v("GCMRegistrar", "Registering receiver");
      context.registerReceiver(sRetryReceiver, filter, permission, null);
    }
  }
  
  static void setRetryReceiverClassName(String className)
  {
    Log.v("GCMRegistrar", "Setting the name of retry receiver class to " + className);
    sRetryReceiverClassName = className;
  }
  
  public static String getRegistrationId(Context context)
  {
    SharedPreferences prefs = getGCMPreferences(context);
    String registrationId = prefs.getString("regId", "");
    

    int oldVersion = prefs.getInt("appVersion", -2147483648);
    int newVersion = getAppVersion(context);
    if ((oldVersion != -2147483648) && (oldVersion != newVersion))
    {
      Log.v("GCMRegistrar", "App version changed from " + oldVersion + " to " + newVersion + "; resetting registration id");
      
      clearRegistrationId(context);
      registrationId = "";
    }
    return registrationId;
  }
  
  public static boolean isRegistered(Context context)
  {
    return getRegistrationId(context).length() > 0;
  }
  
  static String clearRegistrationId(Context context)
  {
    return setRegistrationId(context, "");
  }
  
  static String setRegistrationId(Context context, String regId)
  {
    SharedPreferences prefs = getGCMPreferences(context);
    String oldRegistrationId = prefs.getString("regId", "");
    int appVersion = getAppVersion(context);
    Log.v("GCMRegistrar", "Saving regId on app version " + appVersion);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("regId", regId);
    editor.putInt("appVersion", appVersion);
    editor.commit();
    return oldRegistrationId;
  }
  
  public static void setRegisteredOnServer(Context context, boolean flag)
  {
    SharedPreferences prefs = getGCMPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("onServer", flag);
    
    long lifespan = getRegisterOnServerLifespan(context);
    long expirationTime = System.currentTimeMillis() + lifespan;
    Log.v("GCMRegistrar", "Setting registeredOnServer status as " + flag + " until " + new Timestamp(expirationTime));
    
    editor.putLong("onServerExpirationTime", expirationTime);
    editor.commit();
  }
  
  public static boolean isRegisteredOnServer(Context context)
  {
    SharedPreferences prefs = getGCMPreferences(context);
    boolean isRegistered = prefs.getBoolean("onServer", false);
    Log.v("GCMRegistrar", "Is registered on server: " + isRegistered);
    if (isRegistered)
    {
      long expirationTime = prefs.getLong("onServerExpirationTime", -1L);
      if (System.currentTimeMillis() > expirationTime)
      {
        Log.v("GCMRegistrar", "flag expired on: " + new Timestamp(expirationTime));
        return false;
      }
    }
    return isRegistered;
  }
  
  public static long getRegisterOnServerLifespan(Context context)
  {
    SharedPreferences prefs = getGCMPreferences(context);
    long lifespan = prefs.getLong("onServerLifeSpan", 604800000L);
    
    return lifespan;
  }
  
  public static void setRegisterOnServerLifespan(Context context, long lifespan)
  {
    SharedPreferences prefs = getGCMPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putLong("onServerLifeSpan", lifespan);
    editor.commit();
  }
  
  private static int getAppVersion(Context context)
  {
    try
    {
      PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      
      return packageInfo.versionCode;
    }
    catch (PackageManager.NameNotFoundException e)
    {
      throw new RuntimeException("Coult not get package name: " + e);
    }
  }
  
  static void resetBackoff(Context context)
  {
    Log.d("GCMRegistrar", "resetting backoff for " + context.getPackageName());
    setBackoff(context, 3000);
  }
  
  static int getBackoff(Context context)
  {
    SharedPreferences prefs = getGCMPreferences(context);
    return prefs.getInt("backoff_ms", 3000);
  }
  
  static void setBackoff(Context context, int backoff)
  {
    SharedPreferences prefs = getGCMPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt("backoff_ms", backoff);
    editor.commit();
  }
  
  private static SharedPreferences getGCMPreferences(Context context)
  {
    return context.getSharedPreferences("com.google.android.gcm", 0);
  }
  
  private GCMRegistrar()
  {
    throw new UnsupportedOperationException();
  }
}
