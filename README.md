# Departure Checklist

An Android app that surfaces your upcoming Google Calendar events and lets you attach a to-do checklist to each one — with an optional reminder alarm that fires before you leave.

## Download

Grab the latest APK from the [Releases](https://github.com/aliladak2007/departure-checklist/releases) page.

## What it does

- Reads your Google Calendar and lists events for the next 14 days
- Tap any event to open (or create) a checklist for it
- Add, check off, and delete items
- Set a reminder: pick how many minutes before the event you want a notification
- Notification taps deep-link straight back to the checklist
- Reminders survive device reboots

## Screenshots

_Coming soon_

## Tech stack

- **Java** throughout — no Kotlin
- **Room** for local persistence (checklists and checklist items)
- **Navigation Component** with Safe Args for fragment navigation and deep links
- **ViewModel + LiveData** for UI state
- **AlarmManager** (exact alarms) for departure reminders
- **Material 3** with dynamic colour support
- Single-activity architecture with `LauncherActivity` → `MainActivity` (nav host)

## Permissions

| Permission | Why |
|---|---|
| `READ_CALENDAR` | Load upcoming events from Google Calendar |
| `SCHEDULE_EXACT_ALARM` | Fire reminders at the right time |
| `POST_NOTIFICATIONS` | Show the departure reminder notification |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule alarms after reboot |

## Building

Requirements: Android Studio Hedgehog or later, JDK 17, Android SDK 34.

```bash
git clone https://github.com/aliladak2007/departure-checklist.git
cd departure-checklist
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/java/com/aliladak/departurechecklist/
├── data/
│   ├── db/                  Room database, DAOs, entities
│   ├── model/               CalendarEvent plain model
│   └── repository/          CalendarRepository, ChecklistRepository
├── notification/            DepartureScheduler, DepartureReceiver, BootReceiver, NotificationHelper
├── ui/
│   ├── checklist/           ChecklistFragment, ChecklistViewModel, ChecklistItemAdapter
│   ├── events/              EventListFragment, EventListViewModel, EventAdapter
│   └── common/              ViewModelFactory
└── util/                    CalendarPermissionHelper
```

## Requirements

- Android 8.1 (API 25) or higher
- Google Calendar installed and synced

## Licence

MIT
