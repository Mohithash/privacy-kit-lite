package com.sal.privacykit.xposed

import android.os.IBinder
import android.os.Parcel

internal enum class AdvertisingIdTransaction {
    GET_ID,
    IS_LIMIT_AD_TRACKING_ENABLED,
}

internal object AdvertisingIdBinderProtocol {
    const val INTERFACE_DESCRIPTOR = "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService"
    const val TRANSACTION_GET_ID = 1
    const val TRANSACTION_IS_LIMIT_AD_TRACKING_ENABLED = 2

    fun classify(code: Int, flags: Int, interfaceDescriptor: String?, reply: Parcel?): AdvertisingIdTransaction? {
        if (reply == null || flags and IBinder.FLAG_ONEWAY != 0) return null
        val transaction = when (code) {
            TRANSACTION_GET_ID -> AdvertisingIdTransaction.GET_ID
            TRANSACTION_IS_LIMIT_AD_TRACKING_ENABLED -> AdvertisingIdTransaction.IS_LIMIT_AD_TRACKING_ENABLED
            else -> return null
        }
        return transaction.takeIf { interfaceDescriptor == INTERFACE_DESCRIPTOR }
    }

    fun writeIdReply(reply: Parcel, advertisingId: String) {
        resetReply(reply)
        reply.writeNoException()
        reply.writeString(advertisingId)
        reply.setDataPosition(0)
    }

    fun writeLimitAdTrackingReply(reply: Parcel, limited: Boolean) {
        resetReply(reply)
        reply.writeNoException()
        reply.writeInt(if (limited) 1 else 0)
        reply.setDataPosition(0)
    }

    private fun resetReply(reply: Parcel) {
        reply.setDataPosition(0)
        reply.setDataSize(0)
    }
}
