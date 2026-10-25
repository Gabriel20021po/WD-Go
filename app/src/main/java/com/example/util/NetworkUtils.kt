package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

data class NetworkEndpoint(
    val name: String,
    val ip: String,
    val type: String,
    val description: String
)

object NetworkUtils {

    fun getAllAvailableEndpoints(context: Context): List<NetworkEndpoint> {
        val list = mutableListOf<NetworkEndpoint>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        val name = intf.name.lowercase()
                        val (type, desc) = when {
                            name.contains("wlan") || name.contains("eth") -> {
                                Pair("Wi-Fi", "Rede Wi-Fi Local")
                            }
                            name.contains("ap") || name.contains("softap") -> {
                                Pair("Roteador do Celular", "Ponto de Acesso / Hotspot")
                            }
                            name.contains("rndis") || name.contains("usb") -> {
                                Pair("Cabo USB", "Ancoragem via Cabo USB")
                            }
                            name.contains("rmnet") || name.contains("ccmni") || name.contains("pdp") -> {
                                Pair("Dados Móveis", "Internet 4G / 5G do Celular")
                            }
                            name.contains("tun") -> {
                                Pair("VPN", "Conexão Segura VPN")
                            }
                            else -> Pair("Rede", "Interface ${intf.displayName}")
                        }
                        list.add(NetworkEndpoint(name = intf.displayName, ip = ip, type = type, description = desc))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (list.isEmpty()) {
            list.add(NetworkEndpoint(name = "Local", ip = "127.0.0.1", type = "Loopback", description = "Apenas no dispositivo"))
        }

        return list
    }

    fun getLocalIpAddress(context: Context): String {
        val endpoints = getAllAvailableEndpoints(context)
        // Prioritize Hotspot or Wi-Fi, then cellular, then any non-loopback
        val preferred = endpoints.firstOrNull { it.type == "Roteador do Celular" || it.type == "Wi-Fi" }
            ?: endpoints.firstOrNull { it.type == "Dados Móveis" }
            ?: endpoints.firstOrNull { it.ip != "127.0.0.1" }
            ?: endpoints.firstOrNull()

        return preferred?.ip ?: "127.0.0.1"
    }

    fun isHotspotOrMobileActive(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (e: Exception) {
            false
        }
    }
}
