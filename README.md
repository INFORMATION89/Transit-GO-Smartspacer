# Transit GO for Smartspacer

A [Smartspacer](https://github.com/KieronQuinn/Smartspacer) plugin that shows your active [Transit](https://transitapp.com) GO trip on your home and lock screen: the current instruction, the route chip, the next step with the arrival time or time left, and Transit's logo. Tapping the card opens the trip in Transit. It can also hide Transit's GO notification on the lock screen while the card is showing (with Native Smartspace), and its settings show a live preview of the card. The plugin is in English, Spanish, French, Portuguese, German, Italian, Dutch, Japanese, Chinese and Russian, and the card shows Transit's own text in whatever language Transit uses.

To set it up, install the APK from the [latest release](../../releases/latest), turn on **Notification access** for *Transit GO for Smartspacer*, then add the **Transit GO** Target in Smartspacer and start GO in Transit.

If Google Play Protect blocks the install ("App blocked to protect your device"), that's because the APK comes from a download and asks for Notification access, not because anything is wrong with it. Either install it from a computer with `adb install transit-go-smartspacer-<version>.apk`, or open the Play Store → your profile → **Play Protect** → ⚙ and turn off **Scan apps with Play Protect**, install the APK, then turn it back on.

## Why Notification access

Transit has no API for its GO trips, so the plugin reads the trip from Transit's GO notification, and Android only allows that with Notification access. Here's what it does with it:

- **It reads only Transit's GO notification.** Every other notification is ignored: the plugin checks which app and channel a notification comes from, and nothing else, before dropping it.
- **It has no internet permission**, so it can't send anything anywhere. The trip stays in memory while GO is running and is gone when it ends; the only thing saved is your settings.
- **It never dismisses or changes notifications.** Hiding the GO notification on the lock screen is done by Smartspacer while the card is showing; the notification stays in your shade.

You can check all of this in the source code, or turn Notification access off at any time (the card just stops appearing).

It's a personal project built with help from Claude, not affiliated with Transit or Smartspacer, and licensed under [GPL-3.0](LICENSE).
