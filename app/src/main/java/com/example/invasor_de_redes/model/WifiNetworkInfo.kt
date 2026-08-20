package com.example.invasor_de_redes.model

/**
 * Representa uma rede Wi-Fi detectada no scan.
 * Contém apenas dados públicos de broadcast (beacon frame) — nada de credenciais.
 */
data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String, // ex: "[WPA2-PSK-CCMP][ESS]"
    val signalLevel: Int      // 0..4, calculado via WifiManager.calculateSignalLevel
) {
    val channel: Int
        get() = frequencyToChannel(frequencyMhz)

    val securityType: String
        get() = when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            else -> "Aberta"
        }

    val isOpen: Boolean
        get() = securityType == "Aberta"

    private fun frequencyToChannel(freq: Int): Int = when (freq) {
        in 2412..2484 -> (freq - 2412) / 5 + 1
        in 5170..5825 -> (freq - 5000) / 5
        in 5925..7125 -> (freq - 5950) / 5 + 1 // Wi-Fi 6E
        else -> -1
    }
}
