package com.bunker.urltest

data class CheckResult(
    val id: String,
    val title: String,
    val latencyMs: Long?,
    val httpCode: Integer?
) {
    val available: Boolean
        get() = httpCode != null

    val label: String
        get() = latencyMs?.let { "$it ms" } ?: "НЕТ ДОСТУПА"
}
