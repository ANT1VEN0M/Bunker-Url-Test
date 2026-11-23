<p align="center">
  <img src="app/src/main/res/drawable/mascot1.png" width="128" alt="Bunker URL Test Mascot">
</p>

<h1 align="center">BUNKER URL TEST</h1>

<p align="center">
  <b>Автономный монитор доступности интернет-сервисов и цензуры ТСПУ для Android</b><br>
  Часть экосистемы <a href="https://github.com/ANT1VEN0M"><b>ANT1VEN0M / Bunker VPN</b></a>.
</p>

<p align="center">
  <a href="https://github.com/ANT1VEN0M/Bunker-Url-Test/releases/latest"><b>Скачать APK (v1.1.0)</b></a> ·
  <a href="https://t.me/k0tlux">Telegram Канал</a> ·
  <a href="https://t.me/+gI1vDkHn1m1lMDYy">Чат Бункер Кота</a>
</p>

---

## 🌟 Возможности

- ⚡ **Мгновенный мониторинг сервисов:** Непрерывная проверка доступности ключевых узлов сети (Google, Telegram, YouTube, Instagram, Discord, ChatGPT, Steam, Spotify, Cloudflare, X/Twitter).
- 📱 **Проверка установленных приложений:** Автоматическое определение хостов для более чем 50 популярных приложений и эвристический подбор доменов для остальных.
- 🔍 **Поиск по приложениям (Searchable App Picker):** Быстрая фильтрация сотен установленных на телефоне приложений с кнопками быстрого выбора.
- 🎛️ **Quick Settings Tile:** Переключатель мониторинга прямо в шторке быстрых настроек Android.
- 📊 **Home Screen Widget:** Информативный виджет на рабочий стол с цветной индикацией доступности и временем отклика (пинг в ms).
- 🛡️ **Полная совместимость с Android 14, 15, 16 и 17:** Корректная работа Foreground Service (`specialUse`) без сбоев фонового процесса.
- 🛠️ **Кастомные цели:** Добавление любых собственных URL и доменов для мониторинга корпоративных серверов или прокси.

---

## 🚀 Исправления и улучшения в v1.1.0

1. **Устранён краш на Android 14+ / 17:** Исправлен сбой `MissingForegroundServiceTypeException` из-за незаданного типа `foregroundServiceType` в `UrlTestMonitorService`.
2. **Оптимизация сети против ложных блокировок:** Таймауты увеличены с агрессивных 1800 мс до 3500 мс с поддержкой параллельных корутин и повторных попыток.
3. **Безопасное чтение пакетов:** Исключены падения `Resources.NotFoundException` при загрузке названий системных и OEM-пакетов.
4. **Удобный поиск:** Добавлен полнотекстовый поиск в диалоге выбора приложений для проверки.

---

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/ANT1VEN0M/Bunker-Url-Test.git
cd Bunker-Url-Test
./gradlew assembleRelease
```

Собранный APK появится в папке `app/build/outputs/apk/release/`.

---

## 🤝 Сообщество и поддержка

- Разработчик: **Alexander Petrashkevich** (`KOT ANTIDOT` / `@k0tlux` / `@Titandarkness`)
- Организация: **[ANT1VEN0M](https://github.com/ANT1VEN0M)**
- Telegram: **[Чат Бункер Кота](https://t.me/+gI1vDkHn1m1lMDYy)**
