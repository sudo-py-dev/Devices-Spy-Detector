# NoTrace 🛡️

![NoTrace Hero Banner](assets/images/hero_banner.png)

**NoTrace** הוא כלי עוצמתי להגנה על הפרטיות ב-Android, שנועד לזהות מכשירי ריגול מוסתרים, תגיות מעקב (Trackers) ומכשירי Bluetooth לא מורשים בסביבתכם. באמצעות ניתוח אותות מתקדם ומסד נתונים של חתימות מעקב מוכרות, NoTrace עוזר לכם להישאר בטוחים ומודעים לסביבתכם.

[English README](README.md)

---

## ✨ תכונות עיקריות

- **🔍 סריקה חכמה**: מזהה AirTags, Samsung SmartTags, Tile trackers ומכשירי ריגול מבוססי Bluetooth אחרים.
- **📡 תצוגת רדאר**: משוב ויזואלי בזמן אמת של מכשירים קרובים עם מחווני עוצמת אות.
- **🕰️ היסטוריית זיהוי**: שומר יומן מקומי ומאובטח של כל המכשירים שזוהו לצורך סקירה מאוחרת יותר.
- **🌙 ערכות עיצוב מרובות**: תמיכה בעיצוב בהיר, כהה וברירת מחדל של המערכת עם עיצוב Material 3 יוקרתי.
- **🌍 ריבוי שפות**: תרגום מלא לאנגלית, עברית, גרמנית ורוסית.
- **🔋 אופטימיזציה לסוללה**: סריקת רקע עם השפעה מינימלית על הסוללה באמצעות שירותי חזית (Foreground Services).
- **🛡️ פרטיות תחילה**: כל הנתונים נשמרים מקומית על המכשיר שלכם באמצעות העדפות מוצפנות. שום מידע לא עוזב את הטלפון שלכם.

---

## 🚀 איך זה עובד

NoTrace משתמש באסטרטגיית זיהוי רב-שכבתית:

1. **התאמת מזהה יצרן (Manufacturer ID)**: מזהה מכשירים לפי מזהי היצרן הרשומים ב-Bluetooth SIG.
2. **זיהוי תבניות שם**: סורק אחר מוסכמות שמות ספציפיות המשמשות מכשירי מעקב פופולריים.
3. **ניתוח עוצמת אות**: עוקב אחר RSSI (מחוון עוצמת אות שהתקבל) כדי לעזור לכם לאתר את המכשיר הפיזי.

---

## 🛠️ טכנולוגיות

- **שפה**: [Kotlin](https://kotlinlang.org/)
- **ממשק משתמש**: [Material Design 3](https://m3.material.io/)
- **הזרקת תלויות**: [Koin](https://insert-koin.io/)
- **מסד נתונים**: [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **סריקה**: [Nordic Semiconductor Android Scanner Compat Library](https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library)
- **אבטחה**: [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)

---

## 📥 התקנה

1.  **שכפול המאגר**:
    ```bash
    git clone https://github.com/sudo-py-dev/Devices-Spy-Detector.git
    ```
2.  **בניית הפרויקט**:
    פתחו את הפרויקט ב-Android Studio או השתמשו ב-Gradle:
    ```bash
    ./gradlew assembleDebug
    ```
3.  **התקנה על המכשיר**:
    ```bash
    ./gradlew installDebug
    ```

---

## ⚠️ הצהרת אחריות

NoTrace הוא כלי עזר לזיהוי ועלול לעיתים לדווח על זיהויים שגויים (למשל, משקפי VR, מכשירי בית חכם או ציוד היקפי אחר של Bluetooth). המשתמשים מצופים להשתמש בתוצאות באחריות ובהתאם לחוקי הפרטיות המקומיים.

---

## 📄 רישיון

פרויקט זה מופץ תחת רישיון [MIT License](LICENSE).

---

<p align="center">
  פותח באהבה ❤️ על ידי sudopdev
</p>
