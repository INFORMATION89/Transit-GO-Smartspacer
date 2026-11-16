package dev.marco.smartspacer.transitgo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Picture
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.view.isVisible

/**
 * Inflates Transit's GO RemoteViews in our process and pulls out:
 *  - the route chip (e.g. the green "57-A" pill, view id "floatingServiceName")
 *  - the step icon at the top left (view id "image": clock while waiting, walk, ride...)
 *  - the arrival time, time left and the small line above the title
 */
object ChipRenderer {
    private const val TAG = "TransitGo"
    private const val SCALE = 3f // draw larger so the route text stays sharp

    data class Rendered(
        val chip: Bitmap?,
        val routeName: String?,
        val stepIcon: Bitmap?,
        /** Arrival time only, e.g. "6:12 AM", from Transit's "Arrive at 6:12 AM" label */
        val arrival: String?,
        /** Time left in the trip, e.g. "4 h 00 min", from Transit's bottom-right label */
        val timeLeft: String?,
        /**
         * The small line above the title, e.g. "Walk to Central Station" on a bike-station step,
         * where the title is just the number of bikes and there's no next step
         */
        val previous: String?
    )

    // A clock time inside a label in any language: "6:12", "18:05", "6:12 AM", "6:12 p.m.". The
    // space before AM/PM can be a regular, non-breaking or narrow no-break space (Android 14+).
    private val TIME = Regex("""\d{1,2}:\d{2}([\s  ]*[aApP]\.?[\s  ]?[mM]\.?)?""")

    /** Transit's view ids by name; looking them up by name is slow, so it's done once each */
    private val ids = HashMap<String, Int>()

    fun render(context: Context, rv: RemoteViews?): Rendered? {
        if (rv == null) return null
        return try {
            val root = rv.apply(context, FrameLayout(context))
            root.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)

            // isShown() needs a window, so check visibility directly (detached view)
            val chipView = (root.findByName("floatingServiceName") ?: root.findByName("floatingServiceNameContainer"))
                ?.takeIf { it.isVisible && it.width > 0 }
            val routeName = (chipView?.findByName("text") as? TextView)?.text?.toString()

            val stepView = (root.findByName("image") as? ImageView)
                ?.takeIf { it.isVisible && it.width > 0 && it.drawable != null }

            Rendered(
                chip = chipView?.let(::drawSquare),
                routeName = routeName,
                stepIcon = stepView?.let(::drawSquare),
                // Bottom-left label ("Arrive at 6:12 AM"): keep just the time
                arrival = root.visibleText("leftTextView")?.let { TIME.find(it)?.value?.trim() },
                timeLeft = root.visibleText("rightTextView"),
                previous = root.visibleText("previous_instruction")
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Couldn't render Transit's layout", t)
            null
        }
    }

    /** Trimmed text of a visible TextView, or null if it's hidden or empty. */
    private fun View.visibleText(name: String): String? = (findByName(name) as? TextView)
        ?.takeIf { it.isVisible }?.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    // Transit's views only exist as names in its own package, so they can't be referenced by R.id
    @SuppressLint("DiscouragedApi")
    private fun View.findByName(name: String): View? {
        val id = ids.getOrPut(name) { resources.getIdentifier(name, "id", TransitGoListener.TRANSIT_PACKAGE) }
        return if (id == 0) null else findViewById(id)
    }

    // Square canvas with the view centered: Smartspacer squeezes icons into a square,
    // so a tight (wide) bitmap gets distorted.
    private fun drawSquare(v: View): Bitmap {
        val w = (v.width * SCALE).toInt()
        val h = (v.height * SCALE).toInt()
        val size = maxOf(w, h)
        val picture = Picture()
        picture.beginRecording(size, size).apply {
            translate((size - w) / 2f, (size - h) / 2f)
            scale(SCALE, SCALE)
            v.draw(this)
        }
        picture.endRecording()
        // Picture-backed bitmaps are hardware; copy to software so it can be sent over Binder
        return Bitmap.createBitmap(picture).copy(Bitmap.Config.ARGB_8888, false)
    }
}
