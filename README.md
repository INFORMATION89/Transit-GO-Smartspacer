# Transit GO for Smartspacer

A [Smartspacer](https://github.com/KieronQuinn/Smartspacer) plugin that shows your active [Transit](https://transitapp.com) **GO** trip as a Smartspacer Target, on your home screen and lock screen.

Personal, vibecoded plugin built with help from Claude. Not affiliated with Transit or with Smartspacer's developer. Previously part of [INFOs-Smartspacer-Plugins](https://github.com/INFORMATION89/INFOs-Smartspacer-Plugins).

**[Download the latest release](../../releases/latest)**

The card shows what Transit's GO notification says, for example:

> **Sal en 5 minutos**
> [57-A] en 16 minutos · 6:44 a.m.   [Transit logo] Transit

- **Title:** when to leave, or the current instruction
- **Icon:** Transit's route chip, e.g. a green **57-A**
- **Subtitle:** the next step (**en 16 minutos**), then either the **arrival time** (`· 6:44 a.m.`) or the **time left** in the trip (`· 3 h 07 min`), whichever you pick in the settings. When the line is long (over 16 characters), the time goes first so it isn't cut off: `16 min · Camina hasta CE-186 Gob. Gre…`
- **Transit's logo and the word "Transit"** after the subtitle, so it's clear the info comes from Transit. It's the logo alone, without the app icon's green background (the adaptive icon's monochrome or foreground layer), tinted to match Smartspacer's text

Tapping the card opens Transit on your trip.

**Bike-share steps** work too. On the step where you walk to a bike station, Transit's title is just the bikes available (**0 bicis**) and there's no next-step text, so the card uses the line Transit shows above the title instead:

> **0 bicis**
> [walk icon] 16 min · Camina hasta CE-186 Gob. Gre…   [Transit logo] Transit

The Transit icon uses the card's supplemental spot (a `ComplicationTemplate` passed as the Target's sub-complication), which is where Smartspacer otherwise shows a complication like the weather. While a trip is running, that complication is replaced by the Transit icon.

## Setup

1. Install the APK from the [latest release](../../releases/latest).
2. Turn on **Notification access** for *Transit GO for Smartspacer* (Settings → Notifications → Device & app notifications, or the button in the plugin's settings).
3. In Smartspacer, add the **Transit GO** Target.
4. Start GO in Transit. The card appears while the trip is running and disappears when it ends.

## Settings

Open them from the Target's settings in Smartspacer, or from *Transit GO for Smartspacer* in the app drawer. Smartspacer can't show a plugin's settings inside its own screens, so like other plugins this opens the plugin's own screen.

| Setting | Default | What it does |
|---|---|---|
| Show route chip | On | Uses Transit's colored route chip as the card icon |
| Show step icon | On | When there's no route chip (e.g. walking), uses Transit's icon for the current step, tinted to match Smartspacer |
| After the next step | Arrival | What follows the next step on the card: **Arrival** (`· 6:44 a.m.`), **Time left** (`· 3 h 07 min`) or **Off** |
| Hide notification on lock screen | On | Hides Transit's GO notification on the lock screen while this card is the first one showing |
| Notification access | | Shows whether access is on and opens the system screen |

The top of the screen is a live preview that mirrors the card on your home screen and updates with the trip and with each setting. The screen uses Material 3 Expressive with your wallpaper colors, and Google Sans Text like Smartspacer's other plugins.

With no trip running, the preview shows a **random example trip** (`SampleTrip.kt`): a waiting, riding, walking or bike-station step, with a random route chip (name, color and shape), step icon and times. It changes each time the settings open, on every setting change that affects the card, and after a trip ends. Nothing runs in the background for this: a new example is only drawn when the preview is on screen. Changes inside the preview animate (text cross-fades, the Transit logo slides, icons pop in), and the card never changes size when you change a setting.

If both icon settings are off, or Transit has neither, the card uses Transit's own app icon, loaded from the installed Transit app at runtime (nothing of Transit's is bundled in the plugin). The plugin's bus icon is the last fallback, and is what Smartspacer shows in its Target list, since that list tints icons a single color.

## How it works

**Reading the trip.** Transit posts several notifications on its `go_mode` channel: an empty group summary, one-off alerts, and the live trip. The plugin's notification listener (`TransitGoListener`) only takes the live trip: the foreground-service notification that isn't a group summary. Its title and sub text become the card's title and subtitle.

**Route chip and step icon.** Transit draws the detailed trip view as a custom notification layout (`RemoteViews`), so the route and icons aren't in the normal notification fields. `ChipRenderer` rebuilds that layout inside the plugin and finds:

| Transit view id | What it is |
|---|---|
| `floatingServiceName` | Route chip: colored pill with the route name |
| `text` (inside the chip) | Route name, e.g. "57-A" |
| `image` | Step icon: clock while waiting, walk, ride... |
| `main_instruction` / `next_instruction` | Same text as the title / sub text |
| `previous_instruction` | The small line above the title, e.g. "Camina hasta CE-186 …" on bike-station steps |
| `leftTextView` | "Llegada a las 6:44 a.m.": the plugin keeps just the time |
| `rightTextView` | Time left in the trip, e.g. "3 h 07 min" |

Each one is drawn at 3× into a square bitmap (Smartspacer squeezes icons into a square, so a wide image would look distorted).

**Hiding the notification.** Same approach as the Uber plugin: the card sets `sourceNotificationKey` to the GO notification's key. With Smartspacer's Native Smartspace, the lock screen hides that notification while the card is first. Nothing is cancelled or dismissed, so Transit's trip keeps running. Swiping the card away does nothing to the notification, because cancelling Transit's foreground service could break the trip.

## Languages

The card always shows Transit's own text, so it's in whatever language Transit uses (English, Spanish or any other). Arrival times are recognized in any language and clock format ("Arrive at 6:44 PM", "Llegada a las 6:44 a.m.", "18:44").

The settings screen is in English. The example trip in its preview follows the plugin's language: English, or Spanish on a Spanish phone. If you set Transit to a different language than your phone (Android's per-app **App languages**), you can set the plugin to the same language there so the example matches.

## Limitations

- The route chip and step icon depend on Transit's internal view ids. A Transit update can rename them; the card then falls back to the bus icon until the ids are updated in `ChipRenderer.kt`.
- Only tested with Transit 6.3.7 on a Pixel 7 (Android 17), with live trips in Spanish. English trip text goes through the same code, but hasn't been seen on a live trip yet.
- The hiding only works with Native Smartspace and only on the lock screen; the notification always stays in the shade.
- The plugin has its own notification listener, so it needs its own notification access (unlike the Uber plugin, which uses Smartspacer's).

## Building

GitHub Actions builds the release APK on every push to `main`: open **Actions → Build**, then download the `transit-go-smartspacer` artifact from the latest run.

Locally, with JDK 17+ and the Android SDK (Android 37 platform):

```
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug     # debug build, with the extra logging below
```

Release builds are shrunk with R8 (about 2 MB) and signed with the debug key, since this is a personal plugin with no release key.

Toolchain: Gradle 9.4, Android Gradle Plugin 9.2, Kotlin 2.4, compile SDK 37, Compose with Material 3 `1.5.0-rc01` (the Expressive APIs aren't in a stable release yet).

## Debugging

```
adb logcat -s TransitGo        # one line per update; debug builds also log every go_mode notification
adb logcat -s TransitGoDump    # debug builds: Transit's full layout tree, once per new kind of step
```

Debug builds log every Transit notification on the `go_mode` channel, including ones the plugin skips, and dump the layout again whenever the trip reaches a new kind of step (waiting, walking, bike...), so new step types can be spotted and supported.

Debug builds also save PNGs of Transit's layout and its images to the app's `files/dump/` folder:

```
adb shell run-as dev.marco.smartspacer.transitgo ls files/dump
```

## License

GPL-3.0, see [LICENSE](LICENSE). Built on the [Smartspacer plugin SDK](https://github.com/KieronQuinn/Smartspacer).

## Files

All in `app/src/main/`:

| File | Purpose |
|---|---|
| `TransitGoListener.kt` | Notification listener: picks the live GO notification and stores the trip state |
| `ChipRenderer.kt` | Rebuilds Transit's layout and draws the route chip and step icon |
| `TransitGoTarget.kt` | The Smartspacer Target: builds the card from the trip state and settings |
| `TransitIcon.kt` | Loads Transit's app icon, or just its logo, from the installed app |
| `res/font/` | Google Sans Text (regular, medium, bold), the font Smartspacer's plugins use |
| `Settings.kt` | The settings, stored in SharedPreferences |
| `SettingsActivity.kt` | Compose settings screen (Material 3 Expressive) with the live preview |
| `SampleTrip.kt` | Random example trips for the preview when no trip is running |
| `LayoutDumper.kt` | Debug-only dump of Transit's layout |
