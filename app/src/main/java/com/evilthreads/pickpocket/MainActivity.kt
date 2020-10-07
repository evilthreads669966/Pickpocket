package com.evilthreads.pickpocket

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.candroid.bootlaces.bootService
import com.kotlinpermissions.KotlinPermissions
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        KotlinPermissions.with(this)
            .permissions(Manifest.permission.READ_CALENDAR).onAccepted {
                bootService(this){
                    service = MyService::class
                }
            }.ask()
    }
}