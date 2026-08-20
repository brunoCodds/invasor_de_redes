package com.example.invasor_de_redes.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.invasor_de_redes.databinding.ItemWifiNetworkBinding
import com.example.invasor_de_redes.model.WifiNetworkInfo

class WifiNetworkAdapter(
    private val onItemClick: (WifiNetworkInfo) -> Unit
) : ListAdapter<WifiNetworkInfo, WifiNetworkAdapter.NetworkViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkViewHolder {
        val binding = ItemWifiNetworkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NetworkViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: NetworkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NetworkViewHolder(
        private val binding: ItemWifiNetworkBinding,
        private val onItemClick: (WifiNetworkInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(network: WifiNetworkInfo) {
            binding.textSsid.text = network.ssid.ifBlank { "(SSID oculto)" }
            binding.textDetails.text = buildString {
                append("Canal ${network.channel}")
                append(" • ${network.frequencyMhz} MHz")
                append(" • ${network.rssiDbm} dBm")
            }
            binding.textSecurity.text = network.securityType
            binding.iconLock.setImageResource(
                if (network.isOpen) android.R.drawable.ic_lock_idle_alarm
                else android.R.drawable.ic_lock_lock
            )
            binding.signalBars.progress = network.signalLevel // 0..4
            binding.root.setOnClickListener { onItemClick(network) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WifiNetworkInfo>() {
            override fun areItemsTheSame(old: WifiNetworkInfo, new: WifiNetworkInfo) =
                old.bssid == new.bssid

            override fun areContentsTheSame(old: WifiNetworkInfo, new: WifiNetworkInfo) =
                old == new
        }
    }
}
