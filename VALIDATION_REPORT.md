# Validation report

## Independent calculation-engine smoke tests

The non-Android calculation layer was compiled with `kotlinc` successfully.

Verified cases:
1. F1, Ø6 continuous, 0.8 + 1.0 mm, 2 coated faces, 50 Hz → E=0.8, 10.0 kA, 230 daN, 8 cycles, hold 8.
2. Same case at 60 Hz → current remains 10.0 kA and weld time becomes 10 cycles, matching §5.5.
3. F1, Ø8 pulsation, 1.5 + 2.0 mm, 50 Hz → 13.0 kA, 400 daN, 4(4+1), hold 12.
4. F2, Ø8 pulsation, 1.5 + 2.0 mm, 50 Hz → 11.7 kA, 500 daN, 4(5+1), hold 15.

The Android UI itself was not compiled in this environment because an Android SDK/Gradle installation is not present. The project is configured for Android Studio/JDK 17.
