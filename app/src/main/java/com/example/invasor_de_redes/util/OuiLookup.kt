package com.example.invasor_de_redes.util

/**
 * Identifica o fabricante do Access Point pelos 3 primeiros octetos do BSSID (OUI).
 *
 * IMPORTANTE: esta é uma tabela LOCAL e PARCIAL com fabricantes comuns de
 * roteadores/APs — não é o banco oficial completo da IEEE. Para uma base
 * completa e sempre atualizada, baixe o CSV público da IEEE em
 * https://standards-oui.ieee.org/oui/oui.txt e carregue como asset,
 * substituindo o mapa abaixo por uma leitura desse arquivo.
 */
object OuiLookup {

    // OUI (3 primeiros octetos, maiúsculo, separados por ':') -> fabricante
    private val knownVendors = mapOf(
        "3C:15:C2" to "Apple",
        "AC:DE:48" to "Apple",
        "F0:18:98" to "Apple",
        "00:1A:11" to "Google",
        "F4:F5:D8" to "Google",
        "DC:A6:32" to "Raspberry Pi Foundation",
        "B8:27:EB" to "Raspberry Pi Foundation",
        "00:14:6C" to "Netgear",
        "A0:40:A0" to "Netgear",
        "00:1D:7E" to "Cisco / Linksys",
        "00:0C:41" to "Cisco / Linksys",
        "C0:56:27" to "TP-Link",
        "50:C7:BF" to "TP-Link",
        "F4:F2:6D" to "TP-Link",
        "1C:61:B4" to "D-Link",
        "00:1B:11" to "D-Link",
        "04:D6:AA" to "Ubiquiti Networks",
        "24:A4:3C" to "Ubiquiti Networks",
        "78:8A:20" to "Ubiquiti Networks",
        "00:1E:42" to "Sagemcom",
        "34:31:C4" to "Sagemcom",
        "9C:97:26" to "Technicolor",
        "00:26:5A" to "Technicolor",
        "38:10:D5" to "Huawei",
        "00:E0:FC" to "Huawei",
        "20:F3:A3" to "Huawei",
        "9C:28:41" to "Xiaomi",
        "78:11:DC" to "Xiaomi",
        "64:09:80" to "Intelbras",
        "E4:57:40" to "Intelbras",
        "D4:6E:0E" to "Multilaser",
        "9C:5C:8E" to "Askey",
        "60:E3:27" to "Askey",
        "00:1F:33" to "Netgear",
        "44:94:FC" to "MikroTik",
        "6C:3B:6B" to "MikroTik",
        "00:11:32" to "Synology"
    )

    fun vendorFor(bssid: String): String {
        val oui = normalizeOui(bssid) ?: return "Formato de BSSID inválido"
        return knownVendors[oui] ?: "Fabricante não identificado (OUI local: $oui)"
    }

    private fun normalizeOui(bssid: String): String? {
        val parts = bssid.split(":", "-")
        if (parts.size < 3) return null
        return parts.take(3).joinToString(":") { it.uppercase() }
    }
}
