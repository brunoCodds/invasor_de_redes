package com.example.invasor_de_redes

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.invasor_de_redes.databinding.ActivityNetworkDetailBinding
import com.example.invasor_de_redes.model.IpInfo
import com.example.invasor_de_redes.model.WifiNetworkDetails

class NetworkDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkDetailBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private var targetBssid: String = ""

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshIntervalMs = 5_000L
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadAndRender()
            refreshHandler.postDelayed(this, refreshIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        targetBssid = intent.getStringExtra(EXTRA_BSSID) ?: ""
        binding.toolbar.title = intent.getStringExtra(EXTRA_SSID)?.ifBlank { "(SSID oculto)" }
            ?: "Detalhes da rede"

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        refreshHandler.post(refreshRunnable)
    }

    override fun onStop() {
        super.onStop()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun loadAndRender() {
        try {
            val result = wifiManager.scanResults.firstOrNull { it.BSSID == targetBssid }
            if (result == null) {
                binding.textLastUpdate.text = "Rede não encontrada no último scan (fora de alcance?)"
                return
            }

            val currentBssid = currentConnectedBssid()
            val isConnected = currentBssid != null && currentBssid.equals(targetBssid, ignoreCase = true)
            val ipInfo = if (isConnected) buildIpInfo() else null

            val details = WifiNetworkDetails.fromScanResult(result, isConnected, ipInfo)
            render(details)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permissão de localização necessária.", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun currentConnectedBssid(): String? {
        return try {
            val info = wifiManager.connectionInfo
            if (info != null && info.networkId != -1) info.bssid else null
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun buildIpInfo(): IpInfo? {
        return try {
            val dhcp = wifiManager.dhcpInfo ?: return null
            fun intToIp(addr: Int): String = String.format(
                "%d.%d.%d.%d",
                addr and 0xff, addr shr 8 and 0xff, addr shr 16 and 0xff, addr shr 24 and 0xff
            )
            val dnsList = mutableListOf<String>()
            if (dhcp.dns1 != 0) dnsList.add(intToIp(dhcp.dns1))
            if (dhcp.dns2 != 0) dnsList.add(intToIp(dhcp.dns2))

            IpInfo(
                ipv4Address = if (dhcp.ipAddress != 0) intToIp(dhcp.ipAddress) else null,
                netmask = if (dhcp.netmask != 0) intToIp(dhcp.netmask) else null,
                gateway = if (dhcp.gateway != 0) intToIp(dhcp.gateway) else null,
                dns = dnsList,
                dhcpServer = if (dhcp.serverAddress != 0) intToIp(dhcp.serverAddress) else null
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun render(d: WifiNetworkDetails) {
        binding.textLastUpdate.text = "Atualizado agora • auto-refresh a cada ${refreshIntervalMs / 1000}s"

        // 1. Identificação
        binding.textSsid.text = d.ssid.ifBlank { "(SSID oculto)" }
        binding.textBssid.text = d.bssid
        binding.textHidden.text = if (d.isHidden) "Sim (SSID não veio no beacon)" else "Não"
        binding.textVendor.text = d.vendor

        // 2. Sinal e rádio
        binding.textRssi.text = "${d.rssiDbm} dBm"
        binding.textFrequency.text = "${d.frequencyMhz} MHz"
        binding.textBand.text = d.band
        binding.textChannel.text = if (d.channel > 0) d.channel.toString() else "Não identificado"
        binding.textChannelWidth.text = d.channelWidthLabel
        binding.textCenterFreq.text = buildString {
            append(if (d.centerFreq0 > 0) "${d.centerFreq0} MHz" else "—")
            if (d.centerFreq1 > 0) append(" / ${d.centerFreq1} MHz")
        }

        // 3. Padrão Wi-Fi
        binding.textWifiStandard.text = d.wifiStandardLabel

        // 4. Segurança
        binding.textSecurityLabel.text = d.security.label
        binding.textKeyManagement.text =
            d.security.keyManagement.ifEmpty { listOf("—") }.joinToString(", ")
        binding.textCiphers.text =
            d.security.ciphers.ifEmpty { listOf("Não anunciado") }.joinToString(", ")
        binding.textPmf.text = d.security.pmf
        binding.textWps.text = if (d.security.wpsAdvertised) "Anunciado no beacon" else "Não anunciado"
        binding.textRawCapabilities.text = d.rawCapabilities

        // 5. Radio avançado — transparência sobre o que a API do Android NÃO expõe
        binding.textAdvancedRadioNote.text =
            "Beacon interval, DTIM, HT/VHT/HE/EHT capabilities, MU-MIMO, OFDMA, " +
            "beamforming, spatial streams, MCS e guard interval exigem parsing de baixo " +
            "nível do frame 802.11 que a API pública do Android (ScanResult) não expõe. " +
            "Não estão disponíveis nesta tela por esse motivo — não é uma limitação deste app."

        // 6. Rede/IP
        if (d.isCurrentlyConnected && d.ipInfo != null) {
            binding.groupIpInfo.visibility = android.view.View.VISIBLE
            binding.textConnectionStatus.text = "Você está conectado a esta rede agora"
            binding.textIpv4.text = d.ipInfo.ipv4Address ?: "—"
            binding.textNetmask.text = d.ipInfo.netmask ?: "—"
            binding.textGateway.text = d.ipInfo.gateway ?: "—"
            binding.textDns.text = d.ipInfo.dns.ifEmpty { listOf("—") }.joinToString(", ")
            binding.textDhcpServer.text = d.ipInfo.dhcpServer ?: "—"
        } else {
            binding.groupIpInfo.visibility = android.view.View.GONE
            binding.textConnectionStatus.text =
                "Não conectado a esta rede — dados de IP só aparecem quando conectado"
        }
    }

    companion object {
        const val EXTRA_BSSID = "extra_bssid"
        const val EXTRA_SSID = "extra_ssid"
    }
}
