package com.harrwama.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import java.io.ByteArrayOutputStream

const val MAX_PRODUCTS = 10

data class HomeContent(val titre: String, val texte: String)

data class ServiceItem(
    val domaine: String,
    val titre: String,
    val description: String,
    val image: String // chemin relatif sur le site, ex: assets/services/s-123.jpg ("" = aucune image)
)

data class ProductItem(
    val nom: String,
    val categorie: String,
    val prix: String,
    val description: String,
    val image: String
)

object ImageCache {
    private val cache = LruCache<String, ImageBitmap>(80)
    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }
}

object ImageUtil {

    /** Lit l'image choisie, la corrige (rotation EXIF), la réduit (1200 px max) et la compresse en JPEG. */
    fun prepareJpeg(context: Context, uri: Uri, maxSide: Int = 1200): ByteArray {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ApiException("Image illisible.")

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) {
            sample *= 2
        }
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw ApiException("Image illisible.")

        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            resolver.openInputStream(uri)?.use {
                orientation = ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            // pas d'EXIF : on ignore
        }

        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        }
        val longest = maxOf(decoded.width, decoded.height)
        if (longest > maxSide) {
            val f = maxSide.toFloat() / longest.toFloat()
            m.postScale(f, f)
        }
        var bmp: Bitmap = decoded
        if (!m.isIdentity) {
            bmp = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        }

        // Fond blanc (les PNG transparents deviendraient noirs en JPEG)
        val flat = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flat)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bmp, 0f, 0f, null)

        val out = ByteArrayOutputStream()
        flat.compress(Bitmap.CompressFormat.JPEG, 82, out)
        return out.toByteArray()
    }

    /** Décode des octets d'image en miniature (côté max donné). */
    fun decodeSampled(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) {
            sample *= 2
        }
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
