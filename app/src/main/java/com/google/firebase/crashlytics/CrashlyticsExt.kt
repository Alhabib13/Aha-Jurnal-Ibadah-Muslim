package com.google.firebase.crashlytics

import kotlin.coroutines.cancellation.CancellationException

fun FirebaseCrashlytics.recordExceptionSafe(e: Throwable) {
    if (e is CancellationException) return
    this.recordException(e)
}
