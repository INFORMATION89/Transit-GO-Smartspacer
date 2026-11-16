package dev.marco.smartspacer.transitgo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import java.io.File

/**
 * Debug-only: inflates a notification's RemoteViews inside our process and dumps the
 * view tree (ids, text, colors) to logcat, plus PNGs of every image and the whole layout
 * to files/dump/. Pull with: adb shell run-as dev.marco.smartspacer.transitgo ...
 */
object LayoutDumper {
    private const val TAG = "TransitGoDump"

    fun dump(context: Context, rv: RemoteViews?, name: String) {
        if (rv == null) { Log.d(TAG, "$name: null"); return }
        val dir = File(context.filesDir, "dump").apply { mkdirs() }
        try {
            val parent = FrameLayout(context)
            val root = rv.apply(context, parent)
            val w = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            root.measure(w, h)
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            Log.d(TAG, "$name: ${root.measuredWidth}x${root.measuredHeight}")
            walk(root, 0, name, dir)
            save(render(root), File(dir, "$name-full.png"))
        } catch (t: Throwable) {
            Log.e(TAG, "$name: failed to inflate", t)
        }
    }

    private var counter = 0

    private fun walk(v: View, depth: Int, name: String, dir: File) {
        val pad = "  ".repeat(depth)
        val id = if (v.id != View.NO_ID) runCatching { v.resources.getResourceEntryName(v.id) }.getOrDefault("#${v.id}") else "-"
        val sb = StringBuilder("$pad${v.javaClass.simpleName} id=$id vis=${v.visibility} ${v.width}x${v.height}")
        describeDrawable(v.background)?.let { sb.append(" bg=$it") }
        v.backgroundTintList?.let { sb.append(" bgTint=${hex(it.defaultColor)}") }
        if (v is TextView) {
            sb.append(" text=\"${v.text}\" color=${hex(v.currentTextColor)}")
        }
        if (v is ImageView) {
            sb.append(" img=${describeDrawable(v.drawable)}")
            v.imageTintList?.let { sb.append(" tint=${hex(it.defaultColor)}") }
            if (v.width > 0 && v.height > 0 && v.drawable != null) {
                val f = File(dir, "$name-img${counter++}-$id.png")
                runCatching { save(render(v), f) }
                    .onSuccess { sb.append(" -> ${f.name}") }
                    .onFailure { sb.append(" (render failed: ${it.message})") }
            }
        }
        Log.d(TAG, sb.toString())
        if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i), depth + 1, name, dir)
    }

    private fun describeDrawable(d: Drawable?): String? = when (d) {
        null -> null
        is ColorDrawable -> "color(${hex(d.color)})"
        is GradientDrawable -> "gradient(${d.color?.defaultColor?.let(::hex)})"
        is BitmapDrawable -> "bitmap(${d.bitmap?.width}x${d.bitmap?.height})"
        else -> d.javaClass.simpleName
    }

    // Record into a Picture so hardware bitmaps can be drawn, then copy to a software bitmap
    private fun render(v: View): Bitmap {
        val p = android.graphics.Picture()
        v.draw(p.beginRecording(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1)))
        p.endRecording()
        return Bitmap.createBitmap(p).copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun save(b: Bitmap, f: File) = f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    private fun hex(c: Int) = String.format("#%08X", c)
}
