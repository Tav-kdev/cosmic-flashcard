# Cosmic Flashcards — Android

A native Android flashcard app: Kotlin, Jetpack Compose, Room/SQLite, SM-2
spaced repetition. Fully offline — no server, no account, no permissions.

This is a port of the Django web app, not a wrapper around it. The scheduler,
import parser and duplicate detection are the same logic, rewritten in Kotlin
and verified to behave identically (see **Testing**).

## Build it

You need **Android Studio** (Ladybug or newer) or a JDK 17+ with the Android
SDK. Everything else downloads on first build.

```bash
./gradlew assembleDebug
```

The APK lands at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Copy it to your phone and open it. You'll need to allow installs from your
file manager or browser the first time — Android prompts for this.

Or, with the phone plugged in and USB debugging on:

```bash
./gradlew installDebug
```

In Android Studio: open this folder, let Gradle sync, press Run.

### If the build can't find the SDK

Create `local.properties` in this folder:

```properties
sdk.dir=/Users/you/Library/Android/sdk      # macOS
# sdk.dir=C:\\Users\\you\\AppData\\Local\\Android\\Sdk   # Windows
# sdk.dir=/home/you/Android/Sdk             # Linux
```

Android Studio writes this for you automatically.

## What's in it

| Screen | What it does |
|---|---|
| Command Center | Live counts, recent decks, popular tags, 14-day activity strip |
| Card Vault | Search, filter by deck/tag/state, sort, inline answer reveal |
| Card editor | Deck picker, tag toggles, inline new tags, duplicate blocking |
| Decks | Emoji + accent picker with live preview |
| Tags | Cross-cutting labels, independent of decks |
| Import | Paste anything; live preview flags duplicates before you commit |
| Study | Normal (SM-2 due queue) and Exam (fixed set) |

**Normal mode** works the due queue in due-date order. Anything you mark
*Again* comes back at the end of the same run.

**Exam mode** takes a fixed set — any deck, tag, or everything, due or not —
and runs straight through without repeats. Answers still update each card's
schedule.

Tap the card or **Reveal answer** to flip it; the four grade buttons show the
interval each answer would schedule.

## Moving your cards over from the web app

The Django project has an export command that writes the format this app's
Import screen reads:

```bash
python manage.py export_cards --by-deck -o exported_decks/
```

That writes one file per deck. Open Import in the app, pick the matching deck,
paste a file's contents, and hit Import.

Both sides fingerprint a card by its text (whitespace- and case-normalised,
SHA-256), and the two implementations produce **byte-identical hashes** — so
pasting the same file twice is safe, and cards you already have are skipped
rather than duplicated.

## Architecture

```
domain/     Pure Kotlin. No Android imports. Scheduler, ImportParser,
            ContentHash, StatsCalc — all unit-tested on the JVM.
data/       Room entities, DAOs, Repository, first-run seeder.
ui/         Compose theme, shared components, screens, navigation.
vm/         ViewModels exposing StateFlow, built by a manual factory.
```

Some notes on the choices:

- **Scheduling state is embedded in the card row**, not a separate 1:1 table
  like the web app's `MasteryState`. It's always needed alongside the card, so
  the join bought nothing.
- **Timestamps are epoch millis (`Long`)**, so there are no Room type
  converters to get wrong and SQLite sorts them directly.
- **No Hilt.** One database, one repository, seven ViewModels — a DI framework
  would add an annotation processor and generated code for no benefit here.
- **Orbitron is bundled** for the display face (the wordmark, the big numbers).
  Body text uses the platform font, which keeps it feeling native and avoids
  shipping a second family.

## Testing

Pure-logic tests run on the JVM, no emulator:

```bash
./gradlew test
```

63 assertions covering SM-2 arithmetic (including the ease-factor floor and
the reset-on-miss path), all six import formats plus their edge cases, hash
normalisation, streak counting across gaps, and the activity strip.

The content-hash cases are pinned to values produced by the Django app, so if
the two ever drift apart the test fails.

## Two things worth knowing

**SM-2 compresses short intervals.** On a card with a small interval, *Hard*,
*Good* and *Easy* can all show the same number of days. That's real SM-2
arithmetic, not a bug, but it reads as broken. If it bothers you, add separate
per-grade multipliers (what Anki does) in `domain/Scheduler.kt::apply` —
nothing else changes, and the tests will tell you if you break the rest.

**Due counts advance with the database, not the clock.** They update
immediately when you grade, add or delete a card. But a card that becomes due
purely through the passage of time won't appear in the count until the screen
is revisited. Reopening the app always shows the truth.

## Release builds

The debug APK is what you sideload; it needs no signing setup. For a signed
release you'd add a keystore and a `signingConfigs` block to
`app/build.gradle.kts`, then `./gradlew assembleRelease`. Minification and
resource shrinking are already enabled for that build type.

## Versions

Gradle 8.9 · AGP 8.7.3 · Kotlin 2.0.21 · Compose BOM 2024.12.01 · Room 2.6.1
· compileSdk 35 · minSdk 26 (Android 8.0)
