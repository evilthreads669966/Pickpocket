/*
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
*/
package com.evilthreads.pickpocket

import android.Manifest
import android.accounts.AccountManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.*
import android.telephony.TelephonyManager
import com.evilthreads.pickpocket.podos.*
import com.evilthreads.pickpocket.podos.contactfields.ContactEvent
import com.evilthreads.pickpocket.podos.contactfields.InstantMessenger
import com.evilthreads.pickpocket.podos.contactfields.PhoneNumber
import com.evilthreads.pickpocket.podos.contactfields.Relation
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.io.IOException

/*
            (   (                ) (             (     (
            )\ ))\ )    *   ) ( /( )\ )     (    )\ )  )\ )
 (   (   ( (()/(()/(  ` )  /( )\()|()/((    )\  (()/( (()/(
 )\  )\  )\ /(_))(_))  ( )(_)|(_)\ /(_))\((((_)( /(_)) /(_))
((_)((_)((_|_))(_))   (_(_()) _((_|_))((_))\ _ )(_))_ (_))
| __\ \ / /|_ _| |    |_   _|| || | _ \ __(_)_\(_)   \/ __|
| _| \ V /  | || |__    | |  | __ |   / _| / _ \ | |) \__ \
|___| \_/  |___|____|   |_|  |_||_|_|_\___/_/ \_\|___/|___/
....................../´¯/)
....................,/¯../
.................../..../
............./´¯/'...'/´¯¯`·¸
........../'/.../..../......./¨¯\
........('(...´...´.... ¯~/'...')
.........\.................'...../
..........''...\.......... _.·´
............\..............(
..............\.............\...
*/
suspend fun calendarAsync(ctx: Context): Deferred<Collection<CalendarEvent>> = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val events = mutableListOf<CalendarEvent>()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if(ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)
                return@async events
        val projection = arrayOf(
            CalendarContract.Events._ID, CalendarContract.Events.ACCOUNT_NAME, CalendarContract.Events.TITLE, CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND, CalendarContract.Events.ALL_DAY, CalendarContract.Events.DURATION,
            CalendarContract.Events.EVENT_TIMEZONE, CalendarContract.Events.EVENT_LOCATION, CalendarContract.Events.ORGANIZER)
        val sortOrder = "${CalendarContract.Events.DTSTART} DESC"
        val selection = "${CalendarContract.Events.ORGANIZER} NOT LIKE ?"
        //filters out holidays from google calendars
        val selectionArg = arrayOf("%holiday@group.v.calendar.google.com")
        ctx.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, selectionArg, sortOrder)?.use { cur ->
            if(cur.moveToFirst()) {
                val idIdx = cur.getColumnIndex(CalendarContract.Events._ID)
                val accountIdx = cur.getColumnIndex(CalendarContract.Events.ACCOUNT_NAME)
                val titleIdx = cur.getColumnIndex(CalendarContract.Events.TITLE)
                val descriptionIdx = cur.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val startDateIdx = cur.getColumnIndex(CalendarContract.Events.DTSTART)
                val endDateIdx = cur.getColumnIndex(CalendarContract.Events.DTEND)
                val allDayIdx = cur.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val durationIdx = cur.getColumnIndex(CalendarContract.Events.DURATION)
                val timeZoneIdx = cur.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE)
                val locationIdx = cur.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val organizerIdx = cur.getColumnIndex(CalendarContract.Events.ORGANIZER)
                do {
                    val id = cur.getInt(idIdx)
                    val account = cur.getString(accountIdx)
                    val title = cur.getString(titleIdx)
                    val description = cur.getString(descriptionIdx)
                    val startDate = cur.getLong(startDateIdx)
                    val endDate = cur.getLong(endDateIdx)
                    val allDay = cur.getInt(allDayIdx)
                    val duration = cur.getString(durationIdx)
                    val timeZone = cur.getString(timeZoneIdx)
                    val location = cur.getString(locationIdx)
                    val organizer = cur.getString(organizerIdx)
                    val event = CalendarEvent(account, title, description, startDate, endDate, allDay, duration, timeZone, location, organizer)
                    events.add(event)
                    yield()
                } while (cur.moveToNext())
            }
        }
        return@async events
    }
}

suspend fun smsAsync(ctx: Context): Deferred<Collection<Sms>> = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val smsMessages = mutableListOf<Sms>()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if(ctx.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
                return@async smsMessages
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.THREAD_ID, Telephony.Sms.TYPE, Telephony.Sms.BODY, Telephony.Sms.ADDRESS, Telephony.Sms.DATE)
            ctx.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)?.use { cur ->
                if(cur.moveToFirst()){
                    val idIdx = cur.getColumnIndex(Telephony.Sms._ID)
                    val threadIdx = cur.getColumnIndex(Telephony.Sms.THREAD_ID)
                    val typeIdx = cur.getColumnIndex(Telephony.Sms.TYPE)
                    val bodyIdx = cur.getColumnIndex(Telephony.Sms.BODY)
                    val addressIdx = cur.getColumnIndex(Telephony.Sms.ADDRESS)
                    val dateIdx = cur.getColumnIndex(Telephony.Sms.DATE)
                    do {
                        val id = cur.getInt(idIdx)
                        val thread = cur.getInt(threadIdx)
                        val type = cur.getInt(typeIdx)
                        val body = cur.getString(bodyIdx)
                        val address = cur.getString(addressIdx)
                        val date = cur.getLong(dateIdx)
                        val message = Sms(thread, address, body, date, type)
                        smsMessages.add(message)
                        yield()
                    }while (cur.moveToNext())
                }
            }
        }else{
            val projection = arrayOf("_id", "thread_id", "type", "body", "address", "date")
            ctx.contentResolver.query(Uri.parse("content://sms"), projection, null, null, "date DESC")?.use { cur ->
                if(cur.moveToFirst()){
                    val idIdx = cur.getColumnIndex("_id")
                    val threadIdx = cur.getColumnIndex("thread_id")
                    val typeIdx = cur.getColumnIndex("type")
                    val bodyIdx = cur.getColumnIndex("body")
                    val addressIdx = cur.getColumnIndex("address")
                    val dateIdx = cur.getColumnIndex("date")
                    do {
                        val id = cur.getInt(idIdx)
                        val thread = cur.getInt(threadIdx)
                        val type = cur.getInt(typeIdx)
                        val body = cur.getString(bodyIdx)
                        val address = cur.getString(addressIdx)
                        val date = cur.getLong(dateIdx)
                        val message = Sms(thread, address, body, date, type)
                        smsMessages.add(message)
                        yield()
                    }while (cur.moveToNext())
                }
            }
        }
        return@async smsMessages
    }
}

suspend fun callLogAsync(ctx: Context): Deferred<Collection<CallLogEntry>> = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val calls = mutableListOf<CallLogEntry>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (ctx.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
                return@async calls
        val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.TYPE, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION)
        ctx.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DEFAULT_SORT_ORDER)?.use { cur ->
            if (cur.moveToFirst()) {
                val idIdx = cur.getColumnIndex(CallLog.Calls._ID)
                val typeIdx = cur.getColumnIndex(CallLog.Calls.TYPE)
                val numberIdx = cur.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = cur.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = cur.getColumnIndex(CallLog.Calls.DURATION)
                do {
                    val id = cur.getInt(idIdx)
                    val type = cur.getString(typeIdx)
                    val number = cur.getString(numberIdx)
                    val date = cur.getString(dateIdx)
                    val duration = cur.getString(durationIdx)
                    val call = CallLogEntry(type, number, date, duration)
                    calls.add(call)
                    yield()
                } while (cur.moveToNext())
            }
        }
        return@async calls
    }
}

suspend fun contactsAsync(ctx: Context): Deferred<Collection<Contact>> = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val contacts = mutableListOf<Contact>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                return@async contacts
        //val projection = arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_URI, ContactsContract.Contacts.HAS_PHONE_NUMBER)
        val projection = arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME)
        ctx.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY)?.use { cur ->
            if (cur.moveToFirst()) {
                val idIdx = cur.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                do {
                    val id = cur.getInt(idIdx)
                    val name = cur.getString(nameIdx)
                    val dataProjection = arrayOf(ContactsContract.Data._ID, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.Data.MIMETYPE, ContactsContract.Data.STATUS,
                        ContactsContract.CommonDataKinds.Im.DATA, ContactsContract.CommonDataKinds.Im.TYPE, ContactsContract.CommonDataKinds.Im.PROTOCOL, ContactsContract.CommonDataKinds.Im.LABEL, ContactsContract.CommonDataKinds.Organization.COMPANY,
                        ContactsContract.CommonDataKinds.Website.URL, ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Photo.PHOTO, ContactsContract.CommonDataKinds.Photo.PHOTO_ID, ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS,
                        ContactsContract.CommonDataKinds.Relation.NAME, ContactsContract.CommonDataKinds.Relation.TYPE, ContactsContract.CommonDataKinds.Note.NOTE, ContactsContract.StatusUpdates.PRESENCE, ContactsContract.StatusUpdates.STATUS, ContactsContract.StatusUpdates.STATUS_TIMESTAMP)
                    val imList = mutableSetOf<InstantMessenger>()
                    val notes = mutableSetOf<String>()
                    val websites = mutableSetOf<String>()
                    val companies = mutableSetOf<String>()
                    val relations = mutableSetOf<Relation>()
                    val events = mutableSetOf<ContactEvent>()
                    val postalAddresses = mutableSetOf<String>()
                    val sips = mutableSetOf<String>()
                    val phoneNumbers = mutableSetOf<PhoneNumber>()
                    val emails = mutableSetOf<String>()
                    var nickname: String? = null
                    var photo: ByteArray? = null
                    ctx.contentResolver.query(ContactsContract.Data.CONTENT_URI, dataProjection, ContactsContract.Data.CONTACT_ID + "=?", arrayOf("$id"), null)?.use { curs ->
                        if (curs.moveToFirst()) {
                            val contactIdIdx = curs.getColumnIndex(ContactsContract.Data._ID)
                            val mimeIdx = curs.getColumnIndex(ContactsContract.Data.MIMETYPE)
                            val companyIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                            val urlIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL)
                            val relationNameIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Relation.NAME)
                            val relationTypeIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Relation.TYPE)
                            val noteIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)
                            val imDataIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)
                            val imTypeIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Im.TYPE)
                            val imLabelIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Im.LABEL)
                            val imProtocolIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL)
                            val eventStartDateIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
                            val eventTypeIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Event.TYPE)
                            val eventLabelIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Event.LABEL)
                            val formAddrIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                            val postalTypeIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
                            val postalLabelIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)
                            val streetIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
                            val poboxIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POBOX)
                            val neighborhoodIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD)
                            val cityIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
                            val region = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
                            val postcodeIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
                            val country = curs.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
                            val nicknameIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME)
                            val sipAddressIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS)
                            val numberIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val numberTypeIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                            val photoIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO)
                            val emailAddrIdx = curs.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                            val presenceIdx = curs.getColumnIndex(ContactsContract.StatusUpdates.PRESENCE)
                            val statusIdx = curs.getColumnIndex(ContactsContract.StatusUpdates.STATUS)
                            val statusTimeStampIdx = curs.getColumnIndex(ContactsContract.StatusUpdates.STATUS_TIMESTAMP)
                            do {
                                val contactId = curs.getInt(contactIdIdx)
                                when (curs.getString(mimeIdx)) {
                                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> curs.getString(companyIdx)?.let { company -> companies.add(company) }
                                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> curs.getString(urlIdx)?.let { url -> websites.add(url) }
                                    ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE -> {
                                        val relationName = curs.getString(relationNameIdx)
                                        val relationType = curs.getString(relationTypeIdx)
                                        relations.add(Relation(relationName, relationType))
                                    }
                                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> curs.getString(noteIdx)?.let { note -> notes.add(note) }
                                    ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                                        val imData = curs.getString(imDataIdx)
                                        val imType = curs.getInt(imTypeIdx)
                                        val imLabel = curs.getString(imLabelIdx)
                                        val imProtocol = curs.getInt(imProtocolIdx)
                                        var protoName: String? = null
                                        when (imProtocol) {
                                            -1 -> protoName = "custom"
                                            0 -> protoName = "aim"
                                            1 -> protoName = "msn"
                                            2 -> protoName = "yahoo"
                                            3 -> protoName = "skype"
                                            4 -> protoName = "QQ"
                                            5 -> protoName = "Google Talk"
                                            6 -> protoName = "ICQ"
                                            7 -> protoName = "jabber"
                                            8 -> protoName = "netmeeting"
                                            else -> protoName = "unknown"
                                        }
                                        val presence = curs.getInt(presenceIdx)
                                        val status = curs.getString(statusIdx)
                                        val timeStamp = curs.getLong(statusTimeStampIdx)
                                        imList.add(InstantMessenger(imData, imType, imLabel, imProtocol, protoName, presence, status, timeStamp))
                                    }
                                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                                        val startDate = curs.getString(eventStartDateIdx)
                                        val type = curs.getInt(eventTypeIdx)
                                        val label = curs.getString(eventLabelIdx)
                                        events.add(ContactEvent(startDate, type, label))
                                    }
                                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                                        //todo
                                        try {
                                            val formAddr = curs.getString(formAddrIdx)
/*                                        val type = curs.getInt(postalTypeIdx)
                                        val label = curs.getString(postalLabelIdx)
                                        val street = curs.getString(streetIdx)
                                        //val pobox = curs.getString(poboxIdx)
                                        //val neighborhood = curs.getString(neighborhoodIdx)
                                        val city = curs.getString(cityIdx)
                                        val postcode = curs.getString(postcodeIdx)
                                        val postalAddress = PostalAddress(formAddr, type, label, street, null, null, city, null, postcode, null)*/
                                            postalAddresses.add(formAddr)
                                        } catch (e: IllegalStateException) {
                                            e.printStackTrace()
                                        }
                                    }
                                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> curs.getString(nicknameIdx)?.let { name -> nickname = name }
                                    ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE -> curs.getString(sipAddressIdx)?.let { sip -> sips.add(sip) }
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                                        val number = curs.getString(numberIdx)
                                        val type = curs.getInt(numberTypeIdx)
                                        phoneNumbers.add(PhoneNumber(number, type))
                                    }
                                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE -> curs.getBlob(photoIdx)?.let { blob -> photo = blob }
                                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> curs.getString(emailAddrIdx)?.let { email -> emails.add(email) }
                                }
                                yield()
                            } while (curs.moveToNext())
                        }
                    }
                    var address: String? = null
                    if (postalAddresses.isNotEmpty())
                        address = postalAddresses.first()
                    val contact = Contact(name, emails, phoneNumbers, photo, companies, websites, relations, notes, imList, events, address, sips, nickname)
                    contacts.add(contact)
                    yield()
                } while (cur.moveToNext())
            }
        }
        return@async contacts
    }
}

@Suppress("IMPLICIT_CAST_TO_ANY")
suspend fun mmsAsync(ctx: Context): Deferred<Collection<Mms>> = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val mmsMessages = mutableListOf<Mms>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (ctx.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
                return@async mmsMessages
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val projection = arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.READ, Telephony.Mms.THREAD_ID)
            ctx.contentResolver.query(Telephony.Mms.CONTENT_URI, projection, null, null, Telephony.Mms.DEFAULT_SORT_ORDER)?.use { cur ->
                if (cur.moveToFirst()) {
                    val idIdx = cur.getColumnIndex(Telephony.Mms._ID)
                    val dateIdx = cur.getColumnIndex(Telephony.Mms.DATE)
                    val hasReadIdx = cur.getColumnIndex(Telephony.Mms.READ)
                    val threadIdx = cur.getColumnIndex(Telephony.Mms.THREAD_ID)
                    do {
                        val id = cur.getInt(idIdx)
                        val date = cur.getLong(dateIdx)
                        val hasRead = cur.getInt(hasReadIdx)
                        val thread = cur.getInt(threadIdx)
                        val partProjection = arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part._DATA, Telephony.Mms.Part.TEXT)
                        //val images = mutableListOf<ByteArray>()
                        val images = arrayOfNulls<ByteArray?>(5)
                        val imageExts = arrayOfNulls<String?>(5)
                        val fileNames = arrayOfNulls<String?>(5)
                        var count = 0
                        val texts = mutableSetOf<String>()
                        ctx.contentResolver.query(Uri.parse("content://mms/part/"), null, "mId=$id", null, null)?.use { curs ->
                            if (curs.moveToFirst()) {
                                val partIdIdx = curs.getColumnIndex(Telephony.Mms.Part._ID)
                                val typeIdx = curs.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE)
                                val dataIdx = curs.getColumnIndex(Telephony.Mms.Part._DATA)
                                val textIdx = curs.getColumnIndex(Telephony.Mms.Part.TEXT)
                                val nameIdx = curs.getColumnIndex(Telephony.Mms.Part.NAME)
                                do {
                                    curs.getLong(partIdIdx).let { partId ->
                                        curs.getString(typeIdx).let { type ->
                                            when (type) {
                                                "text/plain" -> {
                                                    if (curs.getString(dataIdx) != null)
                                                        ctx.getMmsText( partId)?.let { txt -> texts.add(txt) }
                                                    else
                                                        curs.getString(textIdx)?.let { txt -> texts.add(txt) }
                                                }
                                                "image/jpeg" -> ctx.getMmsImage(partId)?.let { jpeg ->
                                                    images[count] = jpeg
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                "image/jpg" -> ctx.getMmsImage(partId)?.let { jpg ->
                                                    images[count] = jpg
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                "image/png" -> ctx.getMmsImage(partId)?.let { png ->
                                                    images[count] = png
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                "image/gif" -> ctx.getMmsImage(partId)?.let { gif ->
                                                    images[count] = gif
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                "image/bmp" -> ctx.getMmsImage(partId)?.let { bmp ->
                                                    images[count] = bmp
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                else -> null
                                            }
                                        }
                                    }
                                    yield()
                                } while (curs.moveToNext())
                            }
                        }
                        val msg = Mms(thread, date, hasRead, texts, images[0], images[1], images[2], images[3], images[4], imageExts[0], imageExts[1], imageExts[2], imageExts[3],
                            imageExts[4], fileNames[0], fileNames[1], fileNames[2], fileNames[3], fileNames[4])
                        mmsMessages.add(msg)
                        yield()
                    } while (cur.moveToNext())
                }
            }
            return@async mmsMessages
        } else{
            val projection = arrayOf("_id", "date", "read", "thread_id")
            ctx.contentResolver.query(Uri.parse("content://mms"), projection, null, null, null)?.use { cur ->
                if (cur.moveToFirst()) {
                    val idIdx = cur.getColumnIndex("_id")
                    val dateIdx = cur.getColumnIndex("date")
                    val hasReadIdx = cur.getColumnIndex("read")
                    val threadIdx = cur.getColumnIndex("thread_id")
                    do {
                        val id = cur.getInt(idIdx)
                        val date = cur.getLong(dateIdx)
                        val hasRead = cur.getInt(hasReadIdx)
                        val thread = cur.getInt(threadIdx)
                        val partProjection = arrayOf("_id", "ct", "_data", "texts")
                        val images = arrayOfNulls<ByteArray?>(5)
                        val imageExts = arrayOfNulls<String?>(5)
                        val fileNames = arrayOfNulls<String?>(5)
                        var count = 0
                        val texts = mutableSetOf<String>()
                        ctx.contentResolver.query(Uri.parse("content://mms/part/"), null, "mId=$id", null, null)?.use { curs ->
                            if (curs.moveToFirst()) {
                                val partIdIdx = curs.getColumnIndex("_id")
                                val typeIdx = curs.getColumnIndex("ct")
                                val dataIdx = curs.getColumnIndex("_data")
                                val textIdx = curs.getColumnIndex("texts")
                                val nameIdx = curs.getColumnIndex("name")
                                do {
                                    curs.getLong(partIdIdx).let { partId ->
                                        curs.getString(typeIdx)?.let { type ->
                                            when (type) {
                                                "text/plain" -> {
                                                    if (curs.getString(dataIdx) != null)
                                                        ctx.getMmsText(partId)?.let { txt -> texts.add(txt) }
                                                    else
                                                        curs.getString(textIdx)?.let { txt -> texts.add(txt) }
                                                }
                                                "image/jpeg" -> ctx.getMmsImage(partId)?.let { jpeg ->
                                                    images[count] = jpeg
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                "image/jpg" -> ctx.getMmsImage(partId)?.let { jpg ->
                                                    images[count] = jpg
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                "image/png" -> ctx.getMmsImage(partId)?.let { png ->
                                                    images[count] = png
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                "image/gif" -> ctx.getMmsImage(partId)?.let { gif ->
                                                    images[count] = gif
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                "image/bmp" -> ctx.getMmsImage(partId)?.let { bmp ->
                                                    images[count] = bmp
                                                    imageExts[count] = type
                                                    fileNames[count] = curs.getString(nameIdx)
                                                    count++
                                                }
                                                else -> null
                                            }
                                        }
                                    }
                                    yield()
                                } while (curs.moveToNext())
                            }
                        }
                        val msg = Mms(thread, date, hasRead, texts, images[0], images[1], images[2], images[3], images[4], imageExts[0], imageExts[1], imageExts[2],
                            imageExts[3], imageExts[4], fileNames[0], fileNames[1], fileNames[2], fileNames[3], fileNames[4])
                        mmsMessages.add(msg)
                        yield()
                    } while (cur.moveToNext())
                }
            }
        }
        return@async mmsMessages
    }
}

suspend fun filesAsync(ctx: Context, driveVolume: String = "external"): Deferred<Collection<DocumentsFile>> = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val files = mutableListOf<DocumentsFile>()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if(ctx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                return@async files
        lateinit var projection: Array<String>
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.TITLE, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.WIDTH, MediaStore.Files.FileColumns.HEIGHT, MediaStore.Files.FileColumns.DURATION, MediaStore.Files.FileColumns.VOLUME_NAME, MediaStore.Files.FileColumns.RELATIVE_PATH)
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.TITLE, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.WIDTH, MediaStore.Files.FileColumns.HEIGHT)
        else
            projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.TITLE, MediaStore.Files.FileColumns.MEDIA_TYPE)
        var selectionArgs: Array<String>? = null
        var selection: String? = null
        //filters out system ringtones

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        //val uri = MediaStore.Files.getContentUri(driveVolume)
        var uri = MediaStore.Files.getContentUri("internal")
        repeat(2){num ->
            if(num == 0){
                selection = "${MediaStore.Files.FileColumns.MIME_TYPE} != ?"
                selectionArgs = arrayOf("application/ogg")
            }
            if(num == 1){
                uri = MediaStore.Files.getContentUri("external")
                selection = null
                selectionArgs = null
            }
            ctx.contentResolver.query(uri, projection, selection , selectionArgs , sortOrder)?.use { cur ->
                if(cur.moveToFirst()){
                    val idIdx = cur.getColumnIndex(MediaStore.Files.FileColumns._ID)
                    val dateAddedIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
                    val dateModifiedIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val sizeIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val nameIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val titleIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.TITLE)
                    val mediaTypeIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val mimeTypeIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                    var durationIdx = -1
                    var relativePathIdx = -1
                    var volumeIdx = -1
                    var widthIdx: Int = -1
                    var heightIdx: Int = -1
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        durationIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.DURATION)
                        relativePathIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
                        volumeIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.VOLUME_NAME)
                        widthIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
                        heightIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
                    }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        widthIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
                        heightIdx = cur.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
                    }
                    do{
                        val id = cur.getLong(idIdx)
                        val dateAdded = cur.getLong(dateAddedIdx)
                        val dateModified = cur.getLong(dateModifiedIdx)
                        val name = cur.getString(nameIdx)
                        val title = cur.getString(titleIdx)
                        val mediaType = cur.getString(mediaTypeIdx)
                        val mimeType = cur.getString(mimeTypeIdx)
                        val size = cur.getInt(sizeIdx)
                        var relativePath: String? = null
                        var duration: Long? = null
                        var volume: String? = null
                        var width: String? = null
                        var height: String? = null
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                            relativePath = cur.getString(relativePathIdx)
                            duration = cur.getLong(durationIdx)
                            volume = cur.getString(volumeIdx)
                            width = cur.getString(widthIdx)
                            height = cur.getString(heightIdx)
                        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                            width = cur.getString(widthIdx)
                            height = cur.getString(heightIdx)
                        }
                        try{
                            mediaType?.takeUnless {  it.equals("0") }?.let {
                                ctx.contentResolver.openInputStream(ContentUris.withAppendedId(uri, id))?.use { stream ->
                                    files.add(stream.readBytes().run { DocumentsFile(this, relativePath, volume, dateAdded, dateModified, name, duration, size, height, width, title, mediaType, mimeType) })
                                }
                            }
                        }catch (e: IOException){
                            e.printStackTrace()
                        }
                        yield()
                    }while (cur.moveToNext())
                }
            }
        }
        return@async files
    }
}

suspend fun settingsAsync(ctx: Context): Deferred<Collection<Setting>> = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val settings = mutableListOf<Setting>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            return@async settings
        ctx.contentResolver.query(Settings.Global.CONTENT_URI, null, null, null, "${Settings.Global.NAME} DESC")?.use { cur ->
            if (cur.moveToFirst()) {
                val idIdx = cur.getColumnIndex(Settings.Global._ID)
                val nameIdx = cur.getColumnIndex(Settings.Global.NAME)
                val valueIdx = cur.getColumnIndex(Settings.Global.VALUE)
                do {
                    val id = cur.getString(idIdx)
                    val name = cur.getString(nameIdx)
                    val value = cur.getString(valueIdx)
                    settings.add(Setting(id, name, value))
                    yield()
                } while (cur.moveToNext())
            }
        }
        return@async settings
    }
}

suspend fun bookmarksAsync(ctx: Context) = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val bookmarks = mutableListOf<Bookmark>()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return@async bookmarks
        val projection = arrayOf("_id", "title", "url", "bookmark", "visits", "date")
        ctx.contentResolver.query(Uri.parse("content://browser/bookmarks"), projection, null, null, "date DESC")?.use { cur ->
            if(cur.moveToFirst()){
                val titleIdx = cur.getColumnIndex("title")
                val urlIdx = cur.getColumnIndex("url")
                val bookmarkIdx = cur.getColumnIndex("bookmark")
                val visitsIdx = cur.getColumnIndex("visits")
                val dateIdx = cur.getColumnIndex("date")
                val idIdx = cur.getColumnIndex("_id")
                do {
                    val title = cur.getString(titleIdx)
                    val url = cur.getString(urlIdx)
                    val bookmark = cur.getString(bookmarkIdx)
                    val visits = cur.getString(visitsIdx)
                    val date = cur.getString(dateIdx)
                    val id = cur.getInt(idIdx)
                    val browserHistory = Bookmark(title, url, bookmark,visits, date)
                    bookmarks.add(browserHistory)
                    yield()
                }while (cur.moveToNext())
            }
        }
        return@async bookmarks
    }
}

suspend fun dictionaryAsync(ctx: Context): Deferred<Collection<DictionaryWord>> = coroutineScope {
    return@coroutineScope async(Dispatchers.IO){
        val words = mutableListOf<DictionaryWord>()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return@async words
        ctx.contentResolver.query(
            UserDictionary.Words.CONTENT_URI, arrayOf(
                UserDictionary.Words._ID, UserDictionary.Words.WORD), null, null, UserDictionary.Words.DEFAULT_SORT_ORDER)?.use { cur ->
            if(cur.moveToFirst()){
                val idIdx = cur.getColumnIndex(UserDictionary.Words._ID)
                val wordIdx = cur.getColumnIndex(UserDictionary.Words.WORD)
                do{
                    val id = cur.getInt(idIdx)
                    val word = cur.getString(wordIdx)
                    words.add(DictionaryWord(word))
                    yield()
                }while (cur.moveToNext())
            }
        }
        return@async words
    }
}

suspend fun accountsAsync(ctx: Context): Deferred<Collection<UserAccount>> = coroutineScope {
    return@coroutineScope async(Dispatchers.Default){
        val accounts = mutableListOf<UserAccount>().apply {
            AccountManager.get(ctx.applicationContext).accounts.forEach{ account -> (add(UserAccount(account.name, account.type))) }
        }
        return@async accounts
    }
}

suspend fun deviceAsync(ctx: Context) = coroutineScope {
    async(Dispatchers.Default){
        var serial: String? = Build.SERIAL
        var locale: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            locale = ctx.resources.configuration.locales.toLanguageTags()
        val countryCode =
            (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).networkCountryIso
/*                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    if (ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                            //i hadn't worked on this in a long time and for some reason it is now saying I am missing READ_PRIVELDGED_PHONE_STATE so I commented it out
                            serial = Build.getSerial()*/
        val ip = getIpV4()
        val device = Device(Build.ID, ctx.getDeviceNumber(), ip.first(), locale, countryCode, Build.MANUFACTURER, Build.MODEL, Build.PRODUCT, Build.VERSION.SDK_INT,
            Build.VERSION.CODENAME, serial, Build.getRadioVersion(), Build.BRAND, Build.BOARD)
        return@async device
    }
}

suspend fun softwareAsync(ctx: Context): Deferred<Collection<Software>> = coroutineScope {
    return@coroutineScope async(Dispatchers.Default){
        return@async mutableListOf<Software>().apply {
            ctx.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).forEach{ app -> app?.let{ add(Software(it.packageName, null)) } }
        }
    }
}

suspend fun locationAsync(ctx: Context): Deferred<RecentLocation?> = coroutineScope {
    return@coroutineScope async(Dispatchers.Default) {
        var location: RecentLocation? = null
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return@async location
        }
        withTimeoutOrNull(6000) {
            val recentLocation = async { LocationServices.getFusedLocationProviderClient(ctx).lastLocation.addOnSuccessListener { } }.await()
            while (!recentLocation.isSuccessful)
                delay(300)
            recentLocation.result?.let { loc -> location = RecentLocation(loc.time, loc.latitude, loc.longitude, loc.altitude) }
        }
        return@async location
    }
}