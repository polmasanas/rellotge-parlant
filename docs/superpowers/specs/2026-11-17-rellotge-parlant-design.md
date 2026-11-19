# Rellotge Parlant — Design

*Date: 2026-11-17 · Status: awaiting approval*

## 1. Intent

An Android app for a grandmother with poor eyesight who can't read the time on her
phone or on the wall clocks. She has a limited grasp of the phone: she can answer
calls and nothing else. Anything that requires finding a place on the screen
(Quick Settings, widgets, notifications, apps, voice assistants) is out of reach.

**The app does one thing:** when she presses the side button and the screen lights
up, the phone says the current time out loud in Catalan, the way she would say it
herself (traditional "quarts" system, 12-hour, with the part of the day).

> Example: 16:35 → *"Són dos quarts i cinc de cinc de la tarda."*

She doesn't check the phone often, so it will speak rarely and only when she's
looking at the phone.

## 2. Goals, non-goals, anti-goals

**Goals**
- Zero interaction beyond what she already does: pressing the side button to wake the screen.
- Works from the lock screen, with no unlock.
- Natural traditional Catalan time phrasing that she understands instantly.
- Keeps working for months without anyone touching it (survives reboots and the brand's battery killers).
- Set up once by a family member (the "caregiver").

**Non-goals (v1)**
- Hourly or periodic announcements. *Backlog: v2, off by default, with quiet hours.*
- Other languages, other time styles, or a digital "les quatre i trenta-cinc" mode.
- Play Store distribution. Installed by sideloading a signed APK.
- An iOS version.
- Any interface for her. The only screen is for the caregiver.

**Anti-goals: failure even if it "works"**
- She has to learn a new gesture, button, or place on the screen.
- It talks over a ringing phone or an ongoing call.
- It silently stops working without anyone noticing. *(v1 mitigation: self-healing
  restart and a checklist; she'll notice silence. A remote alert is in the backlog.)*
- The phrasing is "almost right" (wrong article, wrong hour reference, "de un" instead of "d'una").
  A wrong time is worse than no time.

## 3. Constraints

- **Unknown phone:** brand, model and Android version are unknown. Target **minSdk 26
  (Android 8.0)** and the latest targetSdk. Must behave on aggressive OEMs (Xiaomi,
  Samsung, Huawei, Oppo, Vivo).
- **`ACTION_SCREEN_ON` cannot be received by a manifest receiver.** It must be
  registered at runtime by a running component, so a **foreground service** has to
  run permanently. It shows a persistent, low-importance notification. She ignores it.
- **Android 14+ foreground service types:** pick an appropriate type (likely
  `specialUse` with a justification, since there's no Play Store review) and declare
  the matching permissions.
- **Catalan TTS:** Google's speech engine ("Speech Services by Google") has a
  `ca-ES` voice. Samsung's TTS engine does **not**, so the app must request the
  Google engine explicitly when it's installed, and the setup screen must detect a
  missing Catalan voice.
- **Start at boot:** `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` restart the service.
- **Her phone has no PIN/pattern** (confirmed). So no direct-boot support is needed, and the service
  starts normally after a reboot. The lock type must be **"Swipe"**, not "None", or
  the keyguard check never fires. The setup checklist verifies this and recommends
  keeping it without a PIN.

## 4. Approaches considered

| Approach | Trade-off | Decision |
|---|---|---|
| **Native Android app (Kotlin)** | Full control of the background service, TTS engine and phrasing. The phrasing logic is testable. Small and dependable. | **Chosen** |
| Cross-platform (Flutter / React Native) | Only helps if an iPhone version is ever wanted. Background services are harder there than in native Android. | Rejected |
| No-code automation (MacroDroid / Tasker) | Could work in a day, but the quarts phrasing becomes an untestable script inside a third-party app, with nothing to own or track. | Rejected |

The **trigger** was also chosen from: screen wake (chosen), shake gesture (new habit,
false triggers in a bag), and automatic periodic announcements (not on demand; moved to v2).

## 5. Behaviour

### 5.1 When it speaks
- Every `SCREEN_ON` event **while the keyguard is showing** (`KeyguardManager.isKeyguardLocked`)
  → speak the current time once. Waking an already-unlocked phone stays silent.
  *(Builder-check call: this filters most non-button wakes. The setup checklist also asks the
  caregiver to disable "wake screen for notifications" / ambient wake features.)*
- **(v0.1.1)** Also speak on every `SCREEN_OFF` (the screen turning off and locking: side button
  or timeout), with the same call/ringing guard. The screen turning off during a call (proximity
  sensor) is silent because the audio mode is not normal.
- **(v0.1.1)** An on/off switch on the caregiver screen ("Dir l'hora en veu alta", default on)
  disables both triggers. The "Prova" button still speaks while it's off, since it's a test.
- No night-time silence: waking at night and wanting the time is a key use case.
- **Don't speak** when the phone is ringing or in a call (check
  `AudioManager.mode` ≠ `MODE_NORMAL` and/or the telephony call state). A missed
  announcement is fine, talking over a call is not.
- If a new `SCREEN_ON` arrives while it's still speaking, don't stack utterances
  (`QUEUE_FLUSH`).
- No cooldown: pressing the button again repeats the time. That's a feature
  ("press again to hear it again").

### 5.2 How it sounds
- Locale `ca-ES`, using Google's TTS engine when available.
- **Audio stream: alarm** (`AudioAttributes.USAGE_ALARM`), so it stays audible
  even if she accidentally turns the media volume down or switches the phone to silent.
  *(Builder's call: if this proves too loud or is blocked by Do Not Disturb on some
  OEMs, fall back to `USAGE_ASSISTANCE_ACCESSIBILITY`.)*
- Speech rate slightly slower than the default (~0.85), configurable on the caregiver screen.
- Request transient audio focus with ducking, so music or radio dips and comes back.

### 5.3 Catalan time phrasing (the core)

A pure Kotlin function `fun phrase(hour: Int, minute: Int): String`, with no Android
dependencies, fully unit-tested for all 1,440 minutes of the day.

**Reference hour.** In the quarts system, times from :08 onward refer to the **next**
hour ("dos quarts de cinc" = 4:30). Times :00–:07 refer to the current hour.

**Hour names (12-hour):** la una, les dues, les tres, les quatre, les cinc, les sis, les
set, les vuit, les nou, les deu, les onze, les dotze.
After "de", elide before a vowel: **d'una**, **d'onze**; otherwise "de dues", "de
dotze"…

**Minute → phrase (at 5-minute anchors; N = next hour):**

| Minutes | Phrase |
|---|---|
| :00 | Són les H (en punt) / És la una (en punt) |
| :05 | Són les H i cinc / És la una i cinc |
| :08 | És mig quart de N |
| :10 | És un quart menys cinc de N |
| :15 | És un quart de N |
| :20 | És un quart i cinc de N |
| :23 | És un quart i mig de N |
| :25 | Són dos quarts menys cinc de N |
| :30 | Són dos quarts de N |
| :35 | Són dos quarts i cinc de N |
| :38 | Són dos quarts i mig de N |
| :40 | Són tres quarts menys cinc de N |
| :45 | Són tres quarts de N |
| :50 | Són tres quarts i cinc de N |
| :53 | Són tres quarts i mig de N |
| :55 | Són les N menys cinc / És la una menys cinc |

**Precision: round DOWN** to the latest anchor in the table above (decided by the
caregiver). It never announces a time that hasn't arrived yet. For example 4:33 → "dos quarts de
cinc", 4:37 → "dos quarts i cinc de cinc", 11:58 → "les dotze menys cinc del matí".
Anchors are therefore 0, 5, 8, 10, 15, 20, 23, 25, 30, 35, 38, 40, 45, 50, 53, 55.

**Verb agreement:** "És" for singular ("és la una", "és un quart", "és mig quart"),
"Són" for plural ("són les dues", "són dos quarts", "són tres quarts").

**Part of the day** is appended, based on the **actual current hour** (not the
reference hour), so 21:15 → "un quart de deu del vespre" rather than "de la nit",
and 16:35 → "…de cinc de la tarda". Proposed bands (caregiver can't edit
these in v1; adjustable in code):

| Current hour (24h) | Suffix |
|---|---|
| 1–5 | de la matinada |
| 6–11 | del matí |
| 12–14 | del migdia |
| 15–19 | de la tarda |
| 20–21 | del vespre |
| 22–23, 0 | de la nit |

Midnight: "Són les dotze de la nit." Noon: "Són les dotze del migdia."

**Golden examples (acceptance tests):**
- 16:35 → "Són dos quarts i cinc de cinc de la tarda."
- 12:45 → "Són tres quarts d'una del migdia."
- 10:30 → "Són dos quarts d'onze del matí."
- 13:00 → "És la una del migdia."
- 00:00 → "Són les dotze de la nit."
- 07:55 → "Són les vuit menys cinc del matí."
- 21:15 → "És un quart de deu del vespre."

The final phrase table must be **reviewed by a native speaker** (the caregiver, ideally
with the grandmother) before release. That review is a tracked task.

## 6. Components

1. **`CatalanTimeFormatter`**: the pure function above, with exhaustive unit tests and golden tests.
2. **`SpeakerService`**: the foreground service. It registers the `SCREEN_ON` receiver, owns a
   long-lived `TextToSpeech` instance (initialised once, re-initialised on error), checks
   the call state, and speaks.
3. **`BootReceiver`**: restarts the service after boot or an app update.
4. **Caregiver screen (`MainActivity`)**: the only UI, used by the family member during setup:
   - Big **"Prova"** button that speaks the current time.
   - Status checklist with ✅/❌ and a "Fix" button for each:
     notification permission (Android 13+), battery optimisation exemption,
     Catalan TTS voice installed (link to install), service running,
     screen lock is "Swipe" (not None, not PIN), "wake screen for notifications" off,
     OEM auto-start setting (deep links for Xiaomi/Huawei/Oppo/Vivo/Samsung where known,
     else instructions).
   - Speech rate slider. Volume test.
   - Short written guide in Catalan/Spanish for the caregiver.
5. **Self-healing:** a `WorkManager` periodic job (e.g. every 15 min) restarts the service if
   the OS killed it.

## 7. Delivery

- **Repo:** GitHub `rellotge-parlant` (private), Kotlin + Gradle, single `app` module.
- **CI:** GitHub Actions runs the unit tests and lint and builds a debug APK on each push.
  A tag builds a release APK signed with a keystore kept in repo secrets. The
  APK is attached to a GitHub Release for sideloading.
- **Tracking:** a Jira project on pmsns.atlassian.net (key **RP**). There's one epic per milestone:
  1. Catalan phrasing engine plus tests, and the native-speaker review
  2. Speaking service (screen-on, TTS, call detection, boot)
  3. Caregiver setup screen and reliability checklist
  4. CI/CD and signed release
  5. Field test on her phone (a week of real use, then fixes)
  6. *(Backlog)* v2 hourly announcements with quiet hours; remote "it stopped working"
     alert to the caregiver (out of v1 by decision: she will notice it's silent)
- **Testing on device:** unit tests cover the phrasing. The service is tested
  manually on an emulator plus at least one real phone, and finally on hers.

## 8. Open questions

None. The precision and lock questions were answered by the caregiver on 2026-11-17.

## 9. Left to the builder
- Exact foreground service type and permission set per API level.
- Notification wording (Catalan) and icon.
- How to detect "ringing / in call" robustly across API levels without asking for
  `READ_PHONE_STATE` if `AudioManager` suffices.
- OEM auto-start deep-link list.
- Package name (suggested `cat.rellotgeparlant`).
