package com.palia.mediamint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class ProcessedMedia(val fileName: String, val mimeType: String, val bytes: ByteArray)

object MediaProcessing {
    fun process(context: Context, uri: Uri, toolTitle: String): ProcessedMedia {
        return when (toolTitle) {
            "Image to PDF" -> imageToPdf(context, uri)
            "PDF to Image" -> pdfToImage(context, uri)
            "JPG to PNG", "PNG to JPG", "Image Convert", "Image Tools" -> convertImage(context, uri, toolTitle)
            "Image Compressor", "Image Resize" -> compressImage(context, uri)
            else -> throw IllegalArgumentException("This tool is not available offline yet.")
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open selected image.")
        return input.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Selected file is not a supported image.")
    }

    private fun imageToPdf(context: Context, uri: Uri): ProcessedMedia {
        val bitmap = decode(context, uri)
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width.coerceAtLeast(1), bitmap.height.coerceAtLeast(1), 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
            val output = ByteArrayOutputStream()
            document.writeTo(output)
            return ProcessedMedia("Palia_Image.pdf", "application/pdf", output.toByteArray())
        } finally {
            document.close()
            bitmap.recycle()
        }
    }

    private fun convertImage(context: Context, uri: Uri, title: String): ProcessedMedia {
        val bitmap = decode(context, uri)
        val png = title == "JPG to PNG"
        val output = ByteArrayOutputStream()
        bitmap.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 90, output)
        bitmap.recycle()
        return if (png) ProcessedMedia("Palia_Converted.png", "image/png", output.toByteArray())
        else ProcessedMedia("Palia_Converted.jpg", "image/jpeg", output.toByteArray())
    }

    private fun compressImage(context: Context, uri: Uri): ProcessedMedia {
        val bitmap = decode(context, uri)
        val maxSide = 1920
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1), true
        ) else bitmap
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 78, output)
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
        return ProcessedMedia("Palia_Compressed.jpg", "image/jpeg", output.toByteArray())
    }

    private fun pdfToImage(context: Context, uri: Uri): ProcessedMedia {
        val descriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Cannot open selected PDF.")
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) throw IllegalArgumentException("PDF has no pages.")
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    bitmap.recycle()
                    return ProcessedMedia("Palia_Page_1.png", "image/png", output.toByteArray())
                }
            }
        }
    }
}
