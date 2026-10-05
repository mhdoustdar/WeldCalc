# WeldCalc Industrial

Android application for resistance spot-welding parameter calculation based on the supplied PSA PEUGEOT-CITROËN standard **E34.03.180.G** (file revision shown in the document: OR 01/03/2001, E 10/02/2015).

## Implemented engineering scope
- §4.1 reference thickness for 2 and 3 sheets.
- §4.2 retained thickness mapping from 0.55–3.00 mm.
- §5.1 Table 4: continuous rod mill, 50 Hz, Ø5 mm Class N, F1.
- §5.2.1 Tables 5–7: continuous rod mill, 50 Hz, Ø6 mm Class N, F1/F2/F2bis.
- §5.2.2 coating >10 µm increment rule (+0.5 kA, +2 cycles), explicitly flagged for engineering validation.
- §5.3 Tables 8–10: 50 Hz pulsation, Ø8 mm, F1/F2/F2bis.
- §5.5 60 Hz rule: welding time +20%, intensity unchanged.
- §5.4 four-thickness assemblies: reference-thickness calculation and mandatory feasibility/engineering-validation warning; no fabricated parameter table.
- §5.7 Tables 13–16: screws, standard nuts, mass nuts, and embossment parameters.
- Hard-stop/warning behavior for out-of-table combinations and the standard's six-coating prohibition.
- Persian RTL interface.

## Important engineering behavior
The application does **not** extrapolate a welding current/force/time when the supplied standard has no tabulated value. It returns a controlled error or warning instead.

For 3-sheet assemblies, E is calculated from the average real sheet thickness and then mapped through §4.2. For standard continuous welding, the time branch follows the table's second/third-sheet thickness thresholds. For pulsation, current is selected from strongest-sheet and finest-sheet table axes.

## Build
Open the `WeldCalc-Industrial` folder in Android Studio with JDK 17. The project uses modern Jetpack Compose. Current Android guidance lists Compose 1.12.x as stable and the August 2026 release raised the compile SDK requirement to API 37; this project therefore uses compileSdk 37 and AGP 9.1.2.

The execution environment used to generate this project did not contain an Android SDK/Gradle installation, so an APK was not fabricated. The calculation engine itself was compiled and exercised independently with the Kotlin compiler.

## Source traceability
The supplied standard is included at `docs/E34.03.180_G_Eng.pdf`. All parameter tables are encoded in `app/src/main/java/com/example/weldcalc/data/StandardData.kt` and the calculation rules are isolated in `engine/WeldingCalculator.kt` for audit/review.

## Build APK from an Android phone (GitHub Actions)

This project includes `.github/workflows/build-apk.yml` and does not require Android Studio on the phone.

1. Create a GitHub repository named `WeldCalc`.
2. Upload the contents of this project to the repository root (not the ZIP file itself).
3. Open the repository's **Actions** tab.
4. Select **Build WeldCalc APK**.
5. Press **Run workflow** (or push to `main` once; the workflow also runs automatically on pushes).
6. When the run finishes successfully, open the run and download the **WeldCalc-APK** artifact.
7. Extract the downloaded artifact and install `WeldCalc.apk` on Android.

The workflow uses JDK 17, Android API 37 and Gradle 9.3.1 on the GitHub runner. The generated APK is a debug-signed APK intended for direct installation/testing; no release keystore is required.
