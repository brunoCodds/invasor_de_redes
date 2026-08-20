package com.example.invasor_de_redes.util

/**
 * Interpreta a string `capabilities` do ScanResult (ex: "[WPA2-PSK-CCMP][ESS]")
 * em dados estruturados. Baseado apenas no que o Access Point anuncia
 * publicamente no beacon/probe response — não requer credenciais.
 */
data class SecurityInfo(
    val label: String,                 // Ex: "WPA2-Personal", "WPA2/WPA3-Personal (transição)"
    val keyManagement: List<String>,   // PSK, SAE, EAP, EAP-SUITE-B, OWE, Open
    val ciphers: List<String>,         // TKIP, CCMP, GCMP
    val pmf: String,                   // "Obrigatório", "Suportado", "Não anunciado"
    val wpsAdvertised: Boolean,
    val isOpen: Boolean,
    val isEnterprise: Boolean,
    val rawCapabilities: String
)

object SecurityParser {

    fun parse(capabilities: String): SecurityInfo {
        val caps = capabilities.uppercase()

        val hasWep = caps.contains("WEP")
        val hasWpa1 = caps.contains("WPA-")
        val hasWpa2 = caps.contains("WPA2") || (caps.contains("RSN") && !caps.contains("WPA3"))
        val hasWpa3 = caps.contains("WPA3") || caps.contains("SAE")
        val hasOwe = caps.contains("OWE")
        val hasEap = caps.contains("EAP")
        val hasPsk = caps.contains("PSK")
        val hasSuiteB = caps.contains("SUITE-B") || caps.contains("SUITE_B")

        val keyMgmt = mutableListOf<String>()
        if (hasPsk) keyMgmt.add("PSK")
        if (hasWpa3 || caps.contains("SAE")) keyMgmt.add("SAE")
        if (hasEap) keyMgmt.add(if (hasSuiteB) "EAP-SUITE-B" else "EAP/802.1X")
        if (hasOwe) keyMgmt.add("OWE")
        if (keyMgmt.isEmpty() && !hasWep) keyMgmt.add("Aberto")

        val ciphers = mutableListOf<String>()
        if (caps.contains("TKIP")) ciphers.add("TKIP")
        if (caps.contains("CCMP")) ciphers.add("CCMP/AES")
        if (caps.contains("GCMP")) ciphers.add("GCMP")

        val pmf = when {
            caps.contains("MFPR") -> "Obrigatório (PMF/802.11w required)"
            caps.contains("MFPC") -> "Suportado (PMF/802.11w capable)"
            else -> "Não anunciado nas capabilities"
        }

        val wps = caps.contains("WPS")
        val isOpen = !hasWep && !hasWpa1 && !hasWpa2 && !hasWpa3 && !hasOwe && !hasEap
        val isEnterprise = hasEap

        val label = when {
            isOpen -> "Rede aberta (sem criptografia)"
            hasWep -> "WEP (obsoleto e inseguro)"
            hasWpa3 && hasWpa2 && hasEap -> "WPA2/WPA3-Enterprise (transição)"
            hasWpa3 && hasEap -> "WPA3-Enterprise"
            hasWpa2 && hasEap -> "WPA2-Enterprise"
            hasWpa1 && hasEap -> "WPA-Enterprise"
            hasWpa3 && hasWpa2 && hasPsk -> "WPA2/WPA3-Personal (transição)"
            hasWpa3 && hasPsk -> "WPA3-Personal (SAE)"
            hasWpa2 && hasWpa1 && hasPsk -> "WPA/WPA2-Personal (transição)"
            hasWpa2 && hasPsk -> "WPA2-Personal (PSK)"
            hasWpa1 && hasPsk -> "WPA-Personal (PSK)"
            hasOwe -> "OWE (Enhanced Open)"
            else -> "Segurança não identificada"
        }

        return SecurityInfo(
            label = label,
            keyManagement = keyMgmt,
            ciphers = ciphers,
            pmf = pmf,
            wpsAdvertised = wps,
            isOpen = isOpen,
            isEnterprise = isEnterprise,
            rawCapabilities = capabilities
        )
    }
}
