package com.codepipes.ting.customclasses

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVGParseException
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import android.graphics.RectF
import com.caverock.androidsvg.SVG
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import java.io.IOException
import java.io.InputStream


@Suppress("NAME_SHADOWING")
class SvgBitmapDecoder(private val bitmapPool: BitmapPool) : ResourceDecoder<InputStream, Bitmap> {

    val id: String get() = javaClass.simpleName

    constructor(context: Context) : this(Glide.get(context).bitmapPool) {}

    @Throws(IOException::class)
    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        var width = width
        var height = height
        try {
            val svg = SVG.getFromInputStream(source)
            if (width == com.bumptech.glide.request.target.Target.SIZE_ORIGINAL && height == com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) {
                width = svg.documentWidth.toInt()
                height = svg.documentHeight.toInt()
                if (width <= 0 || height <= 0) {
                    val viewBox = svg.documentViewBox
                    width = viewBox.width().toInt()
                    height = viewBox.height().toInt()
                }
            } else {
                if (width == com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) {
                    width = (height * svg.documentAspectRatio).toInt()
                }
                if (height == com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) {
                    height = (width / svg.documentAspectRatio).toInt()
                }
            }
            if (width <= 0 || height <= 0) {
                throw IllegalArgumentException("Either the Target or the SVG document must declare a size.")
            }

            val bitmap = findBitmap(width, height)
            val canvas = Canvas(bitmap)
            svg.renderToCanvas(canvas)
            return BitmapResource.obtain(bitmap, bitmapPool)
        } catch (ex: SVGParseException) {
            throw IOException("Cannot load SVG from stream", ex)
        }

    }

    override fun handles(source: InputStream, options: Options): Boolean = true

    private fun findBitmap(width: Int, height: Int): Bitmap {
        var bitmap: Bitmap? = bitmapPool.get(width, height, Bitmap.Config.ARGB_8888)
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        return bitmap!!
    }
}