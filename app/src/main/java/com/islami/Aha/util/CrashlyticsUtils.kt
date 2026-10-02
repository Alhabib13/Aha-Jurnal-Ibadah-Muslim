package com.islami.Aha.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.coroutines.cancellation.CancellationException

fun logCrashlyticsSafe(e: Throwable) {
    if (e is CancellationException) return
    FirebaseCrashlytics.getInstance().recordException(e)
}
