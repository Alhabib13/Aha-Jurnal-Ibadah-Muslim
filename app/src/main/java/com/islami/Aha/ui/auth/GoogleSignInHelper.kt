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
    val value = context.getString(com.islami.Aha.R.string.default_web_client_id).trim()
    if (value.isBlank()) return null
    if (value.equals("default_web_client_id", ignoreCase = true)) return null
    return value
}
