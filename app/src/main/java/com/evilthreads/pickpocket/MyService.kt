package com.evilthreads.pickpocket

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.candroid.bootlaces.LifecycleBootService
import com.evilthreads.pickpocket.podos.CalendarEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect

@ExperimentalCoroutinesApi
class MyService: LifecycleBootService(){
    init {
        lifecycleScope.launchWhenCreated {
            calendarFlow().collect { calendarEvent ->
                Log.d("PICKPOCKET", "${calendarEvent.title} - ${calendarEvent.startDate}")
            }

            val events = calendarAsync(this@MyService)
            Log.d("ASYNC PICKPOCKET","${events.await().size}")

            calendarProducer(this@MyService).consumeEach { event -> Log.d("PICKPOCKET PRODUCER", event.toString()) }

            settingsProducer(this@MyService).consumeEach { Log.d("PICKPOCKET", it.toString()) }
        }
    }
}