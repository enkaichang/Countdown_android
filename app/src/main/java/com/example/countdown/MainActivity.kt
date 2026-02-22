package com.example.countdown

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: CountdownAdapter
    private val events = mutableListOf<CountdownEvent>()

    companion object {
        const val CHANNEL_ID = "countdown_channel"
        const val PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        requestNotificationPermission()
        
        events.addAll(CountdownManager.loadEvents(this))

        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)

        adapter = CountdownAdapter(events) { position ->
            val event = events[position]
            CountdownManager.cancelReminder(this, event)
            events.removeAt(position)
            adapter.notifyItemRemoved(position)
            CountdownManager.saveEvents(this, events)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            showAddEventDialog()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Countdown Reminders"
            val descriptionText = "Daily reminders for your countdown events"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun showAddEventDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Event")

        val input = EditText(this)
        input.hint = "Enter event title"
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val title = input.text.toString()
            if (title.isNotEmpty()) {
                showDatePickerDialog(title)
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showDatePickerDialog(title: String) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                val targetTimeInMillis = selectedCalendar.timeInMillis
                addEvent(title, targetTimeInMillis)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun addEvent(title: String, targetTimeInMillis: Long) {
        val newEvent = CountdownEvent(title = title, targetTimeInMillis = targetTimeInMillis)
        events.add(newEvent)
        adapter.notifyItemInserted(events.size - 1)
        CountdownManager.saveEvents(this, events)
        CountdownManager.scheduleDailyReminder(this, newEvent)
    }
}
    