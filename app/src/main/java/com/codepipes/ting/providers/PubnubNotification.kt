package com.codepipes.ting.providers

import android.content.Context
import com.pubnub.api.models.consumer.history.PNHistoryItemResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNHistoryResult
import com.pubnub.api.callbacks.PNCallback
import clojure.lang.RT.count
import com.pubnub.api.models.consumer.PNPublishResult
import android.R.id.message
import com.google.gson.JsonObject
import android.os.AsyncTask.execute
import com.codepipes.ting.utils.UtilData
import com.pubnub.api.models.consumer.pubsub.objects.PNMembershipResult
import com.pubnub.api.PubNub
import com.pubnub.api.models.consumer.pubsub.objects.PNSpaceResult
import com.pubnub.api.models.consumer.pubsub.objects.PNUserResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.PNConfiguration
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult


class PubnubNotification (val context: Context) {

    public fun initialize() {

        val userAuthentication = UserAuthentication(context)
        val session = userAuthentication.get()

        val pubnubConfig = PNConfiguration()
        pubnubConfig.subscribeKey = UtilData.PUBNUB_SUBSCRIBE_KEY
        pubnubConfig.publishKey = UtilData.PUBNUB_PUBLISH_KEY
        pubnubConfig.isSecure = true

        val pubnub = PubNub(pubnubConfig)
        pubnub.subscribe().channels(listOf(session?.channel)).withPresence().execute()

        pubnub.addListener(object : SubscribeCallback() {

            override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

            override fun status(pubnub: PubNub, pnStatus: PNStatus) {}

            override fun user(pubnub: PubNub, pnUserResult: PNUserResult) {}

            override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {}

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {}

            override fun space(pubnub: PubNub, pnSpaceResult: PNSpaceResult) {}
        })
    }
}