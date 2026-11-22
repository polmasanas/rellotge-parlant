# Rellotge Parlant — Family calendar & voice volume (v0.2) — Design

*Date: 2026-11-22 · Status: awaiting approval · Builds on `2026-11-17-rellotge-parlant-design.md`*

## 1. Intent

Her sons want to remind her of things (doctor, visits, birthdays) without phoning her. They add
events to a shared calendar from their own phones, and the app reads them out loud in the
same natural Catalan as the time. Separately, the caregiver needs to be able to set how loud the voice is.

## 2. Goals, non-goals, anti-goals

**Goals**
- The sons add events from their own phones, with no new app for them.
- Upcoming events are read out with **the event's own details** (title, location, description),
  with the time said the traditional "quarts" way.
- Two moments to speak: **(a)** appended to the time announcement when she wakes or locks the phone,
  and **(b)** a proactive reminder shortly before each event.
- All-day events are announced **once, in the morning**.
- A **voice volume** slider on the setup screen.

**Non-goals**
- Creating or editing events from her phone, or any UI for her.
- Calendars other than the one selected by the caregiver.
- Letting her change the volume herself (the slider is on the setup screen only).

**Anti-goals**
- Reading out noise: web links, HTML, or a whole page of notes.
- Announcing a time **later** than the real one. Rounding is always down, so she's never told she has
  more time than she does.
- Talking during a call, or when the caregiver has switched speech off.

## 3. Approaches considered

| Approach | Trade-off | Decision |
|---|---|---|
| **Read the phone's synced Google Calendar** (Android `CalendarContract`) | No server, no login in the app, no network permission. Android already syncs her Google account. Needs one runtime permission (calendar read). | **Chosen** |
| App talks to the Google Calendar API itself | Needs an OAuth login and network access, adds a lot of complexity, and duplicates what Android already does. | Rejected |
| Custom backend or shared spreadsheet | Something to host and maintain, and the sons would need to learn a new tool. | Rejected |

## 4. Setup (once, by the caregiver)

1. In her Google account, create a calendar, e.g. **"Iaia"**. Share it with each son with
   **"Make changes to events"**.
2. On her phone, make sure that calendar is synced (Google account → Calendar sync on).
3. In the app's setup screen, grant the calendar permission and **choose the calendar** from a list.

## 5. Behaviour

### 5.1 Which events
- Read from the chosen calendar only, using event *instances* so recurring events work.
- **Timed events** count as "upcoming" when they start within the next **N hours** (default **6**,
  configurable 1–24) and haven't started yet.
- **All-day events** are never part of (a) or (b). They're announced once at the **morning time**
  (default **09:00**, configurable), on the day they occur.
- At most **3** events are read. Beyond that it says "…i N més".

### 5.2 What is said for an event
`<Avui|Demà>, <at-time>: <title>.[ Lloc: <location>.][ <description>]`

- **at-time** uses the quarts formatter, rounded down, without the verb:
  "a les cinc", "a la una", "a dos quarts de sis de la tarda", "a un quart de deu del vespre".
  It adds a new pure function, `CatalanTimeFormatter.atPhrase(hour, minute)`.
- **Avui/Demà**: an event within N hours can fall after midnight.
- **Title**: always read. If it's empty, say "un esdeveniment".
- **Location**: read if present and the caregiver hasn't turned it off.
- **Description**: read if present and the caregiver hasn't turned it off. It's cleaned first:
  HTML tags stripped, URLs removed, whitespace collapsed, then cut to the **first 2 sentences
  or 200 characters**, whichever is shorter.

Example (15:00, event at 17:30 "Metge de capçalera", location "CAP de Gràcia",
description "Porta la targeta sanitària i les anàlisis. https://…"):
> "Són les tres de la tarda. Avui, a dos quarts de sis de la tarda: Metge de capçalera. Lloc: CAP de
> Gràcia. Porta la targeta sanitària i les anàlisis."

*("Lloc: …" avoids getting Catalan contractions like "al / a la / a l'" wrong for arbitrary place
names. Revisit at the native-speaker review.)*

### 5.3 (a) On wake / lock
Every time the time is announced (wake on the lock screen, or screen off), any upcoming events are appended.
It uses the same guards as today: master switch on, not ringing, not in a call.

### 5.4 (b) Proactive reminder
- **M minutes** before each timed event (default **60**, configurable: off / 15 / 30 / 60 / 120), the
  phone speaks unprompted: "Recordatori: <event phrase>".
- Scheduled with `AlarmManager.setExactAndAllowWhileIdle`, using the `USE_EXACT_ALARM` permission. That's
  appropriate for a calendar-reminder app and is granted automatically when sideloaded.
- **Rescheduled** when: the calendar changes (content observer in the service), on boot or app update,
  after each reminder fires, and when settings change. Only the next reminder is scheduled at a time.
- Same guards: master switch, not in a call (a reminder due during a call is dropped).
- A reminder whose time has already passed (the event was added less than M minutes ahead) isn't spoken.
  (a) covers it.

### 5.5 All-day events in the morning
At the morning time: "Bon dia. Avui: <title>[, …]." It's scheduled the same way as reminders.

### 5.6 Volume
- **"Volum de la veu"** slider on the setup screen, 10–100 %, default 100 %. It's passed to TTS as
  `Engine.KEY_PARAM_VOLUME`, so it scales the voice within the alarm stream and affects nothing else.
- **Prova** uses it, so the caregiver hears the result.
- If the phone's **alarm volume is below 50 %**, the checklist shows ❌ "Volum d'alarma baix", with a button
  to open the sound settings, because the slider can't go louder than the alarm volume.

## 6. Setup-screen changes
- New **Calendari** section, containing:
  - a permission row with ✅/❌
  - a calendar picker
  - "Avisar amb N hores d'antelació"
  - "Recordatori abans" (off / 15 / 30 / 60 / 120 min)
  - "Hora de l'avís del matí"
  - switches "Llegir el lloc" and "Llegir la descripció"
  - a **"Prova el calendari"** button that reads the upcoming events now
- **Volum de la veu** slider, next to the speed slider.

## 7. Components (new or changed)
- `CatalanTimeFormatter.atPhrase` (pure, tested).
- `EventText` (pure, tested): description cleaning and building the event phrase from
  `(start, now, title, location, description, settings)`.
- `UpcomingEvents` (pure, tested): filters, orders and limits a list of event instances for a given `now`.
- `ReminderPlanner` (pure, tested): given instances, settings and `now`, returns the next time to
  speak (reminder or morning) and what to say.
- `CalendarSource`: reads `CalendarContract.Instances`. Android only, verified on a device.
- `ReminderReceiver` and scheduling in `SpeakerService`. Android only.
- `Speaker` gains a volume setting, which is re-applied after initialisation like the rate.
- Setup-screen additions.

## 8. Permissions
`READ_CALENDAR` (runtime), `USE_EXACT_ALARM`. Still **no network permission** in the app.

## 9. Risks
- **Sync on her phone is off or slow.** The checklist can show "last event seen" to help diagnose it.
- **Exact alarms on aggressive OEMs** (Samsung) can be delayed. The battery exemption already
  requested by the checklist mitigates this. Accept up to ~1 minute of delay.
- **Long or odd text from the sons.** Cleaning and the 2-sentence cap limit it. The sons get a
  one-line guideline: "Escriviu en català, el títol curt; els detalls a la descripció."
