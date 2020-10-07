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
import com.evilthreads.pickpocket.podos.PocketData

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
data class Audio(
                 @NonNull
                 val path: String,
                 @NonNull
                 val file: ByteArray,
                 val name: String,
                 val title: String?,
                 val description: String?,
                 val mime: String?,
                 val size: String?,
                 val duration: String?,
                 val originId: String?,
                 val artist: String?,
                 val album: String?,
                 val composer: String?,
                 val track: Int?,
                 val year: Int?,
                 val dateAdded: Long?,
                 val dateModified: Long?,
                 val isMusic: Int?,
                 val isAudioBook: Int?,
                 val isPodcast: Int?,
                 val isRingtone: Int?,
                 val isAlarm: Int?
): PocketData