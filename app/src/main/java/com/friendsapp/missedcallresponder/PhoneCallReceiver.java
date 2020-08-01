package com.friendsapp.missedcallresponder;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Random;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class PhoneCallReceiver extends BroadcastReceiver {


    private NotificationManagerCompat notificationManager;
    private boolean hook = false;
    private String incomingNumber;
    private SharedPreferences sharedPref;
    private Context contextm;

    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.i("check", "call2");
        try {

            Bundle extras = intent.getExtras();
            contextm = context;
            incomingNumber = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            sharedPref = context.getSharedPreferences("com.friendsapp.missedcallresponder.sp", Context.MODE_PRIVATE);

            notificationManager = NotificationManagerCompat.from(context);


//            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
//               // handler.postDelayed(runnable, 1000);
//                Toast.makeText(context,"idle"+String.valueOf(hook),Toast.LENGTH_LONG).show();
//            }
            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                PhoneStateChangeListener pscl = new PhoneStateChangeListener();
                TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                tm.listen(pscl, PhoneStateListener.LISTEN_CALL_STATE);

            }
//            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
//               handler.removeCallbacks(runnable);
//            }



        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
    Runnable runnable = new Runnable() {
        public void run() {
            boolean bool = sharedPref.getBoolean("servicestate", false);
            if (bool) {
                sendsms(contextm, incomingNumber, sharedPref);
            } else
                sendNotification(contextm, "Turn On Service", "Are you busy not to attend calls, turn on our service and reply automatically to caller");
        }
    };

    Handler handler = new android.os.Handler();

    private void sendsms(Context context, String incomingNumber, SharedPreferences sharedPreferences) {
        try {
           // SmsManager sms = SmsManager.getDefault();
           // sms.sendTextMessage(incomingNumber, null, getMsg(sharedPreferences) , null, null);
            sendTextSMS(incomingNumber, getMsg(sharedPreferences), context);
           // sendNotification(context, "SMS Sent Success!", "Your busy message was successfully sent to "+incomingNumber);
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

    private void sendTextSMS(final String phoneNumber, String message, final Context context) {

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, new Intent(DELIVERED), 0);

        BroadcastReceiver sendSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        //sendNotification(context, "SMS Sent Success!", "Your busy message was successfully sent to "+phoneNumber);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        sendNotification(context, "SMS Sent Failed!", "Generic failure");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        sendNotification(context, "SMS Sent Failed!", "No service");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        sendNotification(context, "SMS Sent Failed!", "Null PDU");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        sendNotification(context, "SMS Sent Failed!", "Radio Off");
                        break;
                }
            }
        };

        BroadcastReceiver deliverSMS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        sendNotification(context, "SMS Sent Success!", "Your busy message was successfully sent to "+phoneNumber);
                        break;
                    case Activity.RESULT_CANCELED:
                        sendNotification(context, "SMS not delivered", "Problem at receiver side to deliver message");
                        break;
                }
            }
        };
        context.getApplicationContext().registerReceiver(sendSMS, new IntentFilter(SENT));

        context.getApplicationContext().registerReceiver(deliverSMS, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    class PhoneStateChangeListener extends PhoneStateListener {
        private  boolean wasRinging = false;
        private  boolean wasReceived = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.i("check", "call1");

            String LOG_TAG = "PhoneListener";
            switch(state){
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(LOG_TAG, "RINGING");
                    wasRinging = true;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i(LOG_TAG, "OFFHOOK");
                    wasReceived = true;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.i(LOG_TAG, "IDLE "+wasRinging+" "+wasReceived);
                    if(wasRinging && !wasReceived)
                    {
                        wasReceived = false;
                        wasRinging = false;
                        handler.postDelayed(runnable, 1000);
                    }
                    break;
            }

        }
    }


}