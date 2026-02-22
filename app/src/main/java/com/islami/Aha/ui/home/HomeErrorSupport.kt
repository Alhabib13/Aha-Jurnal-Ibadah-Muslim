package com.islami.Aha.ui.home

import java.io.IOException

internal fun Throwable.isLikelyNetworkErrorForHome(): Boolean {
    if (this is IOException) return true
    val messageText = message?.lowercase().orEmpty()
    return "network" in messageText ||
        "timeout" in messageText ||
        "timed out" in messageText ||
        "unable to resolve host" in messageText ||
        "failed to connect" in messageText
}

