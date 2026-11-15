package com.sonora.studio

import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * M4 output-route readout. Listing local output devices needs no
 * permission; failures degrade to an empty list, never a crash.
 */
object OutputRoutes {
    fun current(audioManager: AudioManager): List<String> {
        return try {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { it.isSink }
                .map { typeName(it.type) }
                .distinct()
                .ifEmpty { listOf("Unknown output") }
        } catch (_: Exception) {
            listOf("Unknown output")
        }
    }

    /** Human name for an [AudioDeviceInfo] type. Pure and tested. */
    fun typeName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth audio"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth voice"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE headset"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE speaker"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            AudioDeviceInfo.TYPE_DOCK -> "Dock"
            AudioDeviceInfo.TYPE_FM -> "FM"
            else -> "Audio output"
        }
    }
}
