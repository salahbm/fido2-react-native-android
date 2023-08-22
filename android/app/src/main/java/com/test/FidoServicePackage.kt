package com.test;

import android.util.Log
import android.view.View
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ReactShadowNode
import com.facebook.react.uimanager.ViewManager
import kr.trustkey.api.bridge.TrustKey_API_Bridge

class FidoServicePackage () : ReactPackage {
    override fun createNativeModules(
        reactContext: ReactApplicationContext
    ): MutableList<NativeModule> {
        val modules = ArrayList<NativeModule>()
        modules.add(TrustKey_API_Bridge(reactContext))
        modules.add(FidoServiceManager(reactContext))

        //java.lang.String
        // kr.trustkey.api.bridge.TrustKeyApiBridge.TKAuthN_GetDeviceHandle() (tried Java_kr_trustkey_api_bridge_TrustKeyApiBridge_TKAuthN_1GetDeviceHandle and Java_kr_trustkey_api_bridge_TrustKeyApiBridge_TKAuthN_1GetDeviceHandle__)
        // java.lang.String kr.trustkey.api.bridge.TrustKey_API_Bridge.TKAuthN_GetDeviceHandle() (TrustKey_API_Bridge.kt:-2)

        return modules.toMutableList()
    //listOf(FidoServiceManager(reactContext)).toMutableList()
    }

    override fun createViewManagers(
        reactContext: ReactApplicationContext
    ): MutableList<ViewManager<View, ReactShadowNode<*>>> = mutableListOf()
}