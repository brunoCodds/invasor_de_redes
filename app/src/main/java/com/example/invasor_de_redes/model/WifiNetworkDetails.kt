package com.example.invasor_de_redes.model

import android.net.wifi.ScanResult
import android.os.Build
import com.example.invasor_de_redes.util.OuiLookup
import com.example.invasor_de_redes.util.SecurityInfo
import com.example.invasor_de_redes.util.SecurityParser

data class IpInfo(
    val ipv4Address: String?,
    val netmask: String?,
    val gateway: String?,
    val dns: List<String>,
    val dhcpServer: String?
)

data class WifiNetworkDetails(
    // 1. Identificação
    val ssid: String,
    val bssid: String,
    val isHidden: Boolean,
    val vendor: String,

    // 2. Sinal e rádio
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val band: String,
    val channel: Int,
    val channelWidthLabel: String,
    val centerFreq0: Int,
    val centerFreq1: Int,

    // 3. Padrão Wi-Fi
    val wifiStandardLabel: String,

    // 4. Segurança
    val security: SecurityInfo,

    // 5. Informações do AP (o que é realmente exposto pela API pública Android)
    val rawCapabilities: String,

    // 6. Rede/IP — só preenchido se esta é a rede atualmente conectada
    val isCurrentlyConnected: Boolean,
    val ipInfo: IpInfo?
) {
    companion object {
        fun fromScanResult(
            result: ScanResult,
            isCurrentlyConnected: Boolean,
            ipInfo: IpInfo?
        ): WifiNetworkDetails {
            val ssid = result.SSID ?: ""
            val freq = result.frequency
            val band = bandFor(freq)
            val channel = frequencyToChannel(freq)

            return WifiNetworkDetails(
                ssid = ssid,
                bssid = result.BSSID ?: "",
                isHidden = ssid.isBlank(),
                vendor = OuiLookup.vendorFor(result.BSSID ?: ""),
                rssiDbm = result.level,
                frequencyMhz = freq,
                band = band,
                channel = channel,
                channelWidthLabel = channelWidthLabel(result),
                centerFreq0 = centerFreq0(result),
                centerFreq1 = centerFreq1(result),
                wifiStandardLabel = wifiStandardLabel(result),
                security = SecurityParser.parse(result.capabilities ?: ""),
                rawCapabilities = result.capabilities ?: "",
                isCurrentlyConnected = isCurrentlyConnected,
                ipInfo = ipInfo
            )
        }

        private fun bandFor(freq: Int): String = when (freq) {
            in 2412..2484 -> "2,4 GHz"
            in 5150..5895 -> "5 GHz"
            in 5925..7125 -> "6 GHz"
            else -> "Desconhecida ($freq MHz)"
        }

        private fun frequencyToChannel(freq: Int): Int = when (freq) {
            in 2412..2484 -> (freq - 2412) / 5 + 1
            in 5170..5895 -> (freq - 5000) / 5
            in 5925..7125 -> (freq - 5950) / 5 + 1
            else -> -1
        }

        private fun channelWidthLabel(result: ScanResult): String {
            return try {
                when (result.channelWidth) {
                    ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
                    ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
                    ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
                    ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
                    ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
                    else -> "Desconhecida"
                }
            } catch (e: Exception) {
                "Não disponível"
            }
        }

        private fun centerFreq0(result: ScanResult): Int =
            try { result.centerFreq0 } catch (e: Exception) { -1 }

        private fun centerFreq1(result: ScanResult): Int =
            try { result.centerFreq1 } catch (e: Exception) { -1 }

        private fun wifiStandardLabel(result: ScanResult): String {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return "Não disponível (requer Android 11+)"
            }
            return try {
                when (result.wifiStandard) {
                    ScanResult.WIFI_STANDARD_LEGACY -> "802.11a/b/g (legado)"
                    ScanResult.WIFI_STANDARD_11N -> "802.11n (Wi-Fi 4)"
                    ScanResult.WIFI_STANDARD_11AC -> "802.11ac (Wi-Fi 5)"
                    ScanResult.WIFI_STANDARD_11AX -> "802.11ax (Wi-Fi 6/6E)"
                    ScanResult.WIFI_STANDARD_11AD -> "802.11ad (WiGig)"
                    11 -> "802.11be (Wi-Fi 7)" // WIFI_STANDARD_11BE (API 34+), valor fixo p/ compatibilidade de compilação
                    else -> "Não anunciado pelo AP"
                }
            } catch (e: Exception) {
                "Não disponível"
            }
        }
    }
}
