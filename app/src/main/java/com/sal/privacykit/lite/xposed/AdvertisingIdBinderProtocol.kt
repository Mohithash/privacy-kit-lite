package com.sal.privacykit.lite.xposed

import android.os.IBinder
import android.os.Parcel

internal enum class AdvertisingIdTransaction {
    GET_ID,
    IS_LIMIT_AD_TRACKING_ENABLED,
}

internal object AdvertisingIdBinderProtocol {
    private const val INTERFACE_DESCRIPTOR = "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService"
    private const val TRANSACTION_GET_ID = 1
    private const val TRANSACTION_IS_LIMIT_AD_TRACKING_ENABLED = 2

    fun classify(code: Int, flags: Int, descriptor: String?, reply: Parcel?): AdvertisingIdTransaction? {
        if (reply == null || flags and IBinder.FLAG_ONEWAY != 0) return null
        if (descriptor != INTERFACE_DESCRIPTOR) return null
        return when (code) {
            TRANSACTION_GET_ID -> AdvertisingIdTransaction.GET_ID
            TRANSACTION_IS_LIMIT_AD_TRACKING_ENABLED -> AdvertisingIdTransaction.IS_LIMIT_AD_TRACKING_ENABLED
            else -> null
        }
    }

    fun writeIdReply(reply: Parcel, advertisingId: String) {
        reset(reply)
        reply.writeNoException()
        reply.writeString(advertisingId)
        reply.setDataPosition(0)
    }

    fun writeLimitAdTrackingReply(reply: Parcel, limited: Boolean) {
        reset(reply)
        reply.writeNoException()
        reply.writeInt(if (limited) 1 else 0)
        reply.setDataPosition(0)
    }

    private fun reset(reply: Parcel) {
        reply.setDataPosition(0)
        reply.setDataSize(0)
    }
}
