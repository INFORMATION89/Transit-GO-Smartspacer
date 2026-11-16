package dev.marco.smartspacer.transitgo

import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider

/**
 * Settings screen, opened from the Target's settings in Smartspacer or the app drawer.
 * Material 3 Expressive with wallpaper (dynamic) colors and Google Sans Text.
 */
class SettingsActivity : ComponentActivity() {

    private companion object {
        const val SOURCE_URL = "https://github.com/INFORMATION89/Transit-GO-Smartspacer"
        /** Share of the screen's height to drag past the end for the Source code chip to open the repo */
        const val PULL_SCREEN_SHARE = 0.41f
        /** Share of the pull spent growing the chip into a full-width button (the rest fills the screen) */
        const val CHIP_PHASE = 0.35f
        /** Source code label size on the full-width button and on the full screen (1 = chip) */
        const val LABEL_FULL_BUTTON = 1.3f
        const val LABEL_FULL_SCREEN = 2.2f
        /** Footer height: top gap + the largest chip, so the chip growing never changes the
         *  list's length (the chip sits at the bottom of it) */
        val FOOTER_HEIGHT = 28.dp + 64.dp
    }

    private var accessGranted by mutableStateOf(false)
    /** Bumped on every setting change so the preview re-reads the settings */
    private var settingsVersion by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SettingsTheme { SettingsScreen() } }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when coming back from the notification access screen
        accessGranted = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        // After an app update Android sometimes leaves the listener "bound" but disconnected;
        // asking for a rebind fixes that (does nothing if it's already connected)
        if (accessGranted) runCatching {
            android.service.notification.NotificationListenerService.requestRebind(
                android.content.ComponentName(this, TransitGoListener::class.java)
            )
        }
    }

    private fun openSource() = startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri()))

    /** Opens the plugin's own notification access switch (or the full list if that page is missing). */
    private fun openNotificationAccess() {
        val detail = Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
            AndroidSettings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            android.content.ComponentName(this, TransitGoListener::class.java).flattenToString()
        )
        runCatching { startActivity(detail) }
            .onFailure { startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    /** Saves a change, redraws the preview and tells Smartspacer to redraw the card. */
    private fun onSettingChanged(rerollExample: Boolean = true) {
        settingsVersion++
        // With no live trip, show a different example each time, so the change is easy to see
        // (not for settings the preview doesn't show, like the lock screen one)
        if (rerollExample && GoStore.state == null) SampleTrip.reroll()
        SmartspacerTargetProvider.notifyChange(this, TransitGoTarget::class.java)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun SettingsScreen() {
        val settings = remember { Settings(this) }
        val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val listState = rememberLazyListState()
        // Whether the current touch started with the list already at the bottom
        var touchStartedAtEnd by remember { mutableStateOf(false) }
        val pullToOpen = rememberPullToOpen(armed = { touchStartedAtEnd }) { openSource() }
        // Pull phases: first the chip grows into a full-width button, then that button grows
        // until it fills the whole screen, and the repo opens. A light spring follows the finger,
        // smoothing out uneven touch events, and each phase is eased so it starts and settles
        // gently instead of moving at a constant rate.
        val progress by animateFloatAsState(
            pullToOpen.progress,
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "pull"
        )
        val chipPhase = FastOutSlowInEasing.transform((progress / CHIP_PHASE).coerceIn(0f, 1f))
        val takeover = FastOutSlowInEasing.transform(((progress - CHIP_PHASE) / (1f - CHIP_PHASE)).coerceIn(0f, 1f))
        var chipBounds by remember { mutableStateOf<Rect?>(null) }
        Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                    scrollBehavior = scroll,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                state = listState,
                modifier = Modifier.fillMaxSize()
                    // Note where each touch starts, without taking the touch from the list
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            touchStartedAtEnd = !listState.canScrollForward
                        }
                    }
                    .nestedScroll(pullToOpen.connection)
            ) {
                item { Column { PreviewCard(settings) } }

                item { SectionHeader(R.string.section_card) }
                item {
                    ToggleRow(
                        Icons.Rounded.DirectionsBus, MaterialShapes.Cookie9Sided.toShape(),
                        R.string.setting_chip_title, R.string.setting_chip_summary,
                        settings.showRouteChip, Position.First, colorfulWhenOn = true
                    ) { settings.showRouteChip = it }
                }
                item {
                    ToggleRow(
                        Icons.Rounded.Timeline, MaterialShapes.Clover4Leaf.toShape(),
                        R.string.setting_timeline_title, R.string.setting_timeline_summary,
                        settings.showTimeline, Position.Middle, rerollExample = false
                    ) { settings.showTimeline = it }
                }
                item { ExtraRow(settings, MaterialShapes.Arch.toShape()) }

                item { SectionHeader(R.string.section_lock_screen) }
                item {
                    ToggleRow(
                        Icons.Rounded.VisibilityOff, MaterialShapes.Sunny.toShape(),
                        R.string.setting_hide_title, R.string.setting_hide_summary,
                        settings.hideNotification, Position.Single, rerollExample = false
                    ) { settings.hideNotification = it }
                }

                item { SectionHeader(R.string.section_permissions) }
                item {
                    LinkRow(
                        Icons.Rounded.NotificationsActive, MaterialShapes.Pill.toShape(),
                        R.string.setting_access,
                        if (accessGranted) R.string.setting_access_on else R.string.setting_access_off
                    ) { openNotificationAccess() }
                }

                // Sits at the end of the list; grows as you pull past the end (see rememberPullToOpen)
                item {
                    Box(Modifier.fillMaxWidth().height(FOOTER_HEIGHT)) {
                        SourceChip(
                            chipPhase,
                            Modifier.align(Alignment.BottomCenter)
                                .onGloballyPositioned { chipBounds = it.boundsInRoot() }
                                // Hidden while the full-screen version takes over from it
                                .graphicsLayer { alpha = if (takeover > 0f) 0f else 1f }
                        )
                    }
                }
            }
        }
        SourceTakeover(takeover, chipBounds)
        }
    }

    /**
     * Second half of the pull: the full-width Source code button grows from where it sits until
     * it fills the whole screen (corners straightening, icon and text getting bigger), right as
     * the repo opens. Letting go shrinks it back into the button.
     */
    @Composable
    private fun SourceTakeover(t: Float, from: Rect?) {
        if (t <= 0f || from == null) return
        val colors = MaterialTheme.colorScheme
        Surface(
            shape = RoundedCornerShape(lerp(32.dp, 0.dp, t)),
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            modifier = Modifier
                .fillMaxSize()
                // Place a box between the button's bounds and the full screen
                .layout { measurable, c ->
                    val w = c.maxWidth.toFloat(); val h = c.maxHeight.toFloat()
                    val left = from.left * (1 - t)
                    val top = from.top * (1 - t)
                    val right = from.right + (w - from.right) * t
                    val bottom = from.bottom + (h - from.bottom) * t
                    val placeable = measurable.measure(
                        Constraints.fixed((right - left).roundToInt(), (bottom - top).roundToInt())
                    )
                    layout(c.maxWidth, c.maxHeight) { placeable.place(left.roundToInt(), top.roundToInt()) }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                SourceLabel(scale = LABEL_FULL_BUTTON + (LABEL_FULL_SCREEN - LABEL_FULL_BUTTON) * t)
            }
        }
    }

    /**
     * Mirrors the Smartspacer card: title, then the card icon (same order as the Target:
     * route chip > step icon > Transit icon), the subtitle, and Transit's logo + name.
     * With no live trip it shows an example trip, so the settings' effect is still visible.
     * Switching between the two springs (bouncy pop-in and height change).
     */
    @Composable
    private fun PreviewCard(settings: Settings) {
        settingsVersion // re-read the settings when one changes
        val live = GoStore.state
        // New random example each time the settings open, and again after a trip ends
        val sample = remember(SampleTrip.generation) { SampleTrip.create(this@SettingsActivity) }
        val glyph = remember { TransitIcon.glyph(this@SettingsActivity) }
        val appIcon = remember { TransitIcon.get(this@SettingsActivity) }
        val onCard = MaterialTheme.colorScheme.onPrimaryContainer

        // Darker fill with a lighter outline, both tinted from the wallpaper's primary color
        val colors = MaterialTheme.colorScheme
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = lerp(colors.primaryContainer, colors.surfaceContainerLowest, 0.55f),
            border = BorderStroke(1.5.dp, lerp(colors.primaryContainer, colors.primary, 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                .animateContentSize(spring<IntSize>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        ) {
            AnimatedContent(
                targetState = live != null,
                transitionSpec = {
                    (fadeIn(tween(180)) +
                        scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), initialScale = 0.85f)) togetherWith
                        fadeOut(tween(90))
                },
                label = "preview"
            ) { isLive ->
                val state = if (isLive) GoStore.state ?: sample else sample
                // Experimental timeline, drawn exactly as the card gets it
                val timelineImage = remember(state, settingsVersion) { timeline(this@SettingsActivity, state, settings) }
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    // One line (like the real card), cross-fading when it changes
                    AnimatedContent(
                        targetState = state.title,
                        transitionSpec = { fadeIn(tween(320, delayMillis = 90)) togetherWith fadeOut(tween(200)) },
                        label = "title"
                    ) { title ->
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = onCard,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Fixed height and a fixed icon slot, so changing a setting never resizes the card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp).height(32.dp)
                    ) {
                        val chip = state.chip?.takeIf { settings.showRouteChip }
                        val step = state.stepIcon
                        val iconKind = when {
                            chip != null -> IconKind.Chip
                            step != null -> IconKind.Step
                            appIcon != null -> IconKind.App
                            else -> IconKind.None
                        }
                        // Icon swap: the new one pops in with a small bounce, the old one fades.
                        // The slot itself stays 28dp, so nothing next to it moves.
                        AnimatedContent(
                            targetState = iconKind,
                            transitionSpec = {
                                // Cross-fade (old out, new in a little later) with a slight pop
                                (fadeIn(tween(320, delayMillis = 80)) + scaleIn(springy(), initialScale = 0.8f)) togetherWith
                                    (fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.8f))
                            },
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(28.dp),
                            label = "icon"
                        ) { kind ->
                            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                when (kind) {
                                    IconKind.Chip -> chip?.let { Image(it.asImageBitmap(), state.routeName, Modifier.size(28.dp)) }
                                    IconKind.Step -> step?.let { Image(it.asImageBitmap(), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(onCard)) }
                                    IconKind.App -> appIcon?.let { Image(it.asImageBitmap(), "Transit", Modifier.size(24.dp)) }
                                    IconKind.None -> Unit
                                }
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        // Subtitle change (e.g. "· 3 h 07 min" -> "· 6:44 AM"): the text
                        // cross-fades while its width springs to the new size, so the Transit
                        // logo after it slides smoothly instead of jumping
                        AnimatedContent(
                            targetState = cardSubtitle(state, settings).orEmpty(),
                            transitionSpec = {
                                (fadeIn(tween(320, delayMillis = 90)) togetherWith fadeOut(tween(200)))
                                    .using(SizeTransform(clip = false) { _, _ -> springy() })
                            },
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.weight(1f, fill = false),
                            label = "subtitle"
                        ) { text ->
                            Text(
                                text,
                                style = MaterialTheme.typography.titleMedium, color = onCard,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        // With the timeline, Transit's logo is in the image instead
                        glyph?.takeIf { timelineImage == null }?.let {
                            Spacer(Modifier.size(14.dp))
                            Image(it.asImageBitmap(), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(onCard))
                            Spacer(Modifier.size(6.dp))
                            Text("Transit", style = MaterialTheme.typography.titleMedium, color = onCard)
                        }
                    }
                    timelineImage?.let {
                        Image(
                            it.asImageBitmap(), null,
                            Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(20.dp))
                        )
                    }
                }
            }
        }
        // Note under the card (outside it, so the card itself looks exactly like the real one)
        if (live == null) {
            Text(
                stringResource(R.string.preview_example),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )
        }
    }

    /** Pull-past-the-end state: [progress] 0..1, and the scroll connection that drives it. */
    private class PullToOpen(val connection: NestedScrollConnection, private val pull: () -> Float, private val max: Float) {
        val progress get() = (pull() / max).coerceIn(0f, 1f)
    }

    /**
     * Normal scrolling and flinging are untouched. A pull only starts when the finger touched
     * down while the list was already at the end ([armed]), so swiping or flicking down to the
     * bottom never triggers it. Then the drag left over past the end is the pull, 1:1 with the
     * finger: dragging about [PULL_SCREEN_SHARE] of the screen's height (from near the bottom to
     * near the top) calls [onOpen] once, with a vibration. Letting go springs it back to 0.
     */
    @Composable
    private fun rememberPullToOpen(armed: () -> Boolean, onOpen: () -> Unit): PullToOpen {
        val maxPx = LocalWindowInfo.current.containerSize.height * PULL_SCREEN_SHARE
        val currentArmed by rememberUpdatedState(armed)
        val haptics = LocalHapticFeedback.current
        val pull = remember { mutableFloatStateOf(0f) }
        val opened = remember { mutableStateOf(false) }
        val currentOnOpen by rememberUpdatedState(onOpen)
        val connection = remember(maxPx) {
            object : NestedScrollConnection {
                // Dragging back down while pulled: shrink the pull before the list scrolls
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (pull.floatValue > 0f && available.y > 0f && source == NestedScrollSource.UserInput) {
                        val used = minOf(available.y, pull.floatValue)
                        pull.floatValue -= used
                        return Offset(0f, used)
                    }
                    return Offset.Zero
                }

                // Drag left over after the list hit its end: that's the pull
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < 0f && source == NestedScrollSource.UserInput && currentArmed()) {
                        pull.floatValue = (pull.floatValue - available.y).coerceAtMost(maxPx)
                        if (pull.floatValue >= maxPx && !opened.value) {
                            opened.value = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnOpen()
                        }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                // Finger lifted: spring the chip back
                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pull.floatValue <= 0f) return Velocity.Zero
                    animate(pull.floatValue, 0f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) { v, _ ->
                        pull.floatValue = v
                    }
                    opened.value = false
                    return available
                }
            }
        }
        return remember(connection) { PullToOpen(connection, { pull.floatValue }, maxPx) }
    }

    /** Which icon the card shows, so the preview can animate when it changes */
    private enum class IconKind { Chip, Step, App, None }

    /** Bright colors like Android's colorful settings icons; never the same one twice in a row */
    private val accents = listOf(
        Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFF9AB00), Color(0xFFEA4335),
        Color(0xFFA142F4), Color(0xFF24C1E0), Color(0xFFFA7B17), Color(0xFFF538A0)
    )
    private fun randomAccent(not: Color? = null) = (accents - setOfNotNull(not)).random()

    /** Gentle bounce used by the preview's in-card transitions */
    private fun <T> springy() = spring<T>(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)

    /** Arrival time / Time left / Off, as a segmented button under the row's text. */
    @Composable
    private fun ExtraRow(settings: Settings, iconShape: Shape) {
        var selected by remember { mutableStateOf(settings.extra) }
        val options = listOf(
            Settings.Extra.ARRIVAL to R.string.setting_extra_arrival,
            Settings.Extra.TIME_LEFT to R.string.setting_extra_time_left,
            Settings.Extra.NONE to R.string.setting_extra_none
        )
        Surface(
            shape = Position.Last.shape(),
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Schedule, iconShape)
                    RowTexts(R.string.setting_extra_title, R.string.setting_extra_summary, Modifier.weight(1f).padding(start = 16.dp))
                }
                // Full width (not indented under the text) so "Arrival Time" fits
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = selected == value,
                            onClick = {
                                selected = value
                                settings.extra = value
                                // Keep the same example so the change to its time is easy to see
                                onSettingChanged(rerollExample = false)
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size)
                        ) { Text(stringResource(label), maxLines = 1) }
                    }
                }
            }
        }
    }

    /**
     * "[GitHub] Source code" chip at the bottom, opening the repo in the browser.
     * At [progress] 0 it's a small chip; it grows as you pull past the end of the list until, at
     * 1 (when the repo opens), it's a full-width button: taller, bigger icon and text, tinted.
     */
    @Composable
    private fun SourceChip(progress: Float, modifier: Modifier) {
        val colors = MaterialTheme.colorScheme
        val p = progress
        Surface(
            onClick = { openSource() },
            shape = RoundedCornerShape(50),
            color = lerp(colors.surfaceContainerHigh, colors.primaryContainer, p),
            contentColor = lerp(colors.onSurface, colors.onPrimaryContainer, p),
            border = BorderStroke(1.dp, lerp(colors.outlineVariant, colors.primary.copy(alpha = 0.5f), p)),
            modifier = modifier
                .fillMaxWidth()
                // Width goes from the chip's natural width to the full row
                .layout { measurable, constraints ->
                    val natural = measurable.maxIntrinsicWidth(constraints.maxHeight)
                    val w = (natural + (constraints.maxWidth - natural) * p).toInt()
                        .coerceIn(natural.coerceAtMost(constraints.maxWidth), constraints.maxWidth)
                    val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.place((constraints.maxWidth - w) / 2, 0)
                    }
                }
                .height(lerp(40.dp, 64.dp, p))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                SourceLabel(scale = 1f + (LABEL_FULL_BUTTON - 1f) * p)
            }
        }
    }

    /**
     * "[GitHub] Source code", laid out once at its smallest size and enlarged with [scale] on the
     * GPU, so growing it doesn't re-measure the text on every frame.
     */
    @Composable
    private fun SourceLabel(scale: Float) = Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Icon(painterResource(R.drawable.ic_github), null, Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.source_code), style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }

    @Composable
    private fun SectionHeader(text: Int) = Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )

    private enum class Position { First, Middle, Last, Single }

    // Grouped rows: big outer corners, small inner ones (Android 16 settings style)
    private fun Position.shape(): Shape {
        val big = 24.dp; val small = 6.dp
        return when (this) {
            Position.First -> RoundedCornerShape(big, big, small, small)
            Position.Middle -> RoundedCornerShape(small)
            Position.Last -> RoundedCornerShape(small, small, big, big)
            Position.Single -> RoundedCornerShape(big)
        }
    }

    /**
     * @param colorfulWhenOn give the icon a random bright color while on, like Android's
     *   colorful settings icons (picked again each time it's turned on)
     */
    @Composable
    private fun ToggleRow(
        icon: ImageVector, iconShape: Shape, title: Int, summary: Int,
        initial: Boolean, position: Position, rerollExample: Boolean = true,
        colorfulWhenOn: Boolean = false, onChange: (Boolean) -> Unit
    ) {
        var checked by remember { mutableStateOf(initial) }
        var accent by remember { mutableStateOf(randomAccent()) }
        val toggle = { value: Boolean ->
            if (value && !checked) accent = randomAccent(accent)
            checked = value
            onChange(value)
            onSettingChanged(rerollExample)
        }
        val badgeColor = if (colorfulWhenOn && checked) accent else null
        SettingRow(icon, iconShape, title, summary, position, badgeColor, onClick = { toggle(!checked) }) {
            Switch(
                checked = checked,
                onCheckedChange = toggle,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null
            )
        }
    }

    @Composable
    private fun LinkRow(icon: ImageVector, iconShape: Shape, title: Int, summary: Int, onClick: () -> Unit) =
        SettingRow(icon, iconShape, title, summary, Position.Single, null, onClick) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

    @Composable
    private fun SettingRow(
        icon: ImageVector, iconShape: Shape, title: Int, summary: Int, position: Position,
        badgeColor: Color?, onClick: () -> Unit, trailing: @Composable () -> Unit
    ) {
        Surface(
            shape = position.shape(),
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth().clip(position.shape()).clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                IconBadge(icon, iconShape, badgeColor)
                RowTexts(title, summary, Modifier.weight(1f).padding(horizontal = 16.dp))
                trailing()
            }
        }
    }

    /**
     * The row's icon on an Expressive shape. With [accent], the color spreads from the center
     * out to fill the shape (and shrinks back to the center when it's removed).
     */
    @Composable
    private fun IconBadge(icon: ImageVector, shape: Shape, accent: Color? = null) {
        // Keep the last color so it can shrink away after the accent is removed
        var shown by remember { mutableStateOf(accent ?: Color.Transparent) }
        if (accent != null) shown = accent
        val on = accent != null

        // Color spreading from the center: a soft spring on the way in, quick ease on the way out
        val reveal by animateFloatAsState(
            targetValue = if (on) 1f else 0f,
            animationSpec = if (on) spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            else tween(220, easing = FastOutSlowInEasing),
            label = "reveal"
        )
        // The shape turns a little as it fills (the icon itself stays upright)
        val turn by animateFloatAsState(
            targetValue = if (on) 40f else 0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
            label = "turn"
        )
        // Squish then bounce back when turned on (not when the screen first opens)
        val pop = remember { Animatable(1f) }
        var firstRun by remember { mutableStateOf(true) }
        LaunchedEffect(accent) {
            if (firstRun) { firstRun = false; return@LaunchedEffect }
            if (accent != null) {
                pop.animateTo(0.82f, tween(90))
                pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
            }
        }

        val base = MaterialTheme.colorScheme.onSecondaryContainer
        // White icon on dark colors, near-black on light ones (like yellow)
        val onAccent = if (shown.luminance() > 0.5f) Color(0xFF1F1F1F) else Color.White
        val container = MaterialTheme.colorScheme.secondaryContainer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
        ) {
            // Shape layer: rotates, with the color circle growing inside it
            Box(
                Modifier.matchParentSize()
                    .graphicsLayer { rotationZ = turn }
                    .clip(shape)
                    .background(container)
                    .drawBehind {
                        if (reveal > 0f) drawCircle(
                            shown.copy(alpha = reveal.coerceIn(0f, 1f)),
                            radius = size.maxDimension * 0.75f * reveal
                        )
                    }
            )
            Icon(icon, null, tint = lerp(base, onAccent, reveal.coerceIn(0f, 1f)), modifier = Modifier.size(22.dp))
        }
    }

    @Composable
    private fun RowTexts(title: Int, summary: Int, modifier: Modifier) = Column(modifier) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    // Wallpaper colors need Android 12; older versions get the standard Material colors
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = GoogleSansTypography,
        content = content
    )
}

/** Google Sans Text, the font Smartspacer and its other plugins use in their settings. */
private val GoogleSansText = FontFamily(
    Font(R.font.google_sans_text_regular, FontWeight.Normal),
    Font(R.font.google_sans_text_medium, FontWeight.Medium),
    Font(R.font.google_sans_text_medium, FontWeight.SemiBold),
    Font(R.font.google_sans_text_bold, FontWeight.Bold)
)

/** The default Material 3 type scale with every style switched to Google Sans Text. */
private val GoogleSansTypography: Typography = Typography().run {
    fun TextStyle.gs() = copy(fontFamily = GoogleSansText)
    Typography(
        displayLarge = displayLarge.gs(), displayMedium = displayMedium.gs(), displaySmall = displaySmall.gs(),
        headlineLarge = headlineLarge.gs(), headlineMedium = headlineMedium.gs(), headlineSmall = headlineSmall.gs(),
        titleLarge = titleLarge.gs(), titleMedium = titleMedium.gs(), titleSmall = titleSmall.gs(),
        bodyLarge = bodyLarge.gs(), bodyMedium = bodyMedium.gs(), bodySmall = bodySmall.gs(),
        labelLarge = labelLarge.gs(), labelMedium = labelMedium.gs(), labelSmall = labelSmall.gs()
    )
}
