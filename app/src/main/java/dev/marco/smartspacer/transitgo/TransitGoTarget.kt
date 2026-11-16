package dev.marco.smartspacer.transitgo

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon as AndroidIcon
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate

/**
 * The card's subtitle: Transit's next step (or, when there's none, as on bike-station steps,
 * the line above the title like "Camina hasta CE-186 …"), and the arrival time or time left,
 * joined with " · ". The time goes first when the line is long.
 */
fun cardSubtitle(state: GoState, settings: Settings): String? {
    val extra = when (settings.extra) {
        Settings.Extra.ARRIVAL -> state.arrival
        Settings.Extra.TIME_LEFT -> state.timeLeft
        Settings.Extra.NONE -> null
    }
    val base = state.text ?: state.previous
    // Long lines get cut off at the end, so put the short time first:
    //   "en 16 minutos · 6:44 a.m."  but  "16 min · Camina hasta CE-186 Gob. Gregorio…"
    val parts = if ((base?.length ?: 0) > LONG_LINE) listOf(extra, base) else listOf(base, extra)
    return parts.filterNotNull().joinToString(" · ").takeIf { it.isNotEmpty() }
}

/** Past this many characters the subtitle is likely to be cut off on the card */
private const val LONG_LINE = 16

class TransitGoTarget : SmartspacerTargetProvider() {

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val state = GoStore.state ?: return emptyList()
        val ctx = provideContext()
        val settings = Settings(ctx)
        // Tapping opens Transit on the trip (or just Transit if the notification has no intent)
        val openTrip = TapAction(
            pendingIntent = state.contentIntent,
            intent = if (state.contentIntent == null) {
                ctx.packageManager.getLaunchIntentForPackage(TransitGoNotificationProvider.TRANSIT_PACKAGE)
            } else null
        )

        // Route chip (full color) > step icon (tinted like Smartspacer's text)
        //   > Transit's app icon (full color) > bus glyph
        val icon = state.chip?.takeIf { settings.showRouteChip }
            ?.let { Icon(AndroidIcon.createWithBitmap(it), contentDescription = state.routeName, shouldTint = false) }
            ?: state.stepIcon
                ?.let { Icon(AndroidIcon.createWithBitmap(it), shouldTint = true) }
            ?: TransitIcon.get(ctx)
                ?.let { Icon(AndroidIcon.createWithBitmap(it), contentDescription = "Transit", shouldTint = false) }
            ?: Icon(AndroidIcon.createWithResource(ctx, R.drawable.ic_transit_go))

        // "[logo] Transit" after the subtitle (the supplemental spot, where weather usually
        // shows), so it's clear the info comes from Transit. Logo only, tinted like the text.
        val source = TransitIcon.glyph(ctx)?.let {
            ComplicationTemplate.Basic(
                "transit_go_source_$smartspacerId",
                Icon(AndroidIcon.createWithBitmap(it), contentDescription = "Transit", shouldTint = true),
                Text("Transit"),
                openTrip
            ).create()
        }

        return listOf(
            TargetTemplate.Basic(
                "transit_go_$smartspacerId",
                ComponentName(ctx, TransitGoTarget::class.java),
                SmartspaceTarget.FEATURE_UNDEFINED,
                Text(state.title),
                cardSubtitle(state, settings)?.let { Text(it) },
                icon,
                openTrip,
                source
            ).create().apply {
                // Same trick as the Uber plugin: with Native Smartspace, the lock screen hides
                // this notification while our card is the first one showing. Nothing is cancelled.
                if (settings.hideNotification) sourceNotificationKey = state.notificationKey
            }
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        val ctx = provideContext()
        return Config(
            label = ctx.getString(R.string.target_label),
            description = ctx.getString(R.string.target_description),
            icon = AndroidIcon.createWithResource(ctx, R.drawable.ic_transit_go),
            configActivity = Intent(ctx, SettingsActivity::class.java),
            // Smartspacer sends Transit's notifications to this provider through its own listener
            notificationProvider = TransitGoNotificationProvider.AUTHORITY
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean = false
}
