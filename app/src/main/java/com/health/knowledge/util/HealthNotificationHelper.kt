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
    const val CHANNEL_DAILY_TIPS = "daily_health_tips_channel"
    private const val NOTIFICATION_ID_BASE = 20000
    const val NOTIFICATION_ID_DAILY_TIP = 10001

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel for New User/Admin Approved Posts
            val channelNewPosts = NotificationChannel(
                CHANNEL_NEW_POSTS,
                "নতুন স্বাস্থ্যটিপস ও পোস্ট",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "নতুন কোনো স্বাস্থ্যতথ্য অনুমোদিত বা প্রকাশিত হলে নোটিফিকেশন প্রদান করা হয়"
                enableVibration(true)
            }
            manager.createNotificationChannel(channelNewPosts)

            // Channel for Daily Scheduled Health Tip Reminders
            val channelDailyTips = NotificationChannel(
                CHANNEL_DAILY_TIPS,
                "দৈনিক স্বাস্থ্য টিপস রিমাইন্ডার",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "প্রতিদিনের কাস্টমাইজড টাইমে নির্বাচিত স্বাস্থ্য টিপস নোটিফিকেশন"
                enableVibration(true)
            }
            manager.createNotificationChannel(channelDailyTips)
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
     * Displays a notification for a newly published or admin-approved health article.
     * Shows title and short preview; tapping opens the article detail screen.
     */
    fun showNewPostNotification(
        context: Context,
        articleId: Int,
        title: String,
        content: String,
        isApprovedMine: Boolean = false
    ): Boolean {
        if (!hasNotificationPermission(context)) return false

        createChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_article_id", articleId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            articleId.coerceAtLeast(1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanPreview = content
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .let { if (it.length > 90) it.take(90) + "..." else it }
            .ifBlank { "বিস্তারিত পড়তে ট্যাপ করুন" }

        val headerTitle = if (isApprovedMine) {
            "✅ অনুমোদিত: $title"
        } else {
            "🌿 $title"
        }

        val notifId = if (articleId > 0) {
            NOTIFICATION_ID_BASE + (articleId % 10000)
        } else {
            NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt()
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_POSTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(headerTitle)
            .setContentText(cleanPreview)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(notifId, notification)
        return true
    }

    /**
     * Displays daily health tip push notification.
     * Shows the article title and a short preview; tapping opens the article detail page.
     */
    fun showDailyTipNotification(
        context: Context,
        articleId: Int,
        title: String,
        content: String = ""
    ): Boolean {
        if (!hasNotificationPermission(context)) return false

        createChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_article_id", articleId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            articleId.coerceAtLeast(1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortPreview = content
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .let { if (it.length > 90) it.take(90) + "..." else it }
            .ifBlank { "আজকের স্বাস্থ্য টিপস বিস্তারিত পড়তে ট্যাপ করুন" }

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_TIPS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌿 $title")
            .setContentText(shortPreview)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_DAILY_TIP, notification)
        return true
    }
}
