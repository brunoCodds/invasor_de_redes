package com.example.invasor_de_redes

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.invasor_de_redes.adapter.WifiNetworkAdapter
import com.example.invasor_de_redes.databinding.ActivityMainBinding
import com.example.invasor_de_redes.model.WifiNetworkInfo

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var adapter: WifiNetworkAdapter

    // Atualização automática "quase tempo real". O Android throttla scans
    // (geralmente ~4 por 2 min mesmo em foreground), então 20s é um intervalo
    // seguro que não é bloqueado pelo sistema na maioria dos aparelhos.
    private val autoRefreshHandler = Handler(Looper.getMainLooper())
    private val autoRefreshIntervalMs = 20_000L
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            requestPermissionsAndScan()
            autoRefreshHandler.postDelayed(this, autoRefreshIntervalMs)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startScan()
        } else {
            showEmptyState("Permissão de localização é necessária para listar redes Wi-Fi.")
        }
    }

    private val scanResultsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            handleScanResults(success)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        adapter = WifiNetworkAdapter { network ->
            val detailIntent = Intent(this, NetworkDetailActivity::class.java).apply {
                putExtra(NetworkDetailActivity.EXTRA_BSSID, network.bssid)
                putExtra(NetworkDetailActivity.EXTRA_SSID, network.ssid)
            }
            startActivity(detailIntent)
        }
        binding.recyclerNetworks.layoutManager = LinearLayoutManager(this)
        binding.recyclerNetworks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { requestPermissionsAndScan() }
        binding.fabScan.setOnClickListener { requestPermissionsAndScan() }

        requestPermissionsAndScan()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(scanResultsReceiver, filter)
        autoRefreshHandler.postDelayed(autoRefreshRunnable, autoRefreshIntervalMs)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(scanResultsReceiver)
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
    }

    private fun requestPermissionsAndScan() {
        val required = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startScan() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startScan() {
        binding.swipeRefresh.isRefreshing = true
        if (!wifiManager.isWifiEnabled) {
            showEmptyState("Ative o Wi-Fi para escanear redes por perto.")
            binding.swipeRefresh.isRefreshing = false
            return
        }
        val started = wifiManager.startScan()
        if (!started) {
            // Alguns dispositivos limitam a frequência de scans; usa o último resultado disponível
            handleScanResults(true)
        }
    }

    private fun handleScanResults(success: Boolean) {
        binding.swipeRefresh.isRefreshing = false
        if (!success) {
            showEmptyState("Não foi possível atualizar o scan. Puxe para tentar novamente.")
            return
        }
        try {
            val results = wifiManager.scanResults
            val networks = results.map { result ->
                WifiNetworkInfo(
                    ssid = result.SSID,
                    bssid = result.BSSID,
                    rssiDbm = result.level,
                    frequencyMhz = result.frequency,
                    capabilities = result.capabilities,
                    signalLevel = WifiManager.calculateSignalLevel(result.level, 5)
                )
            }.sortedByDescending { it.rssiDbm }

            if (networks.isEmpty()) {
                showEmptyState("Nenhuma rede encontrada nas proximidades.")
            } else {
                binding.textEmpty.visibility = android.view.View.GONE
                binding.recyclerNetworks.visibility = android.view.View.VISIBLE
                adapter.submitList(networks)
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                supportActionBar?.subtitle = "${networks.size} redes • atualizado às $time"
            }
        } catch (e: SecurityException) {
            showEmptyState("Permissão de localização negada.")
        }
    }

    private fun showEmptyState(message: String) {
        binding.textEmpty.text = message
        binding.textEmpty.visibility = android.view.View.VISIBLE
        binding.recyclerNetworks.visibility = android.view.View.GONE
    }
}
