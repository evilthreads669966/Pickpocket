[![Release](https://jitpack.io/v/evilthreads669966/pickpocket.svg)](https://jitpack.io/#evilthreads669966/pickpocket)&nbsp;&nbsp;[![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=plastic)](https://android-arsenal.com/api?level=14)&nbsp;&nbsp;[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://kotlin.link)
# Pickpocket
### An Android library for content provider queries with reactive streams and coroutines. 
- Calendar
- Contacts
- SMS
- MMS
- Files/Media
- Call Log
- Bookmarks
- Browser History
- Settings
- Device Info
- Installed Apps
- GPS Location
- Accounts
- Dictionary
### User Instructions
1. Add the maven repository to your project's build.gradle file
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
2. Add the dependency to your app's build.gradle file
```gradle
dependencies {
    implementation 'com.github.evilthreads669966:pickpocket:0.2.1'
}
```
3. Use a pickpocket coroutine builder function to retrieve a collection of the data types you want 
```kotlin
//flow
calendarFlow().collect { calendarEvent ->
    Log.d("PICKPOCKET", "${calendarEvent.title} - ${calendarEvent.startDate}")
}

//async
//takes android context as argument
val events = calendarAsync(this)
Log.d("ASYNC PICKPOCKET","${events.await().size}")

//producer channel
//takes android context as argument
calendarProducer(this).consumeEach { event -> Log.d("PICKPOCKET PRODUCER", event.toString()) }

//launch
calendarLaunch(this@MyService).forEach { event -> Log.d("PICKPOCKET LAUNCH", event.toString()) }

```
## License
```
Copyright 2019 Chris Basinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
