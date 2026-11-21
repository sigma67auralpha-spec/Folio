package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

object PdfPageHelper {

    fun getPageCount(file: File): Int {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { it.pageCount }
            }
        }.getOrDefault(1)
    }

    fun renderPage(
        file: File,
        pageIndex: Int,
        targetWidth: Int,
        cropMargins: Boolean,
        darkInvert: Boolean
    ): Bitmap? {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return null
                    val safeIdx = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safeIdx).use { page ->
                        val width = targetWidth.coerceIn(600, 1800)
                        val height = (width.toFloat() * page.height / page.width.coerceAtLeast(1))
                            .toInt().coerceAtLeast(1)
                        val raw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        raw.eraseColor(android.graphics.Color.WHITE)
                        page.render(raw, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val cropped = if (cropMargins) cropWhiteMargins(raw) else raw
                        if (cropped !== raw) raw.recycle()

                        if (darkInvert) {
                            val inverted = invertBitmap(cropped)
                            cropped.recycle()
                            inverted
                        } else {
                            cropped
                        }
                    }
                }
            }
        }.getOrNull()
    }

    private fun cropWhiteMargins(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        var minX = w
        var maxX = -1
        var minY = h
        var maxY = -1
        val step = 3

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val c = pixels[y * w + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                if (r < 245 || g < 245 || b < 245) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                x += step
            }
            y += step
        }

        if (maxX <= minX || maxY <= minY) return source
        val pad = (w * 0.03f).toInt()
        val left = (minX - pad).coerceAtLeast(0)
        val top = (minY - pad).coerceAtLeast(0)
        val right = (maxX + pad).coerceAtMost(w)
        val bottom = (maxY + pad).coerceAtMost(h)
        val cropW = (right - left).coerceAtLeast(1)
        val cropH = (bottom - top).coerceAtLeast(1)
        if (cropW > w * 0.94f && cropH > h * 0.94f) return source
        return Bitmap.createBitmap(source, left, top, cropW, cropH)
    }

    private fun invertBitmap(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val invertMatrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(invertMatrix) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
