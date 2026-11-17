# Rellotge Parlant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app that speaks the current time in traditional Catalan ("quarts") whenever the locked phone's screen is woken.

**Architecture:** A pure-Kotlin `CatalanTimeFormatter` (exhaustively unit-tested) produces the phrase. A permanently running foreground `SpeakerService` registers a runtime `SCREEN_ON` receiver, asks a pure `SpeakPolicy` whether to speak, and passes the phrase to a `Speaker` that wraps `TextToSpeech`. A boot receiver and a WorkManager watchdog keep the service alive. A single caregiver `MainActivity` runs the setup checks.

**Tech Stack:** Kotlin, Gradle (Kotlin DSL, version catalog), AndroidX (core-ktx, appcompat, work-runtime-ktx), JUnit 4, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-17-rellotge-parlant-design.md`

## Global Constraints

- Package / applicationId: `cat.rellotgeparlant`.
- `minSdk 26`, `compileSdk`/`targetSdk` = latest stable at build time (≥ 35). JVM target 17.
- Speech locale `ca-ES`. Prefer engine `com.google.android.tts` when installed.
- Audio: `AudioAttributes.USAGE_ALARM`, `CONTENT_TYPE_SPEECH`, transient focus with ducking, `QUEUE_FLUSH`.
- Default speech rate `0.85f`. The caregiver can adjust it in the range 0.5–1.2.
- Speak only on `SCREEN_ON` while the keyguard is showing and the audio mode is `MODE_NORMAL`.
- Rounding is always **down** to the anchors `0,5,8,10,15,20,23,25,30,35,38,40,45,50,53,55`.
- The part of the day comes from the **current** hour: 1–5 matinada, 6–11 matí, 12–14 migdia, 15–19 tarda, 20–21 vespre, 22–23 and 0 nit.
- If no Catalan voice is available, **stay silent**. Never speak in another language.
- All user-visible strings are in Catalan.
- No network permission.

## Review Focus

1. **Wrap-around at noon and midnight.** 11:55 → "Són les dotze menys cinc del matí". 12:30 → "Són dos quarts d'una del migdia". 23:58 → "Són les dotze menys cinc de la nit". 00:07 → "Són les dotze i cinc de la nit". Tested in Task 2.
2. **First wake after boot, before TTS has initialised.** The phrase must be spoken once TTS is ready, not dropped. A test that the pending utterance is kept goes in Task 4.
3. **Catalan voice missing or engine changed.** Silent, plus ❌ on the setup screen, never another language. Tested in Task 4 (`Speaker` reports `VoiceStatus.MISSING`) and Task 6.
4. **Clock, timezone or DST change while the service runs.** The time is read at the moment of speaking (`LocalTime.now()` via an injected `Clock`), never cached. Tested in Task 5.
5. **Repeated quick presses.** A second wake while speaking restarts the phrase and doesn't queue it. `QUEUE_FLUSH` is asserted in Task 4.

---

### Task 1: Project scaffold and CI

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml` (`app_name` = "Rellotge Parlant"), `.gitignore`, `.github/workflows/ci.yml`, `README.md`

**Interfaces:**
- Produces: a buildable `app` module. `./gradlew testDebugUnitTest lintDebug assembleDebug` works.

- [ ] **Step 1:** Create the Gradle project using the Global Constraints values. Add the dependencies core-ktx, appcompat, work-runtime-ktx and junit.
- [ ] **Step 2:** Add a placeholder unit test `SanityTest.alwaysPasses`.
- [ ] **Step 3:** Run `./gradlew testDebugUnitTest assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4:** Add `ci.yml`: on push and pull_request, JDK 17, run `./gradlew testDebugUnitTest lintDebug assembleDebug`, and upload the debug APK as an artifact.
- [ ] **Step 5:** Commit `chore: scaffold Android project and CI` and push. Expected: green CI run.

### Task 2: Catalan time formatter

**Files:**
- Create: `app/src/main/java/cat/rellotgeparlant/time/CatalanTimeFormatter.kt`
- Test: `app/src/test/java/cat/rellotgeparlant/time/CatalanTimeFormatterTest.kt`

**Interfaces:**
- Produces: `object CatalanTimeFormatter { fun phrase(hour: Int, minute: Int): String }`. The input is 24-hour (`0..23`, `0..59`). It throws `IllegalArgumentException` outside that range. The output is a full sentence ending in ".".

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun goldenExamples() {
    mapOf(
        (16 to 35) to "Són dos quarts i cinc de cinc de la tarda.",
        (12 to 45) to "Són tres quarts d'una del migdia.",
        (10 to 30) to "Són dos quarts d'onze del matí.",
        (13 to 0)  to "És la una del migdia.",
        (0 to 0)   to "Són les dotze de la nit.",
        (7 to 55)  to "Són les vuit menys cinc del matí.",
        (21 to 15) to "És un quart de deu del vespre.",
    ).forEach { (t, exp) -> assertEquals(exp, CatalanTimeFormatter.phrase(t.first, t.second)) }
}
@Test fun everyAnchorForFourPm() { /* 16:00 → "Són les quatre de la tarda.", 16:05 "Són les quatre i cinc de la tarda.",
   16:08 "És mig quart de cinc de la tarda.", 16:10 "És un quart menys cinc de cinc de la tarda.",
   16:15 "És un quart de cinc…", 16:20 "És un quart i cinc de cinc…", 16:23 "És un quart i mig de cinc…",
   16:25 "Són dos quarts menys cinc de cinc…", 16:30 "Són dos quarts de cinc…", 16:35 (golden),
   16:38 "Són dos quarts i mig de cinc…", 16:40 "Són tres quarts menys cinc de cinc…", 16:45 "Són tres quarts de cinc…",
   16:50 "Són tres quarts i cinc de cinc…", 16:53 "Són tres quarts i mig de cinc…", 16:55 "Són les cinc menys cinc de la tarda." */ }
@Test fun roundsDown() { // 16:33 == 16:30 phrase; 16:37 == 16:35; 16:07 == 16:05; 16:09 == 16:08; 11:58 == "Són les dotze menys cinc del matí." }
@Test fun wrapAround() { // 12:30 "Són dos quarts d'una del migdia."; 23:58 "Són les dotze menys cinc de la nit.";
                         // 00:07 "Són les dotze i cinc de la nit."; 00:30 "Són dos quarts d'una de la nit."; 12:00 "Són les dotze del migdia." }
@Test fun singularOne() { // 01:00 "És la una de la matinada."; 00:55 "És la una menys cinc de la nit."; 13:05 "És la una i cinc del migdia." }
@Test fun elision() { // 10:15 "És un quart d'onze del matí."; 1:15 "És un quart de dues de la matinada." }
@Test fun dayPartBands() { // spot-check each boundary hour: 0,1,5,6,11,12,14,15,19,20,21,22,23 at :00 }
@Test fun allMinutesProduceSentence() { // for 0..23 × 0..59: starts with "És " or "Són ", ends with ".", no "de u", no "de o" (un-elided), no double spaces }
@Test(expected = IllegalArgumentException::class) fun rejectsOutOfRange() { CatalanTimeFormatter.phrase(24, 0) }
```

(Spell out every commented case as a real `assertEquals`. The strings above are the exact expected values.)

- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests '*CatalanTimeFormatterTest*'`. Expected: FAIL (unresolved reference).
- [ ] **Step 3:** Implement `phrase`. Floor the minute to its anchor. Anchors 0 and 5 use the current hour ("les H" / "les H i cinc"). 8–53 use "<quarter phrase> de|d' <next hour name without article>". 55 uses "les N menys cinc". The hour names are una, dues, tres, quatre, cinc, sis, set, vuit, nou, deu, onze, dotze. The article is "la" for una and "les" otherwise. "És" goes with singular subjects (la una, mig quart, un quart…) and "Són" with plural ones. The day-part suffix comes from the **current** hour.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Add a test-only task `PhraseTableDump` (a JUnit test annotated `@Ignore` by default, or a Gradle `run` of a main in `src/test`) that writes all 1,440 phrases to `build/phrase-table.txt` for the native-speaker review (Task 8).
- [ ] **Step 6:** Commit `feat: Catalan quarts time formatter`.

### Task 3: Speak policy

**Files:**
- Create: `app/src/main/java/cat/rellotgeparlant/service/SpeakPolicy.kt`
- Test: `app/src/test/java/cat/rellotgeparlant/service/SpeakPolicyTest.kt`

**Interfaces:**
- Produces: `object SpeakPolicy { fun shouldSpeak(keyguardLocked: Boolean, audioMode: Int): Boolean }`. `audioMode` uses `AudioManager.MODE_*` int values.

- [ ] **Step 1:** Write tests. Locked + `MODE_NORMAL` (0) → true. Unlocked + 0 → false. Locked + `MODE_RINGTONE` (1) / `MODE_IN_CALL` (2) / `MODE_IN_COMMUNICATION` (3) → false.
- [ ] **Step 2:** Run. Expected FAIL. **Step 3:** Implement. **Step 4:** Run. Expected PASS.
- [ ] **Step 5:** Commit `feat: speak policy`.

### Task 4: Speaker (TTS wrapper)

**Files:**
- Create: `app/src/main/java/cat/rellotgeparlant/speech/Speaker.kt`, `speech/TtsEngine.kt`
- Test: `app/src/test/java/cat/rellotgeparlant/speech/SpeakerTest.kt`

**Interfaces:**
- Produces:
  - `interface TtsEngine { fun init(onReady: (Boolean) -> Unit); fun isCatalanAvailable(): Boolean; fun speak(text: String, flush: Boolean); fun setRate(rate: Float); fun shutdown() }`
  - `class AndroidTtsEngine(context: Context) : TtsEngine` builds `TextToSpeech(context, listener, "com.google.android.tts")` when that package is installed, else the default engine. It sets `Locale("ca","ES")` and alarm `AudioAttributes`, and requests transient focus with ducking around each utterance (released in `UtteranceProgressListener.onDone/onError`).
  - `enum class VoiceStatus { READY, INITIALISING, MISSING }`
  - `class Speaker(engine: TtsEngine) { val status: VoiceStatus; fun say(text: String); fun setRate(rate: Float); fun shutdown() }`

- [ ] **Step 1:** Write tests against a `FakeTtsEngine`:
  - `say` before ready → nothing spoken. After `onReady(true)` → the last pending text is spoken exactly once.
  - `say` when ready → `speak(text, flush = true)`.
  - Catalan unavailable after init → `status == MISSING` and `say` speaks nothing.
  - `onReady(false)` → `status == MISSING`.
- [ ] **Step 2:** Run. Expected FAIL. **Step 3:** Implement `Speaker` and `AndroidTtsEngine`. **Step 4:** Run. Expected PASS.
- [ ] **Step 5:** Commit `feat: Catalan TTS speaker`.

### Task 5: Foreground service, boot and watchdog

**Files:**
- Create: `service/SpeakerService.kt`, `service/BootReceiver.kt`, `service/WatchdogWorker.kt`, `service/TimeAnnouncer.kt`
- Modify: `AndroidManifest.xml` (service with `foregroundServiceType="specialUse"` and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` = "Anuncia l'hora en veu alta quan s'encén la pantalla". Permissions `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Receiver for `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.)
- Test: `app/src/test/java/cat/rellotgeparlant/service/TimeAnnouncerTest.kt`

**Interfaces:**
- Consumes: `CatalanTimeFormatter.phrase`, `SpeakPolicy.shouldSpeak`, `Speaker`.
- Produces:
  - `class TimeAnnouncer(clock: java.time.Clock, speaker: Speaker) { fun onScreenOn(keyguardLocked: Boolean, audioMode: Int) }`
  - `SpeakerService.ensureRunning(context: Context)` (static, idempotent, uses `ContextCompat.startForegroundService`).
  - `WatchdogWorker.schedule(context)`: unique periodic work every 15 min calling `ensureRunning`.

- [ ] **Step 1:** Write `TimeAnnouncerTest` with a fixed `Clock` and a fake engine:
  - locked + normal at 16:35 → speaks "Són dos quarts i cinc de cinc de la tarda."
  - unlocked → nothing
  - after changing the clock to 09:00 in another zone, the next call speaks the new time (clock read on every call)
- [ ] **Step 2:** Run. Expected FAIL. **Step 3:** Implement `TimeAnnouncer`, then the service. The service should:
  - register the `ACTION_SCREEN_ON` receiver in `onCreate`
  - read `KeyguardManager.isKeyguardLocked` and `AudioManager.mode`
  - show a low-importance ongoing notification "Rellotge parlant actiu"
  - return `START_STICKY`
  - read the speech rate from SharedPreferences key `speech_rate` (default 0.85f)
  Then implement `BootReceiver` (calls `ensureRunning` and `WatchdogWorker.schedule`) and the worker.
- [ ] **Step 4:** Run the unit tests. Expected PASS. Then `assembleDebug`, install on an emulator (API 26 and latest), lock, press power. Expected: Catalan time spoken. Unlock and turn the screen off and on with the keyguard dismissed. Expected: silent. Simulate an incoming call (emulator extended controls). Expected: silent. Reboot the emulator. Expected: works after reboot without opening the app.
- [ ] **Step 5:** Commit `feat: screen-on speaking service with boot and watchdog`.

### Task 6: Caregiver setup screen

**Files:**
- Create: `ui/MainActivity.kt`, `res/layout/activity_main.xml`, `setup/SetupChecks.kt`, `setup/OemAutostart.kt`, strings in `values/strings.xml`
- Test: `app/src/test/java/cat/rellotgeparlant/setup/OemAutostartTest.kt`

**Interfaces:**
- Consumes: `Speaker`/`VoiceStatus`, `SpeakerService.ensureRunning`, `CatalanTimeFormatter`.
- Produces:
  - `data class Check(val id: CheckId, val ok: Boolean?, val fix: Intent?)` (`ok == null` means it can't be detected, shown as an instruction)
  - `enum class CheckId { NOTIFICATIONS, BATTERY_OPTIMISATION, CATALAN_VOICE, SERVICE_RUNNING, SWIPE_LOCK, NOTIFICATION_WAKE, OEM_AUTOSTART }`
  - `fun runChecks(context: Context): List<Check>`
  - `object OemAutostart { fun intentFor(manufacturer: String): ComponentName? }`, which knows Xiaomi, Huawei, Oppo, Vivo and Samsung

- [ ] **Step 1:** Write `OemAutostartTest`: "Xiaomi" (any case) → the MIUI autostart component; an unknown brand → null.
- [ ] **Step 2:** Run. Expected FAIL. **Step 3:** Implement the checks and the screen:
  - a big "Prova" button that speaks now through the same `Speaker`
  - a rate slider 0.5–1.2 that saves `speech_rate`
  - the checklist with ✅/❌/ℹ️ and "Arregla" buttons
  - `SWIPE_LOCK`: `isDeviceSecure == false && isKeyguardSecure == false` can't tell "None" from "Swipe", so show it as ℹ️ with instructions, ok=null
  - `NOTIFICATION_WAKE` is also ℹ️
  - opening the activity calls `ensureRunning` and `WatchdogWorker.schedule`
- [ ] **Step 4:** Run the unit tests. Expected PASS. Check on the emulator: every check renders, each "Arregla" opens the right settings page, and "Prova" speaks.
- [ ] **Step 5:** Commit `feat: caregiver setup screen`.

### Task 7: Signed release

**Files:**
- Create: `.github/workflows/release.yml`
- Modify: `app/build.gradle.kts` (release `signingConfig` from env vars `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Update `README.md` with install and setup steps in Catalan.

- [ ] **Step 1:** Generate the keystore locally (never committed) and add the four repo secrets.
- [ ] **Step 2:** The workflow runs on tag `v*`: tests, then `assembleRelease`, then a GitHub Release with `rellotge-parlant-<tag>.apk` attached.
- [ ] **Step 3:** Push tag `v0.1.0`. Expected: the release has a signed APK, and `apksigner verify` passes.
- [ ] **Step 4:** Commit `ci: signed release workflow`.

### Task 8: Native-speaker phrase review (manual)

- [ ] Generate `phrase-table.txt` (Task 2 Step 5) and review it with the grandmother or the caregiver.
- [ ] Turn every correction into a failing test in `CatalanTimeFormatterTest`, then fix it.
- [ ] Sign-off is recorded in the Jira issue.

### Task 9: Field test on her phone (manual)

- [ ] Install the release APK, then work through the setup checklist until everything is ✅ or ℹ️ handled.
- [ ] One week of real use. Check that it still speaks after day 1, day 3 and day 7, and after a reboot.
- [ ] Log any issue as a Jira bug. Fix it, then cut `v1.0.0`.
