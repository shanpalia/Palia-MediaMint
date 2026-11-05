package com.palia.mediamint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ProcessedMedia(val fileName: String, val mimeType: String, val bytes: ByteArray)

object MediaProcessing {
    fun process(context: Context, uri: Uri, toolTitle: String): ProcessedMedia {
        return when (toolTitle) {
            "Image to PDF" -> imagesToPdf(context, listOf(uri))
            "PDF to Image" -> pdfToImagesZip(context, uri)
            "JPG to PNG" -> convertImage(context, uri, true)
            "PNG to JPG" -> convertImage(context, uri, false)
            "Image Compressor" -> compressImage(context, uri)
            "Image Resize" -> resizeImage(context, uri, 1280)
            "Video to Image" -> videoToImage(context, uri)
            "Video to MP3", "Video to Audio" -> videoToAudio(context, uri)
            else -> throw IllegalArgumentException("This tool is not available offline yet.")
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open selected image.")
        return input.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Selected file is not a supported image.")
    }

    private fun imagesToPdf(context: Context, uris: List<Uri>): ProcessedMedia {
        require(uris.isNotEmpty()) { "Select at least one image." }
        val document = PdfDocument()
        try {
            uris.forEachIndexed { index, uri ->
                val bitmap = decode(context, uri)
                try {
                    val maxDimension = 2400f
                    val scale = minOf(1f, maxDimension / maxOf(bitmap.width, bitmap.height))
                    val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
                    val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, index + 1).create()
                    val page = document.startPage(pageInfo)
                    val drawBitmap = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, width, height, true) else bitmap
                    page.canvas.drawBitmap(drawBitmap, 0f, 0f, null)
                    document.finishPage(page)
                    if (drawBitmap !== bitmap) drawBitmap.recycle()
                } finally {
                    bitmap.recycle()
                }
            }
            val output = ByteArrayOutputStream()
            document.writeTo(output)
            return ProcessedMedia("Palia_Images.pdf", "application/pdf", output.toByteArray())
        } finally {
            document.close()
        }
    }

    private fun convertImage(context: Context, uri: Uri, png: Boolean): ProcessedMedia {
        val bitmap = decode(context, uri)
        val output = ByteArrayOutputStream()
        try {
            bitmap.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 92, output)
        } finally {
            bitmap.recycle()
        }
        return if (png) ProcessedMedia("Palia_Converted.png", "image/png", output.toByteArray())
        else ProcessedMedia("Palia_Converted.jpg", "image/jpeg", output.toByteArray())
    }

    private fun compressImage(context: Context, uri: Uri): ProcessedMedia = encodeResized(context, uri, 1920, 78)
    private fun resizeImage(context: Context, uri: Uri, maxSide: Int): ProcessedMedia = encodeResized(context, uri, maxSide, 90)

    private fun encodeResized(context: Context, uri: Uri, maxSide: Int, quality: Int): ProcessedMedia {
        val bitmap = decode(context, uri)
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, width, height, true) else bitmap
        val output = ByteArrayOutputStream()
        try {
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
        } finally {
            if (resized !== bitmap) resized.recycle()
            bitmap.recycle()
        }
        return ProcessedMedia("Palia_Image.jpg", "image/jpeg", output.toByteArray())
    }

    private fun pdfToImagesZip(context: Context, uri: Uri): ProcessedMedia {
        val descriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Cannot open selected PDF.")
        val output = ByteArrayOutputStream()
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                require(renderer.pageCount > 0) { "PDF has no pages." }
                ZipOutputStream(output).use { zip ->
                    for (index in 0 until renderer.pageCount) {
                        renderer.openPage(index).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            try {
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val image = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, image)
                                zip.putNextEntry(ZipEntry("page_${index + 1}.png"))
                                zip.write(image.toByteArray())
                                zip.closeEntry()
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        }
        return ProcessedMedia("Palia_PDF_Pages.zip", "application/zip", output.toByteArray())
    }

    private fun videoToImage(context: Context, uri: Uri): ProcessedMedia {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: throw IllegalArgumentException("Could not extract a frame from this video.")
            val output = ByteArrayOutputStream()
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            } finally {
                bitmap.recycle()
            }
            return ProcessedMedia("Palia_Video_Frame.jpg", "image/jpeg", output.toByteArray())
        } finally {
            retriever.release()
        }
    }

    // Android's built-in muxer exports the original audio stream as M4A/AAC, not MP3.
    private fun videoToAudio(context: Context, uri: Uri): ProcessedMedia {
        val extractor = MediaExtractor()
        val temp = File.createTempFile("palia_audio_", ".m4a", context.cacheDir)
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrack = -1
            var sourceFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    sourceFormat = format
                    break
                }
            }
            require(audioTrack >= 0 && sourceFormat != null) { "No audio track found in this video." }
            extractor.selectTrack(audioTrack)
            muxer = MediaMuxer(temp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outputTrack = muxer.addTrack(sourceFormat)
            muxer.start()
            val buffer = java.nio.ByteBuffer.allocate(1024 * 1024)
            val info = android.media.MediaCodec.BufferInfo()
            while (true) {
                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(outputTrack, buffer, info)
                if (!extractor.advance()) break
            }
            muxer.stop()
            muxer.release()
            muxer = null
            return ProcessedMedia("Palia_Audio.m4a", "audio/mp4", temp.readBytes())
        } finally {
            try { muxer?.release() } catch (_: Exception) { }
            extractor.release()
            temp.delete()
        }
    }
}
