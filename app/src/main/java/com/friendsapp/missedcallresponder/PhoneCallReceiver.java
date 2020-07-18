package com.friendsapp.missedcallresponder;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import java.util.Random;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class PhoneCallReceiver extends BroadcastReceiver {


    private NotificationManagerCompat notificationManager;

    @Override
    public void onReceive(final Context context, Intent intent) {

        try {

            Bundle extras = intent.getExtras();
            final String incomingNumber = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            final SharedPreferences sharedPref = context.getSharedPreferences("com.friendsapp.missedcallresponder.sp", Context.MODE_PRIVATE);

            notificationManager = NotificationManagerCompat.from(context);

            Runnable runnable = new Runnable() {
                public void run() {
                    boolean bool = sharedPref.getBoolean("servicestate", false);
                    if (bool) {
                        sendsms(context, incomingNumber, sharedPref);
                    } else
                        sendNotification(context, "Turn On Service", "Are you busy not to attend calls, turn on our service and reply automatically to caller");
                }
            };

            Handler handler = new android.os.Handler();
            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                handler.postDelayed(runnable, 1000);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void sendsms(Context context, String incomingNumber, SharedPreferences sharedPreferences) {
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(incomingNumber, null, getMsg(sharedPreferences), null, null);
            sendNotification(context, "SMS Sent Success!", "Your busy message was successfully sent to "+incomingNumber);
        } catch (Exception e) {
            sendNotification(context, "SMS Sent Failed!", "SMS sent fail, may because of deny permissions or no SMS balance, "+e.getMessage());
        }
    }

    private String getMsg(SharedPreferences sharedPref)
    {
        return sharedPref.getString("setsmsstring", "empty");
    }

    public void sendNotification(Context context, String title, String msg){

        Notification notification = new NotificationCompat.Builder(context, App.CHANNEL_1_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .build();
        notificationManager.notify(new Random().nextInt(), notification);

    }


}
