# Z80 Pocket IDE

**Android IDE и ассемблер Z80 для ZX Spectrum / Android IDE and Z80 assembler for ZX Spectrum**

[Русский](#русский) · [English](#english) · [CHANGELOG](CHANGELOG.md)

---

## Русский

**Z80 Pocket IDE** — лёгкая среда разработки на Android для написания, проверки, сборки и запуска программ на ассемблере Z80, прежде всего для **ZX Spectrum 48K**. Приложение написано на Java без AndroidX.

### Скачать APK

**[⬇️ Скачать последнюю сборку Z80 Pocket IDE](https://github.com/knowsbetteralex/Z80-Pocket-IDE/releases/latest/download/Z80-Pocket-IDE.apk)**

[Последний релиз и его описание](https://github.com/knowsbetteralex/Z80-Pocket-IDE/releases/latest) · [Все релизы](https://github.com/knowsbetteralex/Z80-Pocket-IDE/releases)

APK собирается автоматически после успешных тестов в ветке `main`; рядом с ним публикуется контрольная сумма SHA-256. Для установки потребуется разрешить Android установку приложений из выбранного источника.

> **Важно:** опубликованные APK подписаны **отладочным ключом** Android, а не производственным ключом Google Play. Подпись разных CI-сборок может отличаться; перед переустановкой сохраняйте исходники и нужные данные проекта.

### Возможности

- **Редактор ASM:** подсветка синтаксиса, поиск/замена, форматирование, автодополнение меток, отмена/повтор изменений, вкладки и восстановление черновиков. Контроль внешних изменений файлов: автоматическое обновление чистых вкладок и защита несохранённых правок.
- **Проекты:** несколько ASM-файлов, `INCLUDE`, выбор главного ASM, ресурсы `INCBIN` и настройки проекта.
- **Ассемблер Z80:** инструкции Z80, метки, выражения, `ORG`, `EQU`, `DB`, `DW`, `DS`, `MACRO`, условная сборка `IF`/`IFN` и `ASSERT`. Анализ размера кода и T-состояний.
- **ZX Spectrum:** сборка и экспорт `.tap`, экспорт снимка `.sna` (48K), запуск TAP через установленный эмулятор, загрузочная заставка SCREEN$. Кнопка запуска показывает индикатор сборки.
- **Редактор экранов:** 256×192, цвета INK/PAPER, BRIGHT/FLASH, рисование и выделение, импорт/экспорт SCR и ASM.
- **Инструменты:** системы счисления, адреса экрана, спрайты, клавиатура Spectrum, библиотека примеров и справочник Z80/ZX.
- **Обновления:** колокольчик слева от меню сообщает о новых релизах GitHub; в окне уведомления — краткие изменения и ссылка на APK.

**Язык интерфейса:** `Настройки → Язык интерфейса: RU/EN` — переключение без перезапуска вручную.

### Сборка из исходников

Нужны **JDK 17**, **Android SDK 35** и **Gradle 8.9**. В корне репозитория:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

**Руководство пользователя:** [Русский](app/src/main/assets/help/guide_ru.md) · [English](app/src/main/assets/help/guide_en.md)

**Документация:** [История версий](CHANGELOG.md) · [Материалы по редактору экранов](docs/SCREEN_EDITOR_V0.38.0.md) · [Исходный код](app/src/main/java/com/alexzab/z80pocketide)

---

## English

**Z80 Pocket IDE** is a lightweight Android IDE for writing, checking, assembling, and running Z80 assembly programs, primarily for the **ZX Spectrum 48K**. It is implemented in plain Java without AndroidX.

### Download APK

**[⬇️ Download the latest Z80 Pocket IDE APK](https://github.com/knowsbetteralex/Z80-Pocket-IDE/releases/latest/download/Z80-Pocket-IDE.apk)**

[Latest release and notes](https://github.com/knowsbetteralex/Z80-Pocket-IDE/releases/latest) · [All releases](https://github.com/knowsbetteralex/Z80-Pocket-IDE/releases)

An APK is automatically published after a successful `main` build and test run, alongside a SHA-256 checksum. Android may require permission to install apps from your chosen download source.

> **Important:** downloadable APKs use an **Android debug signing key**, not a Google Play production key. The signing key may differ between CI builds; back up your source files and project data before reinstalling.

### Features

- **ASM editor:** syntax highlighting, find/replace, formatting, label completion, undo/redo, source tabs, and draft recovery. External file checks automatically refresh clean tabs while protecting unsaved edits.
- **Projects:** multiple ASM files, `INCLUDE`, selectable entry-point ASM, `INCBIN` assets, and project settings.
- **Z80 assembler:** Z80 instructions, labels, expressions, `ORG`, `EQU`, `DB`, `DW`, `DS`, `MACRO`, `IF`/`IFN` conditional assembly, and `ASSERT`. Byte-size and T-state analysis.
- **ZX Spectrum:** `.tap` build/export, 48K `.sna` snapshot export, emulator launch for TAP files, and optional SCREEN$ loading screen. The Run button shows compilation progress.
- **Screen editor:** 256×192 pixels, INK/PAPER colors, BRIGHT/FLASH, drawing and selection, SCR and ASM import/export.
- **Tools:** number-base converter, screen addresses, sprite tools, Spectrum keyboard, examples, and Z80/ZX reference.
- **Updates:** the bell beside the menu indicates new GitHub releases, with concise release notes and an APK link.

**Interface language:** use `Settings → Interface language: RU/EN` to switch immediately.

### Build from source

Requires **JDK 17**, **Android SDK 35**, and **Gradle 8.9**. Run from the repository root:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

**User guide:** [English](app/src/main/assets/help/guide_en.md) · [Русский](app/src/main/assets/help/guide_ru.md)

**Documentation:** [Version history](CHANGELOG.md) · [Screen editor notes](docs/SCREEN_EDITOR_V0.38.0.md) · [Source code](app/src/main/java/com/alexzab/z80pocketide)
