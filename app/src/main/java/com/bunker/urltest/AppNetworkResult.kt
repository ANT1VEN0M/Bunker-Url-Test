package com.bunker.urltest

data class AppNetworkResult(
    val packageName: String,
    val appLabel: String,
    val hosts: List<String>,
    val availableHosts: List<String>,
    val averageLatencyMs: Long?,
    val isEstimated: Boolean
) {
    val isAvailable: Boolean
        get() = availableHosts.isNotEmpty()

    val statusText: String
        get() {
            val prefix = if (isEstimated) "≈ " else ""
            return if (isAvailable) {
                val ping = averageLatencyMs?.let { "$it ms" } ?: "OK"
                "$prefix$ping (${availableHosts.size}/${hosts.size})"
            } else {
                "${prefix}НЕТ ДОСТУПА"
            }
        }
}
