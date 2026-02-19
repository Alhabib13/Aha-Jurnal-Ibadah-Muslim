package com.islami.Aha.ui.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

internal fun createGoogleSignInClient(context: Context): GoogleSignInClient? {
    val webClientId = resolveGoogleWebClientId(context) ?: return null
    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    return GoogleSignIn.getClient(context, options)
}

private fun resolveGoogleWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    if (resId == 0) return null
    val value = context.getString(resId).trim()
    if (value.isBlank()) return null
    if (value.equals("default_web_client_id", ignoreCase = true)) return null
    return value
}
