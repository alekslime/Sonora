package com.sonora.studio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/** Live output-route information; no permissions needed for outputs. */
data class OutputInfo(val devices: List<String>)

/** Best-effort output device list, never throws. */
fun queryOutputDevices(context: Context): OutputInfo {
    val manager = context.getSystemService(AudioManager::class.java) ?: return OutputInfo(emptyList())
    val devices = try {
        manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    } catch (_: Exception) {
        return OutputInfo(emptyList())
    }
    val names = devices
        .map { device ->
            outputTypeName(device.type).ifEmpty {
                device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Output"
            }
        }
        .distinct()
    return OutputInfo(names.ifEmpty { listOf("Default output") })
}

private fun outputTypeName(type: Int): String = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB audio"
    AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI"
    else -> ""
}
