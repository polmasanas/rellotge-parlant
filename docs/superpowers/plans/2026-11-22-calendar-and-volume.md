# Family Calendar & Voice Volume (v0.2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Read a shared Google Calendar's upcoming events aloud in Catalan (on wake/lock, as a reminder before each event, and as a morning announcement for all-day events), and add a voice volume slider.

**Architecture:** Pure Kotlin components (`atPhrase`, `EventText`, `UpcomingEvents`, `ReminderPlanner`) decide *what* to say and *when*, and are exhaustively unit-tested. Thin Android adapters (`CalendarSource`, `ReminderScheduler`/`ReminderReceiver`, the service and the setup screen) feed them phone data and act on their output.

**Tech Stack:** Kotlin, Android `CalendarContract.Instances`, `AlarmManager` exact alarms, JUnit 4 (existing project).

**Spec:** `docs/superpowers/specs/2026-11-22-calendar-and-volume-design.md` (builds on `docs/superpowers/specs/2026-11-17-rellotge-parlant-design.md`)

## Global Constraints

- Defaults: hours ahead `6` (range 1–24), reminder `60` min (options 0=off/15/30/60/120), morning time `09:00`, read location `true`, read description `true`, volume `1.0` (range 0.1–1.0).
- At most 3 events are read, then " I N més." is said (a new sentence).
- Description cleaning: strip HTML tags, remove URLs, collapse whitespace, keep the first 2 sentences, max 200 chars.
- Times are rounded **down** with the existing anchors, never later than the real time.
- Event phrase: `<Avui|Demà>, <atPhrase>: <title>.[ Lloc: <location>.][ <description>]`. An empty title becomes "un esdeveniment".
- Reminder prefix "Recordatori: "; morning text "Bon dia. Avui: <t1>, <t2>[ i N més]."
- New permissions: `READ_CALENDAR`, `USE_EXACT_ALARM`. Still **no** network permission.
- The master switch (`Prefs.speechEnabled`) and the call guard (audio mode must be normal) apply to every kind of speech except the setup screen's test buttons.
- `versionCode 3`, `versionName "0.2.0"`.

## Review Focus

1. **An event after midnight within the window** must say "Demà", not "Avui". Tested in Task 2 (`describeTomorrow`).
2. **All-day instances are stored at UTC midnight.** A naive local conversion shifts them a day in UTC+1/+2. `CalendarSource` must use UTC for all-day dates. Tested in Task 4 (`allDayDateUsesUtc`, a pure helper).
3. **Missing permission or no calendar selected** must leave the app silent about events: no crash, and time-only announcements keep working. Tested in Task 4 (`CalendarSource` catches `SecurityException`) and Task 3 (empty list → null).
4. **A reminder time that has already passed** is never scheduled, and the planner picks the next future one. Tested in Task 3 (`skipsPastReminder`).
5. **Recurring events** come through as several instances, and each gets its own reminder. Tested in Task 3 (`twoInstancesTwoReminders`).

---

### Task 1: `atPhrase` (RP-19)

**Files:** Modify `app/src/main/java/cat/rellotgeparlant/time/CatalanTimeFormatter.kt`; Test `app/src/test/java/cat/rellotgeparlant/time/CatalanTimeFormatterTest.kt`

**Interfaces:** Produces `CatalanTimeFormatter.atPhrase(hour: Int, minute: Int): String`, with the same range checks as `phrase`.

- [ ] **Step 1: Failing tests** `atPhraseExamples`:
  17:30 → "a dos quarts de sis de la tarda", 13:00 → "a la una del migdia", 17:00 → "a les cinc de la tarda", 21:15 → "a un quart de deu del vespre", 16:55 → "a les cinc menys cinc de la tarda", 17:12 → "a un quart menys cinc de sis de la tarda", 12:05 → "a les dotze i cinc del migdia".
  Also `atPhraseMatchesPhraseForAllMinutes`: for every minute, `phrase(h,m)` without its "És "/"Són " prefix equals `atPhrase(h,m)` without "a " and with the final "." restored.
- [ ] **Step 2:** `./gradlew -q testDebugUnitTest --tests '*CatalanTimeFormatterTest*'`. Expected: compilation FAIL.
- [ ] **Step 3:** Refactor `phrase` into a shared private `body(hour, minute): Pair<String /*body without verb*/, Boolean /*plural*/>`. `phrase` = verb + body + ".", and `atPhrase` = "a " + body.
- [ ] **Step 4:** Run. Expected: PASS (all old formatter tests too).
- [ ] **Step 5:** Commit `feat: atPhrase for event times`.

### Task 2: Event model and text (RP-19)

**Files:** Create `app/src/main/java/cat/rellotgeparlant/calendar/CalendarEvent.kt`, `calendar/EventText.kt`; Test `app/src/test/java/cat/rellotgeparlant/calendar/EventTextTest.kt`

**Interfaces:**
- Produces `data class CalendarEvent(val start: java.time.LocalDateTime, val allDay: Boolean, val title: String?, val location: String?, val description: String?)`. For all-day events, `start` is the local date at 00:00.
- Produces `data class EventSettings(val hoursAhead: Int = 6, val reminderMinutes: Int = 60, val morningTime: java.time.LocalTime = LocalTime.of(9, 0), val readLocation: Boolean = true, val readDescription: Boolean = true)`.
- Produces `object EventText`:
  - `fun cleanDescription(raw: String?): String?`
  - `fun describe(e: CalendarEvent, today: LocalDate, s: EventSettings): String`
  - `fun upcomingSentence(events: List<CalendarEvent>, today: LocalDate, s: EventSettings): String?` (null if empty)
  - `fun morningSentence(allDay: List<CalendarEvent>): String?` (null if empty)

- [ ] **Step 1: Failing tests** in `EventTextTest`:
  - `describeFull`: 17:30 "Metge de capçalera", location "CAP de Gràcia", description "Porta la targeta sanitària i les anàlisis. https://x.y/z", today = same date → "Avui, a dos quarts de sis de la tarda: Metge de capçalera. Lloc: CAP de Gràcia. Porta la targeta sanitària i les anàlisis."
  - `describeTomorrow`: event at 00:30 on today+1 → starts "Demà, a dos quarts d'una de la nit: "
  - `describeRespectsSwitches`: readLocation=false and readDescription=false → "Avui, a les cinc de la tarda: Metge."
  - `emptyTitle`: title "  " → contains ": un esdeveniment."
  - `titleKeepsOwnFullStop`: title "Dinar." → "…: Dinar." (no "..")
  - `cleanStripsHtmlAndLinks`: "<p>Hola <b>iaia</b></p> www.x.com mira https://a.b" → "Hola iaia mira"
  - `cleanTwoSentencesMax`: "U. Dos. Tres." → "U. Dos."
  - `cleanMax200`: 500 'a' chars → length ≤ 200, ending with "…"
  - `cleanBlankIsNull`: "  <br> " → null
  - `upcomingLimitsToThree`: 5 events → three descriptions joined with " " + " I 2 més." Exact: the result ends with "I 2 més."
  - `morningSentence`: titles "Aniversari de la Maria", "Festa" → "Bon dia. Avui: Aniversari de la Maria, Festa."
- [ ] **Step 2:** Run `--tests '*EventTextTest*'`. Expected: compilation FAIL.
- [ ] **Step 3:** Implement. Notes:
  - Strip trailing "." from titles before adding ".".
  - The URL regex covers `https?://\S+` and `www\.\S+`.
  - A sentence split is on `(?<=[.!?])\s+`.
  - The 200-char cut ends with "…".
- [ ] **Step 4:** Run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: event text for calendar announcements`.

### Task 3: Upcoming events and reminder planner (RP-20)

**Files:** Create `calendar/UpcomingEvents.kt`, `calendar/ReminderPlanner.kt`; Tests `calendar/UpcomingEventsTest.kt`, `calendar/ReminderPlannerTest.kt`

**Interfaces:**
- Consumes `CalendarEvent`, `EventSettings`, `EventText` (Task 2).
- Produces `object UpcomingEvents { fun select(events: List<CalendarEvent>, now: LocalDateTime, hoursAhead: Int): List<CalendarEvent> }`. It keeps timed events with `now < start <= now + hoursAhead h`, sorted by start.
- Produces `data class PlannedSpeech(val at: LocalDateTime, val text: String)` and `object ReminderPlanner { fun next(events: List<CalendarEvent>, now: LocalDateTime, s: EventSettings): PlannedSpeech? }`.

- [ ] **Step 1: Failing tests**:
  - `UpcomingEventsTest`:
    - `windowBounds`: now 10:00, hours 6, events at 09:59, 10:00, 10:01, 16:00, 16:01 → [10:01, 16:00]
    - `excludesAllDay`
    - `sorted`
    - `emptyGivesEmpty`
  - `ReminderPlannerTest`, with now = 2026-11-22T10:00 and defaults:
    - `reminderOneHourBefore`: event 12:00 → at 11:00, text "Recordatori: Avui, a les dotze del migdia: Metge."
    - `skipsPastReminder`: events 10:30 (reminder 09:30 is past) and 14:00 → at 13:00
    - `twoInstancesTwoReminders`: events 12:00 and 15:00 → next is 11:00, and with now=11:00:01 the next is 14:00
    - `reminderOffMeansOnlyMorning`: reminderMinutes=0 and no all-day events → null
    - `morningToday`: now 08:00, all-day event dated today → at 09:00 today, text "Bon dia. Avui: Aniversari."
    - `morningTomorrowWhenPassed`: now 10:00, all-day event tomorrow → at tomorrow 09:00
    - `noMorningWithoutAllDay`: all-day event on another day than the next morning → it isn't used for that morning
    - `earliestWins`: morning 09:00 tomorrow vs reminder 13:00 today → 13:00
    - `emptyIsNull`
- [ ] **Step 2:** Run `--tests '*UpcomingEventsTest*' --tests '*ReminderPlannerTest*'`. Expected: compilation FAIL.
- [ ] **Step 3:** Implement. The planner considers:
  - reminder candidates `start - M` with `> now`, for timed events
  - the next morning slot (`today@morningTime` if `> now`, else tomorrow), if any all-day event falls on that date
  It returns the earliest. Reminder text uses `today = at.toLocalDate()`.
- [ ] **Step 4:** Run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: upcoming-events window and reminder planner`.

### Task 4: Calendar reading, announcements, reminders (RP-21)

**Files:**
- Create `calendar/CalendarSource.kt`, `calendar/ReminderScheduler.kt`, `calendar/ReminderReceiver.kt`
- Modify `service/TimeAnnouncer.kt`, `service/SpeakerService.kt`, `service/Prefs.kt`, `service/BootReceiver.kt`, `AndroidManifest.xml`
- Tests `calendar/CalendarDatesTest.kt`, `service/TimeAnnouncerTest.kt`

**Interfaces:**
- Produces:
  - `object CalendarDates { fun allDayDate(utcMillis: Long): LocalDate; fun timedStart(millis: Long, zone: ZoneId): LocalDateTime }` (pure)
  - `class CalendarSource(context) { fun calendars(): List<Pair<Long, String>>; fun events(calendarId: Long, from: LocalDateTime, to: LocalDateTime): List<CalendarEvent> }`. It returns empty on `SecurityException` or when `calendarId < 0`.
  - `object ReminderScheduler { fun reschedule(context: Context) }`, reading the next 48 h
- Produces `Prefs`: `calendarId(ctx): Long` (default -1), `eventSettings(ctx): EventSettings`, and the matching setters.
- Changes `TimeAnnouncer(clock, speaker, events: () -> String? = { null })`. `speakNow()` says the time phrase, then `" " + events()` when that's non-null.
- Produces `SpeakerService.ACTION_SPEAK_TEXT` with extra `EXTRA_TEXT`, and `ACTION_SPEAK_EVENTS_TEST`.

- [ ] **Step 1: Failing tests**:
  - `CalendarDatesTest.allDayDateUsesUtc`: `allDayDate(1764028800000 /*2025-11-25T00:00Z*/)` == 2025-11-25, with the JVM default zone set to America/Los_Angeles during the test (restored after)
  - `timedStart` converts with the given zone
  - `TimeAnnouncerTest.appendsEvents`: an announcer with `events = { "Avui, …" }` speaks "<time phrase> Avui, …"
  - `TimeAnnouncerTest.noEventsJustTime`: an announcer with events returning null speaks the time phrase only
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement:
  - `CalendarSource`: query `Instances.CONTENT_URI` (`Instances.query`-style URI with begin/end millis) with `CALENDAR_ID = ?`, projecting `BEGIN, ALL_DAY, TITLE, EVENT_LOCATION, DESCRIPTION`.
  - Service: the events provider = `EventText.upcomingSentence(UpcomingEvents.select(source.events(...now..now+hours), now, hours), today, settings)`. Register a `ContentObserver` on `CalendarContract.Events.CONTENT_URI` and call `ReminderScheduler.reschedule` on change. Also reschedule in `onCreate`.
  - `ACTION_SPEAK_TEXT`: speak `EXTRA_TEXT` if `speechEnabled` and audio mode is normal, then reschedule.
  - `ReminderScheduler`: computes `ReminderPlanner.next` and sets `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, …)` with a `PendingIntent.getBroadcast` (FLAG_IMMUTABLE|FLAG_UPDATE_CURRENT, single request code). If the planner returns null, it cancels.
  - `ReminderReceiver` calls `SpeakerService.ensureRunning(ctx, ACTION_SPEAK_TEXT)` with the text extra.
  - `BootReceiver` also reschedules.
  - Manifest: add the permissions and the receiver (not exported).
  - Change `SpeakerService.ensureRunning(context, action, text: String? = null)`.
- [ ] **Step 4:** Run the full suite, then `assembleDebug lintDebug`. Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `feat: read calendar events on wake/lock and schedule reminders`.

### Task 5: Voice volume (RP-22)

**Files:** Modify `speech/TtsEngine.kt`, `speech/Speaker.kt`, `speech/AndroidTtsEngine.kt`, `service/Prefs.kt`, `service/SpeakerService.kt`; Test `speech/SpeakerTest.kt`, `speech/FakeTtsEngine.kt`

**Interfaces:** Produces `TtsEngine.setVolume(volume: Float)`, `Speaker.setVolume(volume: Float)` (re-applied after init, like the rate), `Prefs.voiceVolume/setVoiceVolume` (default 1.0f), and `SpeakerService.ACTION_VOLUME_CHANGED`.

- [ ] **Step 1: Failing tests** `volumeSetBeforeReadySurvivesInit` (the fake resets volume to null on ready, like the rate) and `volumeIsForwarded`.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement. `AndroidTtsEngine.speak` passes `Bundle().apply { putFloat(Engine.KEY_PARAM_VOLUME, volume) }`.
- [ ] **Step 4:** Run. Expected: PASS.
- [ ] **Step 5:** Commit `feat: voice volume`.

### Task 6: Setup screen (RP-22)

**Files:** Modify `ui/MainActivity.kt`, `res/layout/activity_main.xml`, `res/values/strings.xml`, `setup/SetupChecks.kt`, `app/build.gradle.kts` (version); Test `setup/SetupLogicTest.kt`

**Interfaces:**
- Consumes `Prefs` (Tasks 4–5), `CalendarSource.calendars()`, `SpeakerService.ACTION_SPEAK_EVENTS_TEST`, `ACTION_VOLUME_CHANGED`.
- Produces `CheckId.CALENDAR_PERMISSION`, `CheckId.ALARM_VOLUME`, and `fun alarmVolumeOk(current: Int, max: Int): Boolean` (pure, ≥ 50 %).

- [ ] **Step 1: Failing test** `SetupLogicTest`: `alarmVolumeOk(4, 7)` true, `alarmVolumeOk(3, 7)` false, `alarmVolumeOk(0, 0)` false.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement the UI:
  - the volume slider (10–100 %) under the speed slider
  - a "Calendari" section with:
    - a permission request on first open
    - a calendar `Spinner` (with a "Cap" entry = -1)
    - hours `SeekBar` 1–24
    - a reminder `Spinner` (No / 15 / 30 / 60 / 120 min)
    - a morning-time button that opens a `TimePickerDialog`
    - two switches, location and description
    - a "Prova el calendari" button
  - every change saves `Prefs` and calls `ReminderScheduler.reschedule`
  - checklist rows for calendar permission (fix: app settings) and alarm volume (fix: `Settings.ACTION_SOUND_SETTINGS`)
  - all strings in Catalan
  - bump the version to 3 / "0.2.0"
- [ ] **Step 4:** Run the full suite plus `assembleDebug lintDebug`. Expected: PASS. Render the screen with the existing Robolectric/Roborazzi scratch setup if available.
- [ ] **Step 5:** Commit `feat: calendar and volume settings on setup screen`.

### Task 7: Device test (RP-23, manual)

- [ ] Create the "Iaia" calendar and share it with the sons. On her phone, check that it syncs, then select it in the app.
- [ ] Add an event 70 min ahead. Expect the wake/lock announcement to include it and a reminder at T-60.
- [ ] Add an all-day event for tomorrow. Expect "Bon dia…" at 09:00.
- [ ] Turn the volume slider down to 30 % and press Prova. Expect it to be quieter.
