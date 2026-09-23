# RSVP Reader

A minimal Rapid Serial Visual Presentation (RSVP) speed-reading app for Android.

Paste raw text or open a `.txt`, `.pdf`, or `.docx` file, and the app shows the
content one word at a time on a pure black background, with a highlighted focus
letter held at a fixed position to reduce eye movement.

## Features

- Word-by-word RSVP display with Spritz-style focus (ORP) letter
- Pure black distraction-free reading background
- Speed control from 100 to 1000 WPM with punctuation-aware pacing
- Color customization for text, background, and focus letter
- Drag-scrub progress bar, sentence skipping, restart, pause/resume
- Reads plain text, PDF (via Apache PDFBox), and Word `.docx` (parsed natively)
- Material You dynamic theming; settings persist across launches

## Building

Open the project in Android Studio, or from the project root:

```
./gradlew assembleDebug
```

The debug APK is output to `app/build/outputs/apk/debug/`.

## Requirements

- Android Studio (or the Android SDK command-line tools)
- JDK 17+
- Minimum supported Android version: 8.0 (API 26)
