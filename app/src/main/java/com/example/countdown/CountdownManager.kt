package com.example.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

object CountdownManager {
    private const val PREFS_NAME = "countdown_prefs"
    private const val EVENTS_KEY = "events_list"

    fun saveEvents(context: Context, events: List<CountdownEvent>) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(events)
        editor.putString(EVENTS_KEY, json)
        editor.apply()
    }

    fun loadEvents(context: Context): MutableList<CountdownEvent> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(EVENTS_KEY, null)
        val gson = Gson()
        val type = object : TypeToken<MutableList<CountdownEvent>>() {}.type
        
        val events: MutableList<CountdownEvent> = if (json.isNullOrEmpty() || json == "[]") {
            loadDefaultHolidays(context)
        } else {
            gson.fromJson(json, type) ?: loadDefaultHolidays(context)
        }

        // 確保所有從 JSON 載入的事件都有計算好的時間
        events.forEach { it.calculateTime() }
        
        // 如果是第一次載入（或原本是空的），就存檔一次
        if (json.isNullOrEmpty() || json == "[]") {
            saveEvents(context, events)
        }

        return events
    }

    private fun loadDefaultHolidays(context: Context): MutableList<CountdownEvent> {
        return try {
            val inputStream = context.assets.open("holidays.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = String(buffer, Charsets.UTF_8)
            val gson = Gson()
            val type = object : TypeToken<MutableList<CountdownEvent>>() {}.type
            val events: MutableList<CountdownEvent> = gson.fromJson(json, type) ?: mutableListOf()
            
            // 預先計算時間
            events.forEach { it.calculateTime() }
            events
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun scheduleDailyReminder(context: Context, event: CountdownEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("event_title", event.title)
            putExtra("target_time", event.targetTimeInMillis)
            action = "REMIND_EVENT"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 9) // 9 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            // If it's already past 9 AM, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, event: CountdownEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllReminders(context: Context) {
        val events = loadEvents(context)
        for (event in events) {
            scheduleDailyReminder(context, event)
        }
    }
}
