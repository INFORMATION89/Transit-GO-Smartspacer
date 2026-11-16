First release. Shows your active [Transit](https://transitapp.com) GO trip as a [Smartspacer](https://github.com/KieronQuinn/Smartspacer) Target on your home and lock screen.

**On the card**
- Transit's instruction ("Sal en 5 minutos") and next step ("en 16 minutos")
- Transit's colored route chip, or the icon for the current step (wait, walk, ride, bike)
- Arrival time or time left in the trip, your pick; the time moves to the front on long lines so it isn't cut off
- Transit's logo and name, so it's clear where the info comes from
- Bike-share steps: shows where to walk to the station
- Tap to open the trip in Transit

**Lock screen**
- Hides Transit's GO notification on the lock screen while the card is first (needs Smartspacer's Native Smartspace; the notification stays in the shade)

**Settings**
- Material 3 Expressive, with your wallpaper colors and Google Sans Text
- Live preview of the card, with a random example trip when no trip is running

**Install**
1. Install the APK below. It's signed with a debug key (personal plugin, no release key).
2. Turn on Notification access for *Transit GO for Smartspacer*.
3. Add the **Transit GO** Target in Smartspacer, then start GO in Transit.

Tested with Transit 6.3.7 on a Pixel 7 (Android 17), in Spanish. The route chip and step icon are read from Transit's notification layout, so a Transit update may need an update here.
