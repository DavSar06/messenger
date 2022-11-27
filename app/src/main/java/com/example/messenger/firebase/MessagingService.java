package com.example.messenger.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        try {
            Log.d("FCM", "Token: " + token);
        }catch (Exception e){
            Log.d("FCM","EXCEPTION: "+e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        try{
            Log.d("FCM","Message: " + message.getNotification().getBody());
        }catch(Exception e){
            Log.d("FCM","EXCEPTION: "+e);
        }

    }
}
