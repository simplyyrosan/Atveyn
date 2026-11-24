package com.atveyn.app.feature.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfRendererHelper {

    fun renderPdfToBitmaps(context: Context, pdfUri: Uri, maxPages: Int = 3): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null

        try {
            // Copy uri stream to a cache file
            val tempFile = File(context.cacheDir, "temp_render_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            parcelFileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor)

            val pageCount = minOf(pdfRenderer.pageCount, maxPages)
            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                // Render at 2x scale for high OCR accuracy
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                // Fill with white background
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
            tempFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return bitmaps
    }
}
