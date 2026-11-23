<p align="center">
  <img src="app/src/main/res/drawable/mascot1.png" width="128" alt="Bunker URL Test Mascot">
</p>

<h1 align="center">BUNKER URL TEST (v1.1.2 Universal)</h1>

<p align="center">
  <b>Автономный монитор доступности интернет-сервисов и цензуры ТСПУ для Android</b><br>
  Часть экосистемы <a href="https://github.com/ANT1VEN0M"><b>ANT1VEN0M / Bunker VPN</b></a>.
</p>

<p align="center">
  <a href="https://github.com/ANT1VEN0M/Bunker-Url-Test/releases/latest"><b>Скачать APK (v1.1.2)</b></a> ·
  <a href="https://ghfast.top/https://github.com/ANT1VEN0M/Bunker-Url-Test/releases/download/v1.1.2/Bunker-Url-Test-v1.1.2-universal.apk"><b>Зеркало РФ (ghfast)</b></a> ·
  <a href="https://t.me/BUNKER_KOTA">Telegram Канал</a> ·
  <a href="https://t.me/BUNKER_K0TA">Чат Бункер Кота</a>
</p>

---

## 🌟 Возможности

- ⚡ **Мгновенный мониторинг сервисов:** Параллельная сверхбыстрая проверка доступности ключевых узлов сети (Google, Telegram, Instagram, YouTube) по умолчанию + мгновенное добавление любых кастомных хостов.
- 📱 **Проверка установленных приложений:** Автоматическое определение хостов для популярных приложений и эвристический подбор доменов для остальных.
- 🔍 **Поиск по приложениям (Searchable App Picker):** Быстрая фильтрация установленных на телефоне приложений с удобным мультивыбором.
- 🎛️ **Quick Settings Tile:** Переключатель мониторинга прямо в шторке быстрых настроек Android.
- 📊 **Home Screen Widget:** Информативный виджет на рабочий стол с цветной индикацией доступности и временем отклика (пинг в ms).
- 🛡️ **Полная совместимость с Android 7.0–17 (API 24–35+):** Корректная работа Foreground Service (`specialUse`) без сбоев фонового процесса.
- 🛠️ **Кастомные цели:** Добавление любых собственных URL и доменов для мониторинга корпоративных серверов, сайтов или прокси.

---

## 🚀 Что нового в версии v1.1.2 Universal

1. 📐 **Полноэкранный дизайн Edge-to-Edge:**
   - Полностью устранено наложение шапки приложения на системную шторку (Status Bar / Display Cutout).
   - Нижние кнопки управления больше не перекрываются панелью жестов / навигации Android.
   - Использован современный `WindowCompat.setDecorFitsSystemWindows(window, false)` и `ViewCompat.setOnApplyWindowInsetsListener`.

2. ⚡ **Компактный и быстрый список по умолчанию:**
   - Список мониторинга по умолчанию оптимизирован до **4 самых популярных сервисов** (Google, Telegram, Instagram, YouTube).
   - Пользователи могут добавлять любые другие хосты вручную через поле ввода.

3. 🏎️ **Параллельные асинхронные проверки (Kotlin Coroutines):**
   - Устранены зависания на «Ожидание проверки...»: все хосты проверяются одновременно через `async`/`awaitAll`.
   - Жёсткий таймаут 2500 мс с заголовком `Connection: close` гарантирует мгновенную отдачу без накопления зависших сокетов.
   - Карточки обновляются «на лету» без мерцания и пересоздания списка.

4. 🔑 **Постоянный Release Keystore & Universal Target:**
   - Сгенерирован и подключён постоянный цифровой сертификат выпуска (срок действия до 2054 года).
   - Включена комбинированная цифровая подпись **V1 (JAR) + V2 (Full APK Signature)** для установки поверх предыдущих версий.
   - Минимальная версия снижена до Android 7.0 (`minSdk = 24`), целевая `targetSdk = 34`.
   - Полная универсальная поддержка всех архитектур процессоров: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`.

---

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/ANT1VEN0M/Bunker-Url-Test.git
cd Bunker-Url-Test
./gradlew assembleRelease
```

Собранный универсальный APK появится в папке `app/build/outputs/apk/release/`.

---

## 🤝 Сообщество и поддержка

- Разработчик: **Alexander Petrashkevich** (`KOT ANTIDOT` / `@k0tlux` / `@Titandarkness`)
- Организация: **[ANT1VEN0M](https://github.com/ANT1VEN0M)**
- Канал: **[@BUNKER_KOTA](https://t.me/BUNKER_KOTA)** · Чат: **[@BUNKER_K0TA](https://t.me/BUNKER_K0TA)**
