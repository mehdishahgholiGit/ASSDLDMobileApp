# Barcode Scanner (Android)

A native Android app that:
1. Scans **QR codes and 2D barcodes** (Data Matrix, Aztec, PDF417) live with the camera.
2. Lists every unique code scanned, with its format and timestamp.
3. Exports the list to a **CSV file** and opens the Android share sheet so you can **send it as an email attachment** using whatever mail app is installed (Gmail, Outlook, etc.) — no SMTP credentials needed.

## How it works

- **Camera + detection**: CameraX streams frames to an `ImageAnalysis.Analyzer` (`BarcodeAnalyzer.kt`), which runs Google's on-device **ML Kit Barcode Scanning** API. Only `QR_CODE`, `DATA_MATRIX`, `AZTEC`, and `PDF417` formats are enabled — add more `Barcode.FORMAT_*` constants there if you also want 1D formats like Code128 or EAN-13.
- **Dedup + list**: Each detected code is keyed by `format:value` so the same sticker isn't added on every frame while it's in view. Results show in a `RecyclerView` (`ScanAdapter.kt`).
- **CSV export**: `CsvExporter.kt` writes `Value,Format,Timestamp` rows (properly quoted/escaped, with a UTF‑8 BOM so Excel opens non-Latin text correctly) to the app's cache directory.
- **Email attachment**: The CSV is exposed via a `FileProvider` (`res/xml/file_paths.xml`) and attached to an `Intent.ACTION_SEND` with `type = "text/csv"`. Android's chooser lets the user pick Gmail/Outlook/etc. and the file arrives as a real attachment.

## Project structure

```
BarcodeScanner/
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/barcodescanner/
        │   ├── MainActivity.kt        # camera lifecycle, permission, buttons
        │   ├── BarcodeAnalyzer.kt      # ML Kit CameraX analyzer
        │   ├── ScanAdapter.kt         # RecyclerView adapter + ScanRecord model
        │   └── CsvExporter.kt        # writes the CSV file
        └── res/
            ├── layout/activity_main.xml, item_scan.xml
            ├── values/strings.xml, themes.xml, colors.xml
            ├── xml/file_paths.xml
            └── mipmap-anydpi-v26 / drawable  (launcher icon)
```

## Opening and running it

1. **Requirements**: Android Studio (Koala/2024.1 or newer recommended), an Android device or emulator running **API 24+** with a camera.
2. Open the `BarcodeScanner/` folder in Android Studio ("Open" → select this folder). It will detect the Gradle wrapper config and offer to download Gradle 8.7 automatically — accept it, then let Gradle sync (this downloads AndroidX, CameraX, and ML Kit dependencies, so an internet connection is needed the first time).
3. Connect a physical device (emulators technically support a virtual camera but a real device scanning real barcodes is much more reliable) and click **Run ▶**.
4. Grant the camera permission when prompted.
5. Point the camera at QR/2D codes — each new one appears in the list below the preview.
6. Tap **Clear** to reset the session, or **Export & Email CSV** to generate the CSV and open the share sheet.

## Notes / things you may want to customize

- **Formats scanned**: currently QR + Data Matrix + Aztec + PDF417 (the common "2D" symbologies). To also catch 1D barcodes (Code128, EAN-13, UPC-A, etc.), add the corresponding `Barcode.FORMAT_*` values in `BarcodeAnalyzer.kt`.
- **Duplicate handling**: identical codes are only added once per app session (cleared by the Clear button). If you want every single scan logged separately (e.g. for counting how many times an item passed by), remove the `seenKeys` dedup check in `MainActivity.handleBarcodes`.
- **No SMTP/API keys required**: the app doesn't send email directly — it hands the CSV to whichever mail app the user has, via the system share sheet. If you instead want the app to send email silently in the background to a fixed address (no user interaction), that requires either an SMTP library with stored credentials, or a backend service — let me know if you'd like that version instead.
- **App icon**: a placeholder blue "barcode lines" icon is included; swap `res/drawable/ic_launcher_foreground.xml` and `res/values/colors.xml` for your own branding.
- **Permissions**: only `CAMERA` is requested. No storage permission is needed because the CSV is written to the app's private cache dir and shared via `FileProvider`.
