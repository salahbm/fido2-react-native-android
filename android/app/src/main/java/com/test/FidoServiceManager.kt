package com.test;

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast
import com.facebook.react.bridge.*
import com.test.fido.EachDeviceRef
import kr.trustkey.api.bridge.TrustKey_API_Bridge
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLDecoder
import java.security.SecureRandom
import javax.net.ssl.*

private val ACTION_USB_PERMISSION = "com.test.USB_PERMISSION"

private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("TKAuthN", "onReceive!!!")
        if (ACTION_USB_PERMISSION == intent.action) {
            synchronized(this) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.apply {
                    }
                } else {
                    Log.d("TKAuthN", "permission denied for device $device")
                }
            }
        }
    }
}

class FidoServiceManager(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val m_TrustKey_API_Bridge = TrustKey_API_Bridge()

    val m_device_custom_info : MutableList<EachDeviceRef> = ArrayList()
    /* 각각의 디바이스의 정보들을 저장합니다 예) UsbInterface, UsbDevice, UsbEndPoint,
       product ID, vendor ID 등등을 Array 로 저장합니다 */

    private lateinit var m_connection: UsbDeviceConnection
    private val m_hid_packet_size : Int  = 64
    private val m_timeout : Int = 2000

    private val TAG = "FidoServiceManager"

    private var connectionPromise: Promise? = null

    override fun getName() = "FidoServiceManager"

    private fun getUsbManager(): UsbManager {
        val rAppContext = reactApplicationContext
        return rAppContext.getSystemService(Context.USB_SERVICE) as UsbManager
    }
    @ReactMethod
    fun initFidoDevice(promise: Promise) {
        Log.d(TAG, "Try to init settings...")

        try {
            Log.d(TAG, "USB Device setting...")
            m_TrustKey_API_Bridge.m_usbManager = getUsbManager()
            m_TrustKey_API_Bridge.m_deviceList = m_TrustKey_API_Bridge.m_usbManager.deviceList

            m_TrustKey_API_Bridge.initSetting()

            val permissionIntent =
                PendingIntent.getBroadcast(reactApplicationContext, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE)

            val filter = IntentFilter(ACTION_USB_PERMISSION)
            reactApplicationContext.registerReceiver(usbReceiver, filter)
            val deviceIterator: Iterator<UsbDevice> = m_TrustKey_API_Bridge.m_deviceList.values.iterator()

            var idx  = 0
            while (deviceIterator.hasNext()) {

                val _device = deviceIterator.next()
                m_TrustKey_API_Bridge.m_usbManager.requestPermission(_device, permissionIntent)
                idx++
            }
        } catch (e : Exception) {
            e.printStackTrace()
            promise.reject("E401", "m_connection has not been initialized")
        }

        Log.d("FidoModule", m_TrustKey_API_Bridge.m_deviceList.toString())
        promise.resolve("USB manager setup!!")
        //initSetting()
    }

    @ReactMethod
    fun preMakeCredentialProcess() {

    }

    @ReactMethod
    fun makeCredentialCTAPLog() {
        Log.d(TAG, "calling makeCredentialCTAPLog...")
        m_TrustKey_API_Bridge.makeCredential_CTAP_Log()
    }

    @ReactMethod
    fun disconnect(promise: Promise) {
        Log.d(TAG, "calling disconnect...")
        /*try {
            if (m_connection == null) {
                val error = "No USB connection established"
                Log.e(TAG, error)
                promise.reject("E400", error)
            } else {
                m_connection.close()
                promise.resolve(null)
            }
        } catch (e : Exception) {
            e.printStackTrace()
            promise.reject("E401", "m_connection has not been initialized")
        }*/
    }

    @ReactMethod
    fun getDeviceHandle(promise: Promise) {
        Log.d(TAG, "calling getDeviceHandle...")
        try {
            val deviceHandle = m_TrustKey_API_Bridge.TKAuthN_GetDeviceHandle()
            promise.resolve(deviceHandle)
        } catch (e: Exception) {
            promise.reject("E_ERROR", e.message)
        }
    }
    @ReactMethod
    fun getMakeCredential(jsonValue: String, promise: Promise) {
        Log.d(TAG, "calling getMakeCredential...")
        try {
            val makeCredentials = m_TrustKey_API_Bridge.TKAuthN_Fido_MakeCredential(jsonValue)
            promise.resolve(makeCredentials)
        } catch (e: Exception) {
            promise.reject("E_ERROR", e.message)
        }
    }

    @ReactMethod
    fun preMakeCredentialProcess(name: String, promise: Promise) {
        val urlPath = "https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoRegRequest.jsp"
        val url = URL(urlPath)
        val connection = url.openConnection() as HttpsURLConnection
        val nonJsonString = "name=$name&displayName=psjewbm.com&deviceId=&options=true&attachment=Cross_Platform&residentKey=true&userVerification=Preferred&attestation=Direct"

        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.setRequestProperty("Accept", "text*/*")
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;* Windows NT)")
        connection.setRequestProperty("Content-Length", nonJsonString.length.toString())

        try {
            m_TrustKey_API_Bridge.TKAuthN_GetDeviceHandle()

            (connection.apply {
                sslSocketFactory = createSocketFactory(listOf("TLSv1.2"))
                hostnameVerifier = HostnameVerifier { _, _ -> true }
                readTimeout = 5_000
            })

            DataOutputStream(connection.outputStream).use {
                it.writeBytes(URLDecoder.decode(nonJsonString, Charsets.UTF_8))
            }

            val code = connection.responseCode
            Log.d(TAG, code.toString())

            val streamReader = InputStreamReader(connection.inputStream)
            val buffered = BufferedReader(streamReader)
            val response = buffered.readText()

            Log.d(TAG, response)

            if ( m_TrustKey_API_Bridge.TKAuthN_Fido_MakeCredential(response) )
            {
                Log.d(TAG, m_TrustKey_API_Bridge.m_makeCredential_CTAP_Result.toString())

                if( postMakeCredentialProcess(m_TrustKey_API_Bridge.m_makeCredential_CTAP_Result) ) {
                    val resultMap = Arguments.createMap()

                    resultMap.putString("message", "Make credential Success")
                    resultMap.putString("result", m_TrustKey_API_Bridge.m_makeCredential_CTAP_Result.toString())
                    resultMap.putString("response", response)

                    promise.resolve(resultMap)
                } else {
                    promise.reject("ERROR", "Make credential Failed")
                }
            }
            else
            {
                m_TrustKey_API_Bridge.makeCredential_CTAP_Log()
                promise.reject("ERROR", "Make credential CTAP Failed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            promise.reject("E_ERROR", e.message)
        }
    }

    private fun postMakeCredentialProcess (pszCredential : ByteArray) : Boolean
    {
        val urlPath = "https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoRegProc.jsp"
        val url = URL(urlPath)
        val connection = url.openConnection() as HttpsURLConnection
        val credentialString = String(m_TrustKey_API_Bridge.m_makeCredential_CTAP_Result)
        val nonJsonString = "publicKeyCredential=$credentialString"

        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.setRequestProperty(
            "Content-Type",
            "application/x-www-form-urlencoded; charset=UTF-8"
        )
        connection.setRequestProperty("Accept", "text*/*")
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/4.0 (compatible; MSIE 5.0;* Windows NT)"
        )
        connection.setRequestProperty("Content-Length", nonJsonString.length.toString())

        try {
            (connection.apply {
                sslSocketFactory = createSocketFactory(listOf("TLSv1.2"))
                hostnameVerifier = HostnameVerifier { _, _ -> true }
                readTimeout = 5_000
            })

            DataOutputStream(connection.outputStream).use { it.writeBytes(nonJsonString) }

            val code = connection.responseCode
            Log.d(TAG, code.toString())

            val streamReader = InputStreamReader(connection.inputStream)
            val buffered = BufferedReader(streamReader)
            val response = buffered.readText()

            Log.d(TAG, response)

            if ( response.contains("1200") ) {
                Log.d(TAG, "Make credential Success!!")
                return true
            }
            else {
                Log.d(TAG, "Make credential Failed!!")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    @ReactMethod
    fun preGetAssertionProcess (name : String, promise : Promise)
    {
        val urlPath = "https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoAuthRequest.jsp"
        val url = URL(urlPath)
        val connection = url.openConnection() as HttpsURLConnection
        val nonJsonString = "name=$name&transaction=&userVerification=Preferred"

        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.setRequestProperty(
            "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"
        )
        connection.setRequestProperty("Accept", "text*/*")
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;* Windows NT)")
        connection.setRequestProperty("Content-Length", nonJsonString.length.toString())

        try {
            m_TrustKey_API_Bridge.TKAuthN_GetDeviceHandle()

            (connection.apply {
                sslSocketFactory = createSocketFactory(listOf("TLSv1.2"))
                hostnameVerifier = HostnameVerifier { _, _ -> true }
                readTimeout = 5_000
            })

            DataOutputStream(connection.outputStream).use { it.writeBytes(nonJsonString)}

            val code = connection.responseCode

            val streamReader = InputStreamReader(connection.inputStream)
            val buffered = BufferedReader(streamReader)
            val response = buffered.readText()

            Log.d(TAG, response)

            if ( m_TrustKey_API_Bridge.TKAuthN_Fido_GetAssertion(response) )
            {
                if(postGetAssertionProcess()) {
                    val resultMap = Arguments.createMap()

                    resultMap.putString("message", "Credential Authorization Success")
                    resultMap.putString("result", m_TrustKey_API_Bridge.m_getAssertion_CTAP_Result.toString())
                    resultMap.putString("response", response)

                    promise.resolve(resultMap)
                } else {
                    promise.reject("ERROR", "Credential Authorization Failed")
                }
            }
            else
            {
                m_TrustKey_API_Bridge.auth_CTAP_Log()
                promise.reject("ERROR", "Get assertion CTAP Failed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            promise.reject("E_ERROR", e.message)
        }
    }
    private fun postGetAssertionProcess () : Boolean
    {
        val urlPathPost =
            "https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoAuthProc.jsp"
        val urlPost = URL(urlPathPost)
        val connectionPost = urlPost.openConnection() as HttpsURLConnection
        val nonJsonStringPost =
            "publicKeyCredential=" + m_TrustKey_API_Bridge.m_getAssertion_CTAP_Result
        Log.d(TAG, nonJsonStringPost)

        connectionPost.requestMethod = "POST"
        connectionPost.doOutput = true

        connectionPost.setRequestProperty(
            "Content-Type",
            "application/x-www-form-urlencoded; charset=UTF-8"
        )
        connectionPost.setRequestProperty("Accept", "text*/*")
        connectionPost.setRequestProperty(
            "User-Agent",
            "Mozilla/4.0 (compatible; MSIE 5.0;* Windows NT)"
        )
        connectionPost.setRequestProperty("Content-Length", nonJsonStringPost.length.toString())

        try {
            (connectionPost.apply {
                sslSocketFactory = createSocketFactory(listOf("TLSv1.2"))
                hostnameVerifier = HostnameVerifier { _, _ -> true }
                readTimeout = 5_000
            })

            DataOutputStream(connectionPost.outputStream).use { it.writeBytes(nonJsonStringPost) }

            val codePost = connectionPost.responseCode

            val streamReaderPost = InputStreamReader(connectionPost.inputStream)
            val bufferedPost = BufferedReader(streamReaderPost)
            val response: String = bufferedPost.readText()

            Log.d(TAG, response)

            if ( response.contains("1200")) {
                Log.d(TAG, "Get assertion Success")
                return true
            }
            else {
                Log.d(TAG, "Get assertion Failed")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }
    private fun createSocketFactory(protocols: List<String>) =
        SSLContext.getInstance(protocols[0]).apply {

            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {

                override fun getAcceptedIssuers():
                        Array<out java.security.cert.X509Certificate>? = null

                override fun checkClientTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?,
                    authType: String? ) = Unit

                override fun checkServerTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?,
                    authType: String? ) = Unit
            })
            init(null, trustAllCerts, SecureRandom())
        }.socketFactory

    init {
        System.loadLibrary("trustkey_android_api")
        Log.d(TAG, "Load Library : trustkey_android_api")
    }
}