package com.health.knowledge.sync

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.health.knowledge.util.HealthNotificationHelper

class HealthFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        HealthNotificationHelper.subscribeToAllUsersTopic()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "নতুন স্বাস্থ্য তথ্য"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["content"]
            ?: "একটি নতুন স্বাস্থ্য টিপস প্রকাশিত হয়েছে।"

        val articleId = remoteMessage.data["articleId"]?.toIntOrNull()
            ?: remoteMessage.data["open_article_id"]?.toIntOrNull()
            ?: 0

        HealthNotificationHelper.showNewPostNotification(
            context = applicationContext,
            articleId = articleId,
            title = title,
            content = body
        )
    }
}
