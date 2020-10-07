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
package com.evilthreads.pickpocket.podos

import androidx.annotation.NonNull
import com.evilthreads.pickpocket.podos.contactfields.ContactEvent
import com.evilthreads.pickpocket.podos.contactfields.InstantMessenger
import com.evilthreads.pickpocket.podos.contactfields.PhoneNumber
import com.evilthreads.pickpocket.podos.contactfields.Relation

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
data class Contact(@NonNull
                   val name: String?,
                   val email: Set<String>?,
                   val phoneNumbers: Set<PhoneNumber>?,
                   val photo: ByteArray?,
                   val companies: Set<String?>?,
                   val websites: Set<String?>?,
                   val relations: Set<Relation>?,
                   val notes: Set<String?>?,
                   val ims: Set<InstantMessenger>?,
                   val events: Set<ContactEvent>?,
                   //todo
                   val postalAddresses:String?,
                   val sips: Set<String>?,
                   val nickname: String?
): PocketData