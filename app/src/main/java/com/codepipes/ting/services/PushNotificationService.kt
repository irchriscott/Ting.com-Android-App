package com.codepipes.ting.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.pushnotifications.PushNotifications.subscribe
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import android.support.v4.content.ContextCompat.getSystemService
import android.util.Log
import com.codepipes.ting.R
import com.codepipes.ting.providers.UserAuthentication
import com.google.gson.JsonParser
import com.pusher.client.connection.ConnectionEventListener
import android.support.v4.content.ContextCompat.getSystemService
import com.squareup.picasso.Picasso


class PushNotificationService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val userAuthentication = UserAuthentication(this@PushNotificationService)
        val user = userAuthentication.get()

        if (user != null && userAuthentication.isLoggedIn()) {
            val options = PusherOptions()

            options.setCluster("mt1")
            val pusher = Pusher("299875b04b5fe1dc527a", options)

            pusher.connect(object : ConnectionEventListener {
                override fun onConnectionStateChange(change: ConnectionStateChange) {}
                override fun onError(message: String, code: String, e: Exception) {}
            }, ConnectionState.ALL)

            val channel = pusher.subscribe(user.channel)

            channel.bind(user.channel) { event ->
                val data = JsonParser().parse(event.data).asJsonObject
                Log.i("NOTIFICATION DATA", event.data)

                val title = data["title"].asString
                val body = data["body"].asString

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val notificationChannel = NotificationChannel(user.channel, title, importance)
                    notificationChannel.description = body
                    notificationChannel.enableLights(true)
                    notificationChannel.lightColor = Color.BLUE
                    notificationChannel.enableVibration(true)
                    notificationChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                    notificationChannel.setShowBadge(true)
                    notificationManager.createNotificationChannel(notificationChannel)
                }

                val builder = NotificationCompat.Builder(this, user.channel)
                    .setSmallIcon(R.drawable.ic_restaurants)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)

                if(data.has("image")) {

                    builder
                        .setLargeIcon(Picasso.get().load(data["image"].asString).get())
                        .setStyle(NotificationCompat.BigPictureStyle()
                            .bigLargeIcon(null)
                            .bigPicture(Picasso.get().load(data["image"].asString).get())
                            .setBigContentTitle(title)
                            .setSummaryText(body)
                        )
                }

                if(data.has("text")) {
                    builder.setStyle(NotificationCompat.BigTextStyle()
                        .bigText(data["text"].asString)
                    )
                }

                val notificationId = (0..1000000000).random()
                notificationManager.notify(notificationId, builder.build())
            }
        }

        return Service.START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
}
