package com.evilthreads.pickpocket

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.candroid.bootlaces.LifecycleBootService
import com.candroid.bootlaces.bootService
import com.kotlinpermissions.KotlinPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

@InternalCoroutinesApi
class MainActivity : AppCompatActivity() {
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        KotlinPermissions.with(this)
            .permissions(Manifest.permission.READ_CALENDAR).onAccepted {
                val pickpocketPayload = suspend {
                    withContext(Dispatchers.Default){
                        calendarFlow().collect { calendarEvent -> Log.d("PICKPOCKET", "${calendarEvent.title} - ${calendarEvent.startDate}") }
                        val events = calendarAsync(this@MainActivity)
                        Log.d("ASYNC PICKPOCKET","${events.await().size}")
                        calendarProducer(this@MainActivity).consumeEach { event -> Log.d("PICKPOCKET PRODUCER", event.toString()) }
                        settingsProducer(this@MainActivity).consumeEach { Log.d("PICKPOCKET", it.toString()) }
                        calendarLaunch(this@MainActivity).forEach { event -> Log.d("PICKPOCKET LAUNCH", event.toString()) }
                    }
                }
                bootService(this, payload = pickpocketPayload){
                    service = MyService::class
                }
            }.ask()
    }
}

class MyService: LifecycleBootService()