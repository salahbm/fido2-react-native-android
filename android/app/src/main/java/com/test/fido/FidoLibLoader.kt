package com.test.fido

class FidoLibLoader {
    companion object {
        @JvmStatic fun LoadLib(): Boolean {
            return try {
                System.loadLibrary("trustkey_android_api")
                true
            } catch (ex: Exception) {
                System.err.println("WARNING: Could not trustkey load library")
                false
            }
        }
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