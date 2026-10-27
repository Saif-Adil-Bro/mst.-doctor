package com.health.knowledge.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.health.knowledge.MainActivity

object HealthNotificationHelper {

    const val CHANNEL_NEW_POSTS = "new_posts_channel"
    private const val NOTIFICATION_ID_BASE = 20000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_NEW_POSTS,
                "নতুন স্বাস্থ্যটিপস ও পোস্ট",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "নতুন কোনো স্বাস্থ্যতথ্য অনুমোদিত বা প্রকাশিত হলে নোটিফিকেশন প্রদান করা হয়"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Subscribes device to the global topic so all users receive push notifications.
     */
    fun subscribeToAllUsersTopic() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all_users")
        } catch (_: Exception) {
            // Ignored if offline or Google Play Services missing
        }
    }

    /**
     * Displays a rich push notification for a newly published health article.
     */
    fun showNewPostNotification(
        context: Context,
        articleId: Int,
        title: String,
        content: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_article_id", articleId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            articleId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanPreview = content.take(180).replace("\n", " ").trim()
        val notifId = if (articleId > 0) NOTIFICATION_ID_BASE + (articleId % 10000) else NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt()

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_POSTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌿 নতুন স্বাস্থ্যটিপস: $title")
            .setContentText(cleanPreview)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🌿 নতুন স্বাস্থ্যটিপস: $title")
                    .bigText("$cleanPreview...\n\n👉 বিস্তারিত সমাধান জানতে এখানে ট্যাপ করুন।")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(notifId, notification)
    }
}
