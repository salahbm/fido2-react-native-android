package com.test;

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class FidoServiceManager(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName() = "FidoModule"

    private external fun stringFromJNI() : String


    @ReactMethod 
    fun createFidoEvent(name: String, location: String) {
        Log.d("FidoModule", "Event called with $name & $location")
    }
}