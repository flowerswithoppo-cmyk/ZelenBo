package com.zelenbo.app.domain.usecase

import android.util.Base64
import com.zelenbo.app.domain.model.BestEffortConfig
import com.zelenbo.app.domain.model.DnsConfig
import com.zelenbo.app.domain.model.DnsPreset
import com.zelenbo.app.domain.model.DnsTransportMode
import com.zelenbo.app.domain.model.ProxyMode
import com.zelenbo.app.domain.model.ServiceId
import com.zelenbo.app.domain.model.ZelenBoConfig
import org.json.JSONArray
import org.json.JSONObject

object ConfigCodec {

    fun encode(config: ZelenBoConfig): String {
        val root = JSONObject()
        root.put("enabledServices", JSONArray(config.enabledServices.map { it.name }))
        root.put("optimizedDomains", JSONArray(config.optimizedDomains.toList()))

        root.put("proxyMode", config.proxyMode.name)

        val dns = JSONObject()
        dns.put("transportMode", config.dns.transportMode.name)
        dns.put("preset", config.dns.preset.name)
        dns.put("customEndpoint", config.dns.customEndpoint)
        dns.put("udpFallbackServerIp", config.dns.udpFallbackServerIp)
        dns.put("udpFallbackServerPort", config.dns.udpFallbackServerPort)
        root.put("dns", dns)

        val best = JSONObject()
        best.put("tlsFragmentationEnabled", config.bestEffort.tlsFragmentationEnabled)
        best.put("tlsFragmentSizeBytes", config.bestEffort.tlsFragmentSizeBytes)
        best.put("tlsFragmentDelayMs", config.bestEffort.tlsFragmentDelayMs)
        best.put("tcpDesyncEnabled", config.bestEffort.tcpDesyncEnabled)
        best.put("tcpDesyncMode", config.bestEffort.tcpDesyncMode)
        best.put("echEnabled", config.bestEffort.echEnabled)
        root.put("bestEffort", best)

        val json = root.toString()
        return Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun decode(encoded: String): Result<ZelenBoConfig> {
        return try {
            val jsonBytes = Base64.decode(encoded, Base64.DEFAULT)
            val json = String(jsonBytes, Charsets.UTF_8)
            val root = JSONObject(json)

            val enabledServicesArray = root.getJSONArray("enabledServices")
            val enabledServices = buildSet {
                for (i in 0 until enabledServicesArray.length()) {
                    add(ServiceId.valueOf(enabledServicesArray.getString(i)))
                }
            }

            val optimizedDomainsArray = root.getJSONArray("optimizedDomains")
            val optimizedDomains = buildSet {
                for (i in 0 until optimizedDomainsArray.length()) {
                    add(optimizedDomainsArray.getString(i))
                }
            }

            val proxyMode = ProxyMode.valueOf(root.getString("proxyMode"))

            val dnsRoot = root.getJSONObject("dns")
            val transportMode = DnsTransportMode.valueOf(dnsRoot.getString("transportMode"))
            val preset = DnsPreset.valueOf(dnsRoot.getString("preset"))
            val customEndpoint = if (dnsRoot.isNull("customEndpoint")) null else dnsRoot.getString("customEndpoint")

            val dnsConfig = DnsConfig(
                transportMode = transportMode,
                preset = preset,
                customEndpoint = customEndpoint,
                udpFallbackServerIp = dnsRoot.getString("udpFallbackServerIp"),
                udpFallbackServerPort = dnsRoot.getInt("udpFallbackServerPort")
            )

            val bestRoot = root.getJSONObject("bestEffort")
            val best = BestEffortConfig(
                tlsFragmentationEnabled = bestRoot.getBoolean("tlsFragmentationEnabled"),
                tlsFragmentSizeBytes = bestRoot.getInt("tlsFragmentSizeBytes"),
                tlsFragmentDelayMs = bestRoot.getInt("tlsFragmentDelayMs"),
                tcpDesyncEnabled = bestRoot.getBoolean("tcpDesyncEnabled"),
                tcpDesyncMode = bestRoot.getString("tcpDesyncMode"),
                echEnabled = bestRoot.getBoolean("echEnabled")
            )

            Result.success(
                ZelenBoConfig(
                    enabledServices = enabledServices,
                    dns = dnsConfig,
                    bestEffort = best,
                    proxyMode = proxyMode,
                    optimizedDomains = optimizedDomains
                )
            )
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

