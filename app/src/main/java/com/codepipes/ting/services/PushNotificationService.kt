package com.codepipes.ting.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.codepipes.ting.R
import com.codepipes.ting.providers.UserAuthentication
import com.google.gson.JsonParser
import com.pusher.client.connection.ConnectionEventListener
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.activities.placement.RestaurantScanner
import com.codepipes.ting.activities.base.TingDotCom
import com.codepipes.ting.providers.UserPlacement
import com.squareup.picasso.Picasso
import java.lang.Exception


class PushNotificationService : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val userAuthentication = UserAuthentication(this@PushNotificationService)
        val user = userAuthentication.get()

        if (user != null && userAuthentication.isLoggedIn()) {
            val options = PusherOptions()

            options.setCluster("mt1")
            val pusher = Pusher("299875b04b5fe1dc527a", options)

            try {

                pusher.connect()
                val channel = pusher.subscribe(user.channel)

                channel.bind(user.channel) { event ->
                    val data = JsonParser().parse(event.data).asJsonObject

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
					
					val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                    val builder = NotificationCompat.Builder(this, user.channel)
                        .setSmallIcon(R.drawable.logo_round)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
						.setSound(soundUri)

                    if(data.has("image") && data.get("image").asString != null) {
                        builder
                            .setLargeIcon(Picasso.get().load(data["image"].asString).fit().get())
                            .setStyle(NotificationCompat.BigPictureStyle()
                                .bigLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo_round))
                                .bigPicture(Picasso.get().load(data["image"].asString).fit().get())
                                .setBigContentTitle(title)
                                .setSummaryText(body)
                            )
                    }

                    if(data.has("text")) {
                        if (data["text"].asString.replace("\\s", "") != "") {
                            builder.setStyle(NotificationCompat.BigTextStyle()
                                .bigText(data["text"].asString)
                            )
                        }
                    }

                    if(data.has("navigate")) {
                        when (data["navigate"].asString) {
                            "current_restaurant" -> {
                                val userPlacement = UserPlacement(this@PushNotificationService)
                                val navigateIntent = if (userPlacement.isPlacedIn()) { Intent(this, CurrentRestaurant::class.java) }
                                else { Intent(this, RestaurantScanner::class.java) }

                                navigateIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                if(data.has("data")) { navigateIntent.putExtra("token", data["data"].asString) }

                                val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, navigateIntent, 0)
                                builder.setContentIntent(pendingIntent)
                            }
                            "placement_done" -> {
                                val userPlacement = UserPlacement(this@PushNotificationService)
                                userPlacement.placeOut()
                                val navigateIntent = Intent(this, RestaurantScanner::class.java)
                                navigateIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, navigateIntent, 0)
                                builder.setContentIntent(pendingIntent)
                            }
                            else -> {
                                val navigateIntent = Intent(this, TingDotCom::class.java)
                                navigateIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, navigateIntent, 0)
                                builder.setContentIntent(pendingIntent)
                            }
                        }
                    }
					
					try {
						val notification =
							RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
						val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
						ringtone.play()
					} catch (e: java.lang.Exception) { }

                    val notificationId = (0..1000000000).random()
                    notificationManager.notify(notificationId, builder.build())
                }
            } catch (e: Exception) {}
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
