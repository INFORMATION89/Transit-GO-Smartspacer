package dev.marco.smartspacer.transitgo

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider

/** The live GO trip, as read from Transit's notification. */
data class GoState(
    /** Transit's instruction, e.g. "Leave in 5 minutes" */
    val title: String,
    /** The next step, e.g. "in 16 minutes" */
    val text: String?,
    val contentIntent: PendingIntent?,
    /** Transit's route chip (e.g. green "57-A"), or a bike/other chip, drawn on a square */
    val chip: Bitmap? = null,
    val routeName: String? = null,
    /** Transit's icon for the current step (wait, walk, ride, bike...) */
    val stepIcon: Bitmap? = null,
    /** Arrival time, e.g. "6:44 AM" */
    val arrival: String? = null,
    /** Time left in the trip, e.g. "3 h 07 min" */
    val timeLeft: String? = null,
    /** Transit's small line above the title, e.g. "Walk to Central Station" on bike-station steps */
    val previous: String? = null,
    /** Key of Transit's GO notification, so Native Smartspace can hide it on the lock screen */
    val notificationKey: String? = null
)

object GoStore {
    /** The live trip, or null. Compose state, so the settings preview follows it live. */
    var state by mutableStateOf<GoState?>(null)
}

/**
 * The plugin's notification listener. Android hands a listener every app's notifications and
 * can't be told to limit them to one app, so this drops anything that isn't Transit's GO channel
 * immediately, looking only at the package name and channel, before reading any content.
 *
 * (Smartspacer's own listener, via a SmartspacerNotificationProvider like the Uber plugin uses,
 * was tried first, but Smartspacer didn't deliver Transit's notifications to it.)
 */
class TransitGoListener : NotificationListenerService() {

    override fun onListenerConnected() {
        val active = activeNotifications.orEmpty()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Counts only, no content
            Log.d("TransitGo", "listener connected: delivered=${active.size} " +
                "transit=${active.count { it.packageName == TRANSIT_PACKAGE }} go=${active.count { isTransitGo(it) }}")
        }
        // Pick up a trip that was already running before we connected
        active.forEach { if (isTransitGo(it)) TripReader.onPosted(this, it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isTransitGo(sbn)) TripReader.onPosted(this, sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Only the key is compared: nothing else about other apps' notifications is read
        TripReader.onRemoved(this, sbn.key)
    }

    // Notification access turned off (or the system unbinding us): we'd stop hearing about
    // the trip, so don't leave its last state stuck on the card
    override fun onListenerDisconnected() = TripReader.onLost(this)

    private fun isTransitGo(sbn: StatusBarNotification) =
        sbn.packageName == TRANSIT_PACKAGE && sbn.notification.channelId == GO_CHANNEL

    companion object {
        const val TRANSIT_PACKAGE = "com.thetransitapp.droid"
        const val GO_CHANNEL = "go_mode"
    }
}

/** Turns Transit's notifications into the [GoState] the card shows. */
// bigContentView is deprecated for apps building notifications, but it's the only way to
// read another app's custom expanded layout
@Suppress("DEPRECATION")
object TripReader {
    private const val TAG = "TransitGo"

    /** Debug: last layout "shape" dumped, so each new kind of step (walk, bike...) gets dumped once */
    private var lastDumped: String? = null
    private var dumpCount = 0

    /** A Transit GO-channel notification was posted or updated. */
    fun onPosted(context: Context, sbn: StatusBarNotification) {
        val isDebug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebug) logGoNotification(sbn)
        if (isLiveTrip(sbn)) update(context, sbn, isDebug)
    }

    /** Some notification was removed: clear the card if it was the live trip's. */
    fun onRemoved(context: Context, key: String) {
        if (key == GoStore.state?.notificationKey) clearTrip(context)
    }

    /** We stopped receiving notifications. */
    fun onLost(context: Context) {
        if (GoStore.state != null) clearTrip(context)
    }

    /**
     * Transit posts several notifications on go_mode: an empty group summary, one-off alerts
     * (AUTO_CANCEL), and the live trip, which is the foreground-service one.
     */
    private fun isLiveTrip(sbn: StatusBarNotification): Boolean {
        val flags = sbn.notification.flags
        return flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
            flags and Notification.FLAG_FOREGROUND_SERVICE != 0
    }

    private fun clearTrip(context: Context) {
        GoStore.state = null
        SampleTrip.reroll() // the settings preview shows a fresh example from now on
        notifyTarget(context)
    }

    private fun update(context: Context, sbn: StatusBarNotification, isDebug: Boolean) {
        val n = sbn.notification
        // Transit's own text, in whatever language Transit uses (e.g. "Leave in 5 minutes").
        // Trimmed: on bike steps it's " 0 bikes", with a space where Transit draws an inline icon
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }?.let(::withBikesLeft) ?: "Transit GO"
        // Observed: text = null and subText = the next step ("in 16 minutes"); the details are
        // in the custom layout, which ChipRenderer reads
        val text = (n.extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT))?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        val r = ChipRenderer.render(context, n.bigContentView)

        if (isDebug) {
            Log.d(TAG, "update: title=$title text=$text route=${r?.routeName} chip=${r?.chip != null} " +
                "step=${r?.stepIcon != null} arrival=${r?.arrival} left=${r?.timeLeft} previous=${r?.previous}")
            // Dump the layout again whenever the kind of step changes (first word of the
            // title, e.g. "Leave" / "Walk" / "Ride", plus whether there's a route name)
            val shape = "${title.substringBefore(' ')}|${r?.routeName != null}|${r?.chip != null}"
            if (shape != lastDumped) {
                lastDumped = shape
                LayoutDumper.dump(context, n.bigContentView, "big${dumpCount++}")
            }
        }

        val new = GoState(
            title = title,
            text = text,
            contentIntent = n.contentIntent,
            chip = r?.chip,
            routeName = r?.routeName,
            stepIcon = r?.stepIcon,
            arrival = r?.arrival,
            timeLeft = r?.timeLeft,
            previous = r?.previous,
            notificationKey = sbn.key
        )
        // Transit re-posts the notification often with nothing visible changed: skip those, so
        // Smartspacer doesn't redraw the card and get sent the same icons again
        val old = GoStore.state
        if (old != null && old.looksSameAs(new)) {
            GoStore.state = old.copy(contentIntent = new.contentIntent) // keep the tap action fresh
            return
        }
        GoStore.state = new
        notifyTarget(context)
    }

    /** Transit's bike-station title, just a count: "10 bikes" / "0 bicis" */
    private val BIKES = Regex("""^(\d+)\s+(bikes?|bicis?)$""", RegexOption.IGNORE_CASE)

    /**
     * "10 bikes" -> "10 bikes left", "0 bicis" -> "0 bicis restantes". Uses the language of
     * Transit's own word, since the title is in Transit's language, not the plugin's.
     */
    fun withBikesLeft(title: String): String {
        val m = BIKES.matchEntire(title) ?: return title
        val n = m.groupValues[1].toIntOrNull() ?: return title
        val spanish = m.groupValues[2].startsWith("bici", ignoreCase = true)
        return when {
            !spanish -> "$title left"
            n == 1 -> "$title restante"
            else -> "$title restantes"
        }
    }

    /** Same text and same kind of icons (bitmaps are redrawn every time, so compare what they show). */
    private fun GoState.looksSameAs(other: GoState) =
        title == other.title && text == other.text && previous == other.previous &&
            routeName == other.routeName && arrival == other.arrival && timeLeft == other.timeLeft &&
            notificationKey == other.notificationKey &&
            (chip == null) == (other.chip == null) && (stepIcon == null) == (other.stepIcon == null)

    /** Debug: one line per GO notification, including the ones we skip (to spot bike/other variants). */
    private fun logGoNotification(sbn: StatusBarNotification) {
        val n = sbn.notification
        Log.d(TAG, "go_mode: id=${sbn.id} live=${isLiveTrip(sbn)} flags=0x${n.flags.toString(16)} " +
            "title=${n.extras.getCharSequence(Notification.EXTRA_TITLE)} " +
            "text=${n.extras.getCharSequence(Notification.EXTRA_TEXT)} " +
            "sub=${n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)} " +
            "custom=${n.bigContentView != null}")
    }

    private fun notifyTarget(context: Context) =
        SmartspacerTargetProvider.notifyChange(context, TransitGoTarget::class.java)
}
