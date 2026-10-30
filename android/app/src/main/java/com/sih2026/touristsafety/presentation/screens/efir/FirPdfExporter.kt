package com.sih2026.touristsafety.presentation.screens.efir

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import kotlin.math.ceil
import kotlin.math.max

object FirPdfExporter {
    private const val TAG = "FirPdfExporter"

    fun exportHtmlToPdf(
        context: Context,
        htmlContent: String,
        baseFileName: String,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            try {
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = true
                    settings.textZoom = 100
                }

                val a4WidthPx = 794
                webView.layout(0, 0, a4WidthPx, 1123)

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Allow DOM layout and images to settle
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Query exact document scroll height via Javascript
                            webView.evaluateJavascript(
                                "(function() { return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, document.body.offsetHeight); })();"
                            ) { result ->
                                val parsedHeight = result?.replace("\"", "")?.toDoubleOrNull()?.toInt() ?: 0
                                renderMultiPagePdf(
                                    context = context,
                                    webView = webView,
                                    jsHeight = parsedHeight,
                                    baseFileName = baseFileName,
                                    onSuccess = onSuccess,
                                    onError = onError
                                )
                            }
                        }, 500)
                    }
                }

                webView.loadDataWithBaseURL("https://police.gov.in", htmlContent, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to initialize WebView for PDF export")
            }
        }
    }

    private fun renderMultiPagePdf(
        context: Context,
        webView: WebView,
        jsHeight: Int,
        baseFileName: String,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val pageWidthPt = 595   // Standard A4 width in PostScript points (72 DPI)
            val pageHeightPt = 842  // Standard A4 height in PostScript points (72 DPI)
            val a4WidthPx = 794     // Standard A4 width in 96 DPI CSS pixels

            // Determine actual full height of document
            val density = context.resources.displayMetrics.density
            val contentHeightPx = (webView.contentHeight * density).toInt()
            val fullHeightPx = max(1123, max(jsHeight, contentHeightPx))

            // Re-measure and layout WebView to the FULL content height
            // With enableSlowWholeDocumentDraw enabled in Application, the entire document is drawn
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(a4WidthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(fullHeightPx, View.MeasureSpec.EXACTLY)
            )
            webView.layout(0, 0, a4WidthPx, fullHeightPx)

            val scale = pageWidthPt.toFloat() / a4WidthPx.toFloat()
            // Height of one A4 page in WebView pixels (approx 1123.6 px)
            val pageHeightInWebViewPx = (pageHeightPt / scale).toInt()
            val totalPages = max(1, ceil(fullHeightPx.toDouble() / pageHeightInWebViewPx).toInt())

            Log.d(TAG, "Generating e-FIR PDF: fullHeightPx=$fullHeightPx, pageHeightPx=$pageHeightInWebViewPx, totalPages=$totalPages")

            val pdfDocument = PdfDocument()

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                canvas.save()
                // 1. Scale down from 794px width to 595pt A4 width
                canvas.scale(scale, scale)
                // 2. Translate in WebView pixels: page 0 starts at 0, page 1 starts at 1123, etc.
                canvas.translate(0f, -(pageIndex * pageHeightInWebViewPx).toFloat())
                webView.draw(canvas)
                canvas.restore()

                pdfDocument.finishPage(page)
            }

            val fileNameWithExt = "${baseFileName}_${System.currentTimeMillis()}.pdf"
            val tempFile = File(context.cacheDir, fileNameWithExt)
            if (tempFile.exists()) tempFile.delete()

            tempFile.outputStream().use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            val savedUri = saveToDownloads(context, tempFile, fileNameWithExt)
            if (savedUri != null) {
                Log.d(TAG, "e-FIR PDF successfully written: $savedUri ($totalPages page(s))")
                onSuccess(savedUri)
            } else {
                onError("Failed to save PDF to Downloads storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF: ${e.message}", e)
            onError(e.message ?: "Failed to generate e-FIR PDF")
        }
    }

    fun printOfficialFir(context: Context, htmlContent: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                val webView = WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                        val printAdapter = webView.createPrintDocumentAdapter("e-FIR_Official_Document")
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        printManager?.print("Official e-FIR Document", printAdapter, printAttributes)
                    }
                }
                webView.loadDataWithBaseURL("https://police.gov.in", htmlContent, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveToDownloads(context: Context, sourceFile: File, displayName: String): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return null

            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openPdfViewer(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open e-FIR Document"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
