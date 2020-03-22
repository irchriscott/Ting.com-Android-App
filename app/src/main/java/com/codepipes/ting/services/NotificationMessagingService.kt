package com.codepipes.ting.services

import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.pusher.pushnotifications.fcm.MessagingService

class NotificationMessagingService : MessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val navigate = remoteMessage.data["navigate"]
        val data = remoteMessage.data["data"]
    }
}