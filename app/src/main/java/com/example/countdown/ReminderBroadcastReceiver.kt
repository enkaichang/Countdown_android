package com.example.countdown

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.concurrent.TimeUnit

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CountdownManager.rescheduleAllReminders(context)
            return
        }

        val eventTitle = intent.getStringExtra("event_title")
        val targetTime = intent.getLongExtra("target_time", 0)

        if (eventTitle != null && targetTime > 0) {
            val diff = targetTime - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            if (days >= 0) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val notification = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(eventTitle)
                    .setContentText("There are $days days left until your event.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

                notificationManager.notify(targetTime.toInt(), notification)
            }
        }
    }
}
