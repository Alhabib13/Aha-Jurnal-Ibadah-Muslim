package com.islami.Aha.ui.settings

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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

        val defaultAssetUrl = if (documentType == DOC_TERMS) {
            "file:///android_asset/terms-of-service.html"
        } else {
            "file:///android_asset/privacy-policy.html"
        }
        val contentUrl = configuredUrl.ifBlank { defaultAssetUrl }
        val isRemoteUrl = configuredUrl.startsWith("http://") || configuredUrl.startsWith("https://")

        setContentView(
            WebView(this).apply {
                var hasFallenBack = false
                webViewClient = object : WebViewClient() {
                    private fun fallbackToAsset(view: WebView?) {
                        if (!hasFallenBack && isRemoteUrl) {
                            hasFallenBack = true
                            view?.loadUrl(defaultAssetUrl)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        val isMainFrame = request?.isForMainFrame == true
                        if (isMainFrame) fallbackToAsset(view)
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        val isMainFrame = request?.isForMainFrame == true
                        if (isMainFrame && (errorResponse?.statusCode ?: 200) >= 400) {
                            fallbackToAsset(view)
                        }
                    }
                }
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                loadUrl(contentUrl)
            }
        )
    }
}
