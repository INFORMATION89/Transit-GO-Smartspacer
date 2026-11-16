package dev.marco.smartspacer.transitgo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap

/**
 * Draws the trip timeline shown on the card (experimental): Transit's logo, a progress bar with
 * your position marker (the route chip or step icon), and the arrival time and time left below.
 * Colors come from the wallpaper on Android 12+.
 */
object TimelineRenderer {
    // Same 16:9 canvas the Uber plugin uses for Smartspacer's image card
    private const val WIDTH = 768
    private const val HEIGHT = 432
    private const val PAD = 56f
    private const val CORNERS = 40f
    private const val BAR_Y = 230f
    private const val BAR_HEIGHT = 18f
    private const val MARKER_RADIUS = 50f
    private const val MARKER_ICON = 64
    private const val LOGO = 44
    private const val TEXT_TOP = 40f
    private const val TEXT_BOTTOM = 42f

    private class Colors(val background: Int, val track: Int, val fill: Int, val marker: Int, val onMarker: Int, val text: Int)

    private fun colors(context: Context): Colors =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Colors(
            background = context.getColor(android.R.color.system_neutral1_900),
            track = context.getColor(android.R.color.system_accent1_700),
            fill = context.getColor(android.R.color.system_accent1_200),
            marker = context.getColor(android.R.color.system_accent1_100),
            onMarker = context.getColor(android.R.color.system_accent1_800),
            text = context.getColor(android.R.color.system_neutral1_50)
        ) else Colors(
            background = 0xFF1B1B1F.toInt(), track = 0xFF3F4759.toInt(), fill = 0xFFADC6FF.toInt(),
            marker = 0xFFD8E2FF.toInt(), onMarker = 0xFF1A2F5B.toInt(), text = Color.WHITE
        )

    fun draw(context: Context, state: GoState, progress: Float, marker: Bitmap?, markerIsChip: Boolean): Bitmap {
        val c = colors(context)
        val font = runCatching { ResourcesCompat.getFont(context, R.font.google_sans_text_medium) }.getOrNull()
            ?: Typeface.DEFAULT_BOLD
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; color = c.text }

        // Background card
        paint.color = c.background
        canvas.drawRoundRect(RectF(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat()), CORNERS, CORNERS, paint)

        // Top: [Transit logo] Transit
        val topY = PAD + LOGO / 2f
        TransitIcon.glyph(context)?.let { logo ->
            paint.colorFilter = PorterDuffColorFilter(c.text, PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(logo, null, Rect(PAD.toInt(), (topY - LOGO / 2f).toInt(), PAD.toInt() + LOGO, (topY + LOGO / 2f).toInt()), paint)
            paint.colorFilter = null
        }
        text.textSize = TEXT_TOP
        canvas.drawText("Transit", PAD + LOGO + 16f, topY - (text.descent() + text.ascent()) / 2f, text)

        // Bar: track, then the part already traveled
        val left = PAD
        val right = WIDTH - PAD
        val p = progress.coerceIn(0f, 1f)
        val markerX = (left + MARKER_RADIUS + (right - left - 2 * MARKER_RADIUS) * p)
        paint.color = c.track
        canvas.drawRoundRect(RectF(left, BAR_Y - BAR_HEIGHT / 2, right, BAR_Y + BAR_HEIGHT / 2), BAR_HEIGHT, BAR_HEIGHT, paint)
        paint.color = c.fill
        canvas.drawRoundRect(RectF(left, BAR_Y - BAR_HEIGHT / 2, markerX, BAR_Y + BAR_HEIGHT / 2), BAR_HEIGHT, BAR_HEIGHT, paint)

        // Marker: a ring in the card color, then the circle with the chip or step icon
        paint.color = c.background
        canvas.drawCircle(markerX, BAR_Y, MARKER_RADIUS + 8f, paint)
        paint.color = c.marker
        canvas.drawCircle(markerX, BAR_Y, MARKER_RADIUS, paint)
        marker?.let {
            val size = if (markerIsChip) (MARKER_ICON * 1.25f).toInt() else MARKER_ICON
            val half = size / 2
            if (!markerIsChip) paint.colorFilter = PorterDuffColorFilter(c.onMarker, PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(it, null, Rect(markerX.toInt() - half, BAR_Y.toInt() - half, markerX.toInt() + half, BAR_Y.toInt() + half), paint)
            paint.colorFilter = null
        }

        // Bottom: "Arrives 6:44 PM" on the left, time left on the right
        text.textSize = TEXT_BOTTOM
        val bottomY = HEIGHT - PAD
        val timeLeft = state.timeLeft.orEmpty()
        val timeLeftWidth = text.measureText(timeLeft)
        state.arrival?.let { arrival ->
            val label = context.getString(R.string.timeline_arrives, arrival)
            val maxWidth = right - left - timeLeftWidth - 32f
            canvas.drawText(TextUtils.ellipsize(label, text, maxWidth, TextUtils.TruncateAt.END).toString(), left, bottomY, text)
        }
        if (timeLeft.isNotEmpty()) canvas.drawText(timeLeft, right - timeLeftWidth, bottomY, text)
        return bitmap
    }
}
