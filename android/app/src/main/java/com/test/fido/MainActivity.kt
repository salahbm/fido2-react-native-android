package com.ewbm.myapplication

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.ewbm.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.URL
import java.net.URLDecoder
import java.security.SecureRandom
import javax.net.ssl.*
import kotlin.Exception

import kr.trustkey.api.bridge.TrustKey_API_Bridge

private const val ACTION_USB_PERMISSION = "com.ewbm.myapplication.USB_PERMISSION"

private val usbReceiver = object : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val m_TrustKey_API_Bridge = TrustKey_API_Bridge()

    private lateinit var m_userID_Registration : String
    private lateinit var m_userID_Auth: String

    @RequiresApi(33)
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fido2RegisterButton.isEnabled = false
        binding.fido2AuthButton.isEnabled = false

        binding.fido2RegisterButton.setOnClickListener {
            m_userID_Registration = binding.editTextIDRegistration.text.toString()
            Log.d("trustkey", m_userID_Registration)
            preMakeCredentialProcess()
        }

        binding.fido2AuthButton.setOnClickListener {
            m_userID_Auth = binding.editTextIDAuth.text.toString()
            Log.d("trustkey", m_userID_Auth)
            preGetAssertionProcess()
        }

        binding.editTextIDRegistration.addTextChangedListener(object : TextWatcher
        {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
                binding.fido2RegisterButton.isEnabled = s.toString().isNotEmpty()
                //s.toString().trim { it <= ' '}.matches(emailPattern.toRegex())
            }

            override fun afterTextChanged(s: Editable?) {
                //TODO("Not yet implemented")
            }
        })

        binding.editTextIDAuth.addTextChangedListener(object : TextWatcher
        {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
                binding.fido2AuthButton.isEnabled = s.toString().isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
                //TODO("Not yet implemented")
            }
        })




        /* classContextToHID 함수는 .so 공용 라이브러리의  USB HID관련 함수들은 현재의 클래스
           MainActivity에 구현된 각종 USB 관련 함수들을 통해 실제로 USB 통신을 합니다 따라서 라이브러리는
           MainActivty의 함수나 변수에 접근이 가능해야 합니다 그러기 위해서 현재의 클래스의 Context
           정보를 jobject 파라메터를 라이브러리 쪽에 전달을 합니다
        */

        m_TrustKey_API_Bridge.m_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        m_TrustKey_API_Bridge.m_deviceList = m_TrustKey_API_Bridge.m_usbManager.deviceList


        m_TrustKey_API_Bridge.initSetting()

        val permissionIntent =
            PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE)

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
        val deviceIterator: Iterator<UsbDevice> = m_TrustKey_API_Bridge.m_deviceList.values.iterator()

        var idx  = 0

        while (deviceIterator.hasNext()) {

            val _device = deviceIterator.next()
            m_TrustKey_API_Bridge.m_usbManager.requestPermission(_device, permissionIntent)
            idx++
        }
    }

    @RequiresApi(33)
    fun preMakeCredentialProcess ()
    {
        m_TrustKey_API_Bridge.TKAuthN_GetDeviceHandle()

        GlobalScope.launch(Dispatchers.IO)
        {
            val urlPath = "https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoRegRequest.jsp"
            val url = URL(urlPath)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            val nonJsonString = "name=$m_userID_Registration&displayName=psjewbm.com&deviceId=&options=true&attachment=Cross_Platform&residentKey=true&userVerification=Preferred&attestation=Direct"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.setRequestProperty("Accept", "text*/*")
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;* Windows NT)")
            connection.setRequestProperty("Content-Length", nonJsonString.length.toString())

            try {
                (connection.apply {
                    sslSocketFactory = createSocketFactory(listOf("TLSv1.2"))
                    hostnameVerifier = HostnameVerifier { _, _ -> true }
                    readTimeout = 5_000
                })

                DataOutputStream(connection.outputStream).use { it.writeBytes(URLDecoder.decode(nonJsonString, Charsets.UTF_8)) }

                val code = connection.responseCode
                Log.d("trustkey", code.toString())

                val streamReader = InputStreamReader(connection.inputStream)
                val buffered = BufferedReader(streamReader)
                val response = buffered.readText()

                Log.d("trustkey", response)

                if ( m_TrustKey_API_Bridge.TKAuthN_Fido_MakeCredential(response) )
                {
                    Log.d("trustkey", m_TrustKey_API_Bridge.m_makeCredential_CTAP_Result.toString())

                    postMakeCredentialProcess(m_TrustKey_API_Bridge.m_makeCredential_CTAP_Result)
                }
                else
                {
                    launch (Dispatchers.Main)
                    {
                        m_TrustKey_API_Bridge.makeCredential_CTAP_Log()
                        Toast.makeText(this@MainActivity, "Make credential CTAP Failed", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun postMakeCredentialProcess (pszCredential : ByteArray)
    {
        GlobalScope.launch(Dispatchers.IO)
        {
            val urlPath = "https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoRegProc.jsp"
            val url = URL(urlPath)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            val credentialString = String(m_TrustKey_API_Bridge.m_makeCredential_CTAP_Result)
            val nonJsonString = "publicKeyCredential=$credentialString"
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
                Log.d("trustkey", code.toString())

                val streamReader = InputStreamReader(connection.inputStream)
                val buffered = BufferedReader(streamReader)
                val  response = buffered.readText()

                Log.d("trustkey", response)

                launch (Dispatchers.Main)
                {
                    //m_TrustKey_API_Bridge.makeCredential_CTAP_Log()

                    if ( response.contains("1200") )
                        Toast.makeText(this@MainActivity, "Make credential Success!!", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this@MainActivity, "Make credential Failed!!", Toast.LENGTH_SHORT).show()

                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(33)
    fun preGetAssertionProcess ()
    {
        m_TrustKey_API_Bridge.TKAuthN_GetDeviceHandle()

        GlobalScope.launch(Dispatchers.IO)
        {
            val urlPath = "https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoAuthRequest.jsp"
            val url = URL(urlPath)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            val nonJsonString = "name=$m_userID_Auth&transaction=&userVerification=Preferred"
            connection.setRequestProperty(
                "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"
            )
            connection.setRequestProperty("Accept", "text*/*")
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;* Windows NT)")
            connection.setRequestProperty("Content-Length", nonJsonString.length.toString())

            try {
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

                Log.d("trustkey", response)

                if ( m_TrustKey_API_Bridge.TKAuthN_Fido_GetAssertion(response) )
                {
                    postGetAssertionProcess()
                }
                else
                {
                    launch (Dispatchers.Main)
                    {
                        m_TrustKey_API_Bridge.auth_CTAP_Log()
                        Toast.makeText(this@MainActivity, "get assertion CTAP failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun postGetAssertionProcess ()
    {
        GlobalScope.launch( Dispatchers.IO)
        {
            val urlPathPost =
                "https://demo.trustkeysolutions.com:12001/FidoDemo/demoDeveloper/fidoAuthProc.jsp"
            val urlPost = URL(urlPathPost)
            val connectionPost = urlPost.openConnection() as HttpsURLConnection
            connectionPost.requestMethod = "POST"

            connectionPost.doOutput = true
            val nonJsonStringPost =
                "publicKeyCredential=" + m_TrustKey_API_Bridge.m_getAssertion_CTAP_Result
            Log.d("trustkey", nonJsonStringPost)
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

                Log.d("trustkey", response)

                launch(Dispatchers.Main)
                {
                    //m_TrustKey_API_Bridge.auth_CTAP_Log()
                    if ( response.contains("1200"))
                        Toast.makeText(this@MainActivity, "get assertion Success", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this@MainActivity, "get assertion Failed", Toast.LENGTH_SHORT).show()



                }


            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
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

    /**
     * A native method that is implemented by the 'myapplication' native library,
     * which is packaged with this application.
     */
    companion object {

        init {
            System.loadLibrary("trustkey_android_api")
        }
    }
}