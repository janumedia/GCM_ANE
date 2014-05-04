package com.google.android.gcm;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public abstract class GCMBaseIntentService
  extends IntentService
{
  public static final String TAG = "GCMBaseIntentService";
  private static final String WAKELOCK_KEY = "GCM_LIB";
  private static PowerManager.WakeLock sWakeLock;
  private static final Object LOCK = GCMBaseIntentService.class;
  private final String[] mSenderIds;
  private static int sCounter = 0;
  private static final Random sRandom = new Random();
  private static final int MAX_BACKOFF_MS = (int)TimeUnit.SECONDS.toMillis(3600L);
  private static final String TOKEN = Long.toBinaryString(sRandom.nextLong());
  private static final String EXTRA_TOKEN = "token";
  
  protected GCMBaseIntentService()
  {
    this(getName("DynamicSenderIds"), null);
  }
  
  protected GCMBaseIntentService(String... senderIds)
  {
    this(getName(senderIds), senderIds);
  }
  
  private GCMBaseIntentService(String name, String[] senderIds)
  {
    super(name);
    this.mSenderIds = senderIds;
  }
  
  private static String getName(String senderId)
  {
    String name = "GCMIntentService-" + senderId + "-" + ++sCounter;
    Log.v("GCMBaseIntentService", "Intent service name: " + name);
    return name;
  }
  
  private static String getName(String[] senderIds)
  {
    String flatSenderIds = GCMRegistrar.getFlatSenderIds(senderIds);
    return getName(flatSenderIds);
  }
  
  protected String[] getSenderIds(Context context)
  {
    if (this.mSenderIds == null) {
      throw new IllegalStateException("sender id not set on constructor");
    }
    return this.mSenderIds;
  }
  
  protected abstract void onMessage(Context paramContext, Intent paramIntent);
  
  protected void onDeletedMessages(Context context, int total) {}
  
  protected boolean onRecoverableError(Context context, String errorId)
  {
    return true;
  }
  
  protected abstract void onError(Context paramContext, String paramString);
  
  protected abstract void onRegistered(Context paramContext, String paramString);
  
  protected abstract void onUnregistered(Context paramContext, String paramString);
  
  public final void onHandleIntent(Intent intent)
  {
    try
    {
      Context context = getApplicationContext();
      String action = intent.getAction();
      if (action.equals("com.google.android.c2dm.intent.REGISTRATION"))
      {
        GCMRegistrar.setRetryBroadcastReceiver(context);
        handleRegistration(context, intent);
      }
      else if (action.equals("com.google.android.c2dm.intent.RECEIVE"))
      {
        String messageType = intent.getStringExtra("message_type");
        if (messageType != null)
        {
          if (messageType.equals("deleted_messages"))
          {
            String sTotal = intent.getStringExtra("total_deleted");
            if (sTotal != null) {
              try
              {
                int total = Integer.parseInt(sTotal);
                Log.v("GCMBaseIntentService", "Received deleted messages notification: " + total);
                
                onDeletedMessages(context, total);
              }
              catch (NumberFormatException e)
              {
                Log.e("GCMBaseIntentService", "GCM returned invalid number of deleted messages: " + sTotal);
              }
            }
          }
          else
          {
            Log.e("GCMBaseIntentService", "Received unknown special message: " + messageType);
          }
        }
        else {
          onMessage(context, intent);
        }
      }
      else if (action.equals("com.google.android.gcm.intent.RETRY"))
      {
        String token = intent.getStringExtra("token");
        if (!TOKEN.equals(token))
        {
          Log.e("GCMBaseIntentService", "Received invalid token: " + token); return;
        }
        if (GCMRegistrar.isRegistered(context))
        {
          GCMRegistrar.internalUnregister(context);
        }
        else
        {
          String[] senderIds = getSenderIds(context);
          GCMRegistrar.internalRegister(context, senderIds);
        }
      }
    }
    finally
    {
      synchronized (LOCK)
      {
        if (sWakeLock != null)
        {
          Log.v("GCMBaseIntentService", "Releasing wakelock");
          sWakeLock.release();
        }
        else
        {
          Log.e("GCMBaseIntentService", "Wakelock reference is null");
        }
      }
    }
  }
  
  static void runIntentInService(Context context, Intent intent, String className)
  {
    synchronized (LOCK)
    {
      if (sWakeLock == null)
      {
        PowerManager pm = (PowerManager)context.getSystemService("power");
        
        sWakeLock = pm.newWakeLock(1, "GCM_LIB");
      }
    }
    Log.v("GCMBaseIntentService", "Acquiring wakelock");
    sWakeLock.acquire();
    intent.setClassName(context, className);
    context.startService(intent);
  }
  
  private void handleRegistration(Context context, Intent intent)
  {
    String registrationId = intent.getStringExtra("registration_id");
    String error = intent.getStringExtra("error");
    String unregistered = intent.getStringExtra("unregistered");
    Log.d("GCMBaseIntentService", "handleRegistration: registrationId = " + registrationId + ", error = " + error + ", unregistered = " + unregistered);
    if (registrationId != null)
    {
      GCMRegistrar.resetBackoff(context);
      GCMRegistrar.setRegistrationId(context, registrationId);
      onRegistered(context, registrationId);
      return;
    }
    if (unregistered != null)
    {
      GCMRegistrar.resetBackoff(context);
      String oldRegistrationId = GCMRegistrar.clearRegistrationId(context);
      
      onUnregistered(context, oldRegistrationId);
      return;
    }
    Log.d("GCMBaseIntentService", "Registration error: " + error);
    if ("SERVICE_NOT_AVAILABLE".equals(error))
    {
      boolean retry = onRecoverableError(context, error);
      if (retry)
      {
        int backoffTimeMs = GCMRegistrar.getBackoff(context);
        int nextAttempt = backoffTimeMs / 2 + sRandom.nextInt(backoffTimeMs);
        
        Log.d("GCMBaseIntentService", "Scheduling registration retry, backoff = " + nextAttempt + " (" + backoffTimeMs + ")");
        
        Intent retryIntent = new Intent("com.google.android.gcm.intent.RETRY");
        
        retryIntent.putExtra("token", TOKEN);
        PendingIntent retryPendingIntent = PendingIntent.getBroadcast(context, 0, retryIntent, 0);
        
        AlarmManager am = (AlarmManager)context.getSystemService("alarm");
        
        am.set(3, SystemClock.elapsedRealtime() + nextAttempt, retryPendingIntent);
        if (backoffTimeMs < MAX_BACKOFF_MS) {
          GCMRegistrar.setBackoff(context, backoffTimeMs * 2);
        }
      }
      else
      {
        Log.d("GCMBaseIntentService", "Not retrying failed operation");
      }
    }
    else
    {
      onError(context, error);
    }
  }
}
