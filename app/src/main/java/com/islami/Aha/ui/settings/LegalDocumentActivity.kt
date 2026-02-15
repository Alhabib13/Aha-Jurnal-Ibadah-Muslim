package com.islami.Aha.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.islami.Aha.R

class LegalDocumentActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DOCUMENT_TYPE = "document_type"
        const val DOC_PRIVACY = "privacy"
        const val DOC_TERMS = "terms"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val documentType = intent.getStringExtra(EXTRA_DOCUMENT_TYPE)
        val configuredUrl = if (documentType == DOC_TERMS) {
            getString(R.string.terms_of_service_url)
        } else {
            getString(R.string.privacy_policy_url)
        }.trim()

        val contentUrl = if (configuredUrl.startsWith("http://") || configuredUrl.startsWith("https://")) {
            configuredUrl
        } else if (documentType == DOC_TERMS) {
            "file:///android_asset/terms-of-service.html"
        } else {
            "file:///android_asset/privacy-policy.html"
        }

        setContentView(
            WebView(this).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                loadUrl(contentUrl)
            }
        )
    }
}
