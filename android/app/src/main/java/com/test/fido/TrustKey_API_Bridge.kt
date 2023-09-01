package kr.trustkey.api.bridge

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class EachDeviceRef(
    var m_usb_device: UsbDevice,
    var m_usb_interface: UsbInterface,
    var m_usb_in_endPoint: UsbEndpoint,
    var m_usb_out_endPoint : UsbEndpoint,
    var m_vid: Int,
    var m_pid: Int,
    var m_name: String,
)

class WebAuthNCredential (
    var m_dwVersion: Long = 0,
    var m_cdID: Long = 0,
    var m_pbID: ByteArray,
    var m_pwszCredentialType: String? )

data class WebAuthNUserEntityInformation (
    var m_dwVersion: Long = 0,
    var m_cbID: Long = 0,
    var m_pbID: ByteArray,
    var m_pwszName: String?,
    var m_pwszIcon: String?,
    var m_pwszDisplayName: String? )

class WebAuthNExtension (
    var m_pwszExtensionIdentifier : String?,
    var m_cbExtension : Long = 0,
    var m_pvExtension : ByteArray )

data class WebAuthNExtensions (
    var m_cExtensions: Long = 0,
    var m_pExtensions : WebAuthNExtension
)

class WebAuthNAssertionEX (
    var m_dwVersion: Long = 0,
    var m_cbAuthenticatorData : Long = 0,
    var m_pbAuthenticatorData : ByteArray,
    var m_cbSignature: Long = 0,
    var m_pbSignature :ByteArray,
    var m_credential : WebAuthNCredential,
    var m_cbUserID : Long = 0,
    var m_pbUserID : ByteArray,
    var m_extensions : WebAuthNExtensions )

class BaseResultType (
    var m_error_code : Int = 0,
    var m_error_reason : String? )

class UserInfoResultType (
    var m_result : BaseResultType,
    var m_userName : String?,
    var m_userTel : String?,
    var m_userAddr : String?,
    var m_userEtc: String?,
    var m_userLevel: String?,
    var m_userSecKey: ByteArray,
    var m_isWalletFile : Boolean = false,
    var m_nExpire: Long = 0 )

class WebAuthNX5C(
    var m_cbData : Long = 0,
    var m_pbData : ByteArray
)

class WebAuthNCommonAttestation (
    var m_dwVersion : Long = 0,
    var m_pwszAlg : String?,
    var m_lAlg : Long = 0,
    var m_cbSignature: Long = 0,
    var m_pbSignature: ByteArray,
    var m_cx5c: Long = 0,
    var m_px5c : WebAuthNX5C,
    var m_pwszVer: String?,
    var m_cbCertInfo : Long = 0,
    var m_pbCertInfo : ByteArray,
    var m_cbPubArea: Long = 0,
    var m_pbPubArea: ByteArray)

class WebAuthNCredentialAttestation (
    var m_dwVersion: Long = 0,
    var m_pwszFormatType : String?,
    var m_cbAuthenticatorData: Long = 0,
    var m_pbAuthenticatorData: ByteArray,
    var m_cbAttestation : Long = 0,
    var m_pbAttestation: ByteArray,
    var m_dwAttestationDecodeType : Long = 0,
    var m_pvAttestationDecode : WebAuthNCommonAttestation,
    var m_cbAttestationObject: Long = 0,
    var m_pbAttestationObject : ByteArray,
    var m_cbCredentialId : Long = 0,
    var m_pbCredentialId : ByteArray,
    var m_extensions: WebAuthNExtensions,
    var m_dwUsedTransport : Long = 0
)

class TrustKey_API_Bridge  {

    lateinit var m_callerContext: Context
    val m_device_custom_info : MutableList<EachDeviceRef> = ArrayList()
    /* 각각의 디바이스의 정보들을 저장합니다 예) UsbInterface, UsbDevice, UsbEndPoint,
       product ID, vendor ID 등등을 Array 로 저장합니다 */

    lateinit var m_usbManager: UsbManager
    lateinit var m_deviceList: HashMap<String, UsbDevice>
    private lateinit var m_connection: UsbDeviceConnection
    private val m_hid_packet_size : Int  = 64
    private val m_timeout : Int = 2000

    /* Basic UserInfo */
    lateinit var m_userId : String
    lateinit var m_userSeckey : String

    lateinit var m_makeCredential_CTAP_Result: ByteArray

    /* 강영도  preAuthProcess 가 끝난후 보안키 지문인증으로 부터 Assertion 관련 데이타를
    * 받아오게 되는데 이 데이터를 기공해서 서버에 전달하는 json 향식으로 만든 문자열이
    * m_jsonFormatAssertion 입니다 이 모든 일련의  과정은 라이브러리 내부에서 일어납니다 */
    lateinit var m_getAssertion_CTAP_Result : String

    val m_environment_rpID_URL : String = "https://demo.trustkeysolutions.com:12001"
    lateinit var m_environment_rpID : String

    /* MakeCredential Related */
    private lateinit var m_webAuthNCredentialAttestation : WebAuthNCredentialAttestation
    private lateinit var m_webAuthNCommonAttestation : WebAuthNCommonAttestation
    private lateinit var m_webAuthNX5C:  WebAuthNX5C

    /* GetAssertion Related */
    private lateinit var m_webAuthNUserEntityInformation : WebAuthNUserEntityInformation
    private lateinit var m_webAuthNAssertionEx : WebAuthNAssertionEX
    private lateinit var m_webAuthNCredential : WebAuthNCredential
    private lateinit var m_baseResult : BaseResultType
    private lateinit var m_userInfoResult : UserInfoResultType

    private val ACTION_USB_PERMISSION = "com.test.USB_PERMISSION"
    private val TAG = "ReactNative"

    private var connectionPromise: Promise? = null

    private fun rejectConnectionPromise(code: String, message: String) {
        Log.e(TAG, message)
        connectionPromise?.reject(code, message)
        connectionPromise = null
    }

    fun initSetting () {
        Log.d(TAG, "Init Setting...")
        m_environment_rpID = m_environment_rpID_URL.substringAfter("//")
        m_environment_rpID = m_environment_rpID.substringBefore(":")


        send_ContextToModule()
        setEnvironmentData()

        /* =================  아래의 함수에서 각종 클래스들의 초기화를 진행합니다 ============= */
        Log.d(TAG, "class initialization...")
        classInitialization()
        /* =========================================================================== */
    }

    // @ReactMethod
    // fun getMakeCredentialCTAPResult(promise: Promise) {
    //     // Assuming m_makeCredential_CTAP_Result is defined in your native module
    //     byte[] resultBytes = m_makeCredential_CTAP_Result;
        
    //     // Convert the byte array to a writable array for JavaScript
    //     WritableArray resultArray = Arguments.createArray();
    //     for (byte b : resultBytes) {
    //         resultArray.pushInt(b & 0xFF);
    //     }

    //     promise.resolve(resultArray);
    // }

    fun makeCredential_CTAP_Log()
    {
        Log.d("trustkey", "[Credential Attestation] dwVersion = " + m_webAuthNCredentialAttestation.m_dwVersion.toString())
        Log.d("trustkey", "[Credential Attestation] pwszFormatType = " + m_webAuthNCredentialAttestation.m_pwszFormatType.toString())
        Log.d("trustkey", "[Credential Attestation] cbAuthenticatorData = " + m_webAuthNCredentialAttestation.m_cbAuthenticatorData.toString())
        Log.d("trustkey", "[Credential Attestation] pbAuthenticatorData = " + BytesToHex(m_webAuthNCredentialAttestation.m_pbAuthenticatorData))
        Log.d("trustkey", "[Credential Attestation] cbAttestation = " + m_webAuthNCredentialAttestation.m_cbAttestation.toString())
        Log.d("trustkey", "[Credential Attestation] pbAttestation = " + BytesToHex(m_webAuthNCredentialAttestation.m_pbAttestation))
        Log.d("trustkey", "[Credential Attestation] dwAttestationDecodeType = " + m_webAuthNCredentialAttestation.m_dwAttestationDecodeType.toString())
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] dwVersion = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_dwVersion.toString())
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] pwszAlg = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_pwszAlg.toString())
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] lAlg = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_lAlg.toString())
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] cbSignature = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_cbSignature.toString())
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] pbSignature = " + BytesToHex(m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_pbSignature))
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] cx5c = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_cx5c.toString())
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode pX5C ] cbData = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_px5c.m_cbData.toString())
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode pX5C ] pbData = " + BytesToHex(m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_px5c.m_pbData))
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] pwszVer = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_pwszVer.toString())
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] cbCertInfo = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_cbCertInfo.toString())
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] pbCertInfo = " + BytesToHex(m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_pbCertInfo))
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] cbPubArea = " + m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_cbPubArea.toString())
        Log.d("trustkey", "[Credential Attestation pvAttestationDecode ] pbPubArea = " + BytesToHex(m_webAuthNCredentialAttestation.m_pvAttestationDecode.m_pbPubArea))
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Credential Attestation] cbAttestationObject = " + m_webAuthNCredentialAttestation.m_cbAttestationObject.toString())
        Log.d("trustkey", "[Credential Attestation] pbAttestationObject = " + BytesToHex(m_webAuthNCredentialAttestation.m_pbAttestationObject))
        Log.d("trustkey", "[Credential Attestation] cbCredentialId = " + m_webAuthNCredentialAttestation.m_cbCredentialId.toString())
        Log.d("trustkey", "[Credential Attestation] pbCredentialId = " + BytesToHex(m_webAuthNCredentialAttestation.m_pbCredentialId))
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Credential Attestation Extensions] cExtensions = " + m_webAuthNCredentialAttestation.m_extensions.m_cExtensions.toString())
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Credential Attestation Extensions pExtensions ] pwszExtensionIdentifier = " + m_webAuthNCredentialAttestation.m_extensions.m_pExtensions.m_pwszExtensionIdentifier.toString())
        Log.d("trustkey", "[Credential Attestation Extensions pExtensions ] cbExtension = " + m_webAuthNCredentialAttestation.m_extensions.m_pExtensions.m_cbExtension.toString())
        Log.d("trustkey", "[Credential Attestation Extensions pExtensions ] pvExtension = " + BytesToHex(m_webAuthNCredentialAttestation.m_extensions.m_pExtensions.m_pvExtension))
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Credential Attestation] cbCredentialId = " + m_webAuthNCredentialAttestation.m_dwUsedTransport.toString())
    }

    fun auth_CTAP_Log()
    {
        /* 아래의 로그들은 결과값의 로그를 나타 냅니다 */
        Log.d("trustkey", "[UserInfo] dwVersion = " + m_webAuthNUserEntityInformation.m_dwVersion.toString())
        Log.d("trustkey", "[UserInfo] cbID = " + m_webAuthNUserEntityInformation.m_cbID.toString())
        Log.d("trustkey", "[UserInfo] name = " + m_webAuthNUserEntityInformation.m_pwszName.toString())
        Log.d("trustkey", "[UserInfo] pbID = " + BytesToHex(m_webAuthNUserEntityInformation.m_pbID))
        Log.d("trustkey", "[UserInfo] Icon = " + m_webAuthNUserEntityInformation.m_pwszIcon.toString())
        Log.d("trustkey", "[UserInfo] displayName = " + m_webAuthNUserEntityInformation.m_pwszDisplayName.toString())
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Assertion EX] dwVersion = " + m_webAuthNAssertionEx.m_dwVersion.toString())
        Log.d("trustkey", "[Assertion EX] cbAuthenticatorData = " + m_webAuthNAssertionEx.m_cbAuthenticatorData.toString())
        Log.d("trustkey", "[Assertion EX] pbAuthenticatorData = " + BytesToHex(m_webAuthNAssertionEx.m_pbAuthenticatorData))
        Log.d("trustkey", "[Assertion EX] cbSignature = " + m_webAuthNAssertionEx.m_cbSignature.toString())
        Log.d("trustkey", "[Assertion EX] pbSignature = " + BytesToHex(m_webAuthNAssertionEx.m_pbSignature))
        Log.d("trustkey", "[Assertion EX] cbUserID = " + m_webAuthNAssertionEx.m_cbUserID.toString())
        Log.d("trustkey", "[Assertion EX] pbUserID = " + BytesToHex(m_webAuthNAssertionEx.m_pbUserID))
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Assertion EX - Credential] dwVersion = " + m_webAuthNAssertionEx.m_credential.m_dwVersion.toString())
        Log.d("trustkey", "[Assertion EX - Credential] cbID = " + m_webAuthNAssertionEx.m_credential.m_cdID.toString())
        Log.d("trustkey", "[Assertion EX - Credential] pbID = " + BytesToHex(m_webAuthNAssertionEx.m_credential.m_pbID))
        Log.d("trustkey", "[Assertion EX - Credential] credentialType = " + m_webAuthNAssertionEx.m_credential.m_pwszCredentialType.toString())
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Assertion EX - Extensions] cExtensions = " + m_webAuthNAssertionEx.m_extensions.m_cExtensions.toString())
        Log.d("trustkey", " ")
        Log.d("trustkey", " ")
        Log.d("trustkey", "[Assertion EX - Extensions - Extension - pbExtensions] pwszExtensionIdentifier = " + m_webAuthNAssertionEx.m_extensions.m_pExtensions.m_pwszExtensionIdentifier.toString())
        Log.d("trustkey", "[Assertion EX - Extensions - Extension - pbExtensions] cbExtension = " + m_webAuthNAssertionEx.m_extensions.m_pExtensions.m_cbExtension.toString())
        Log.d("trustkey", "[Assertion EX - Extensions - Extension - pbExtensions] pvExtension = " + BytesToHex(m_webAuthNAssertionEx.m_extensions.m_pExtensions.m_pvExtension))
    }

    /* 이 샘플에서 사용 되는 각종 클래스들의 초기화를 시행 합니다 */
    private fun classInitialization()
    {
        m_webAuthNUserEntityInformation =
            WebAuthNUserEntityInformation(0,0, byteArrayOf(0x00),null,null, null)

        m_webAuthNCredential = WebAuthNCredential(0, 0, byteArrayOf(0x00), null)

        m_webAuthNAssertionEx = WebAuthNAssertionEX(0, 0,
            byteArrayOf(0x00), 0, byteArrayOf(0x00), m_webAuthNCredential, 0, byteArrayOf(0x00), WebAuthNExtensions(0, WebAuthNExtension(null, 0, byteArrayOf(0x00)))
        )

        m_baseResult = BaseResultType(0, null)

        m_userInfoResult = UserInfoResultType(m_baseResult, null, null, null , null, null, byteArrayOf(0x00), false, 0)

        m_webAuthNX5C = WebAuthNX5C(0, byteArrayOf(0x00))

        m_webAuthNCommonAttestation = WebAuthNCommonAttestation(0, null, 0, 0, byteArrayOf(0x00), 0, m_webAuthNX5C, null, 0, byteArrayOf(0x00),0,
            byteArrayOf(0x00))

        m_webAuthNCredentialAttestation = WebAuthNCredentialAttestation( 0,null,0, byteArrayOf(0x00), 0,
            byteArrayOf(0x00), 0, m_webAuthNCommonAttestation, 0, byteArrayOf(0x00),0,
            byteArrayOf(0x00),  WebAuthNExtensions(0, WebAuthNExtension(null, 0, byteArrayOf(0x00))) ,0)
    }

    fun BytesToHex(bytes: ByteArray): String {

        val hexArray = "0123456789ABCDEF".toCharArray()
        val space = " ".toCharArray()
        val hexChars = CharArray(bytes.size * 3)

        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF

            hexChars[j * 3] = hexArray[v ushr 4]
            hexChars[j * 3 + 1] = hexArray[v and 0x0F]
            hexChars[j * 3 + 2] = space[0]
        }

        return String(hexChars)
    }

    /* 이 함수에서 실제적으로 각종 하드웨어 정보를 얻어 옵니다 */

    fun app_hid_enumerate(vendor_id: Int, product_id: Int)
    {
        val deviceIterator: Iterator<UsbDevice> = m_deviceList.values.iterator()

        var idx  = 0

        while (deviceIterator.hasNext()) {

            val _device = deviceIterator.next()

            var hid_interface_num : Int  = 0

            for (i in 0 .._device.interfaceCount - 1) {
                val temp = _device.getInterface(i)
                if (temp.interfaceClass == UsbConstants.USB_CLASS_HID)
                {
                    hid_interface_num = i
                    break
                }

            }

            val _interface = _device.getInterface(hid_interface_num)

            var _in: Int = 0
            var _out: Int = 0

            for ( i in 0 .. _interface.endpointCount - 1 )
            {
                val temp = _interface.getEndpoint(i)
                if ( temp.direction == UsbConstants.USB_DIR_IN )
                    _in = i

                if ( temp.direction == UsbConstants.USB_DIR_OUT )
                    _out = i
            }

            val _in_endPoint = _interface.getEndpoint(_in)
            val _out_endPoint = _interface.getEndpoint(_out)
            val _vendor_id = _device.vendorId
            val _product_id = _device.productId
            val _name = _device.deviceName

            m_device_custom_info.add(idx, EachDeviceRef(_device, _interface, _in_endPoint,
                _out_endPoint,_vendor_id, _product_id, _name))

            idx++
        }
    }

    fun app_hid_write(writeData: ByteArray, writeLength: Int ) : Int
    {
        /* writeLength 는 현재 사용되지 않습니다 */
        /* 그 대신 m_hid_packet_size 가 사용됩니다 */
        if ( m_device_custom_info.isEmpty())
        {
            return -1
        }

        try {
            m_connection = m_usbManager.openDevice(m_device_custom_info[0].m_usb_device)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        m_connection.claimInterface(m_device_custom_info[0].m_usb_interface, true)

        /* Windows 버전과의 버퍼 차이를 보정하기 위해 사용됩니다 */
        val tempArray = ByteArray(writeData.size)
        System.arraycopy(writeData, 1, tempArray, 0, m_hid_packet_size)

        val sent: Int =
            m_connection.bulkTransfer(m_device_custom_info[0].m_usb_out_endPoint,
                tempArray, m_hid_packet_size, m_timeout)

        m_connection.releaseInterface(m_device_custom_info[0].m_usb_interface)
        m_connection.close()

        return sent
    }

    fun app_hid_read_timeout(readData: ByteArray, readLength: Int, milliseconds: Int) : Int
    {
        /* milliseconds 는 현재 사용되지 않습니다
         * 그 대신에 안드로이드에서 정의한 m_timeout 을 이용합니다
        */
        m_connection = m_usbManager.openDevice(m_device_custom_info[0].m_usb_device)
        m_connection.claimInterface(m_device_custom_info[0].m_usb_interface, true)

        var dataReceivedNum : Int =
            m_connection.bulkTransfer(m_device_custom_info[0].m_usb_in_endPoint,
                readData, m_hid_packet_size, 0 /* 무한 timeout */)

        m_connection.releaseInterface(m_device_custom_info[0].m_usb_interface)
        m_connection.close()

        // 받은 데이타를 모듈 쪽으로 넘깁니다
        send_HID_ReceivedDataToModule(readData, readLength)

        return dataReceivedNum
    }

    init {
        Log.d(TAG, "Creating TrustKey Api Bridge class!!")
    }

    companion object
    {
    }

    external fun send_ContextToModule()

    /*  USB HID 에서 받은 데이터들을 TrustKey 모듈 쪽으로 보냅니다 */
    external fun send_HID_ReceivedDataToModule(receivedData: ByteArray, receivedDataSize: Int)

    /*  주요 환경변수들 예) URL 주소 등등을 라이브러리로 전달합니다 */
    external fun setEnvironmentData()
    external fun TKAuthN_GetDeviceHandle() : String
    external fun TKAuthN_Fido_GetAssertion( jsonValue : String ) : Boolean
    external fun TKAuthN_Fido_MakeCredential( jsonValue: String ) : Boolean
}