package com.x2tora.torabox.domain

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.x2tora.torabox.model.CarrierInfo
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.TrackingEvent
import com.x2tora.torabox.model.TrackingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Offscreen WebView fallback for JS-rendered carrier tracking pages when HTTP endpoints fail.
 * Respects CAPTCHA and login walls (never attempts to bypass them).
 */
object HiddenWebViewScraper {

    private val blockedMarkers = listOf(
        "recaptcha", "hcaptcha", "turnstile", "보안문자", "자동입력방지", "captcha", "로그인 후"
    )

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchTracking(
        context: Context,
        carrier: CarrierInfo,
        waybill: String
    ): TrackingResult? {
        val template = carrier.trackingUrlTemplate
        if (template.isBlank() || !template.contains("{waybill}")) return null
        val targetUrl = template.replace("{waybill}", waybill)

        val extractedJson = withTimeoutOrNull(12_000L) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<String?> { cont ->
                    val webView = WebView(context.applicationContext)
                    var finished = false

                    fun complete(result: String?) {
                        if (!finished) {
                            finished = true
                            runCatching {
                                webView.stopLoading()
                                webView.destroy()
                            }
                            if (cont.isActive) cont.resume(result)
                        }
                    }

                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = false
                        blockNetworkImage = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            val js = """
                                (function() {
                                    var text = document.body ? document.body.innerText : '';
                                    var rows = [];
                                    var trs = document.querySelectorAll('table tbody tr, .tracking-list li, .delivery-step li');
                                    for (var i = 0; i < trs.length && i < 30; i++) {
                                        var rowText = trs[i].innerText.replace(/\s+/g, ' ').trim();
                                        if (rowText.length > 4) rows.push(rowText);
                                    }
                                    return JSON.stringify({ body: text.slice(0, 2000), rows: rows });
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(js) { raw ->
                                complete(raw)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                complete(null)
                            }
                        }
                    }

                    cont.invokeOnCancellation {
                        runCatching {
                            webView.stopLoading()
                            webView.destroy()
                        }
                    }
                    webView.loadUrl(targetUrl)
                }
            }
        } ?: return null

        return parseEvaluatedPayload(extractedJson, carrier, waybill)
    }

    internal fun parseEvaluatedPayload(
        rawEvalResult: String,
        carrier: CarrierInfo,
        waybill: String
    ): TrackingResult? {
        val unescaped = rawEvalResult.trim()
            .removeSurrounding("\"")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")

        val json = runCatching { JSONObject(unescaped) }.getOrNull() ?: return null
        val body = json.optString("body")
        if (blockedMarkers.any { body.contains(it, ignoreCase = true) }) {
            return null
        }

        val rowsArray = json.optJSONArray("rows") ?: JSONArray()
        val events = mutableListOf<TrackingEvent>()
        val timeRegex = Regex("""(\d{4}[.\-/]\d{2}[.\-/]\d{2}(?:\s+\d{2}:\d{2}(?::\d{2})?)?)""")

        for (i in 0 until rowsArray.length()) {
            val row = rowsArray.optString(i).trim()
            if (row.length < 6) continue
            val timeMatch = timeRegex.find(row)?.groupValues?.getOrNull(1).orEmpty()
            val remainder = if (timeMatch.isNotEmpty()) row.replace(timeMatch, "").trim() else row
            val phase = inferPhaseFromText(remainder)
            events += TrackingEvent(
                timeIso = timeMatch,
                displayTime = timeMatch,
                statusName = remainder.take(28),
                phase = phase,
                carrierStageIndex = phase.stepIndex,
                location = "",
                description = remainder
            )
        }

        if (events.isEmpty()) return null
        val latest = events.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = events,
            syncSource = "WebView 자동조회"
        )
    }

    private fun inferPhaseFromText(text: String): StandardDeliveryPhase = when {
        text.contains("완료") || text.contains("전달") || text.contains("수령") -> StandardDeliveryPhase.DELIVERED
        text.contains("출발") || text.contains("배달중") || text.contains("배송중") -> StandardDeliveryPhase.OUT_FOR_DELIVERY
        text.contains("도착") || text.contains("하차") -> StandardDeliveryPhase.ARRIVED_AT_BRANCH
        text.contains("집하") || text.contains("접수") -> StandardDeliveryPhase.PICKED_UP
        text.contains("반송") || text.contains("미배달") || text.contains("예외") -> StandardDeliveryPhase.EXCEPTION
        else -> StandardDeliveryPhase.IN_TRANSIT
    }
}
