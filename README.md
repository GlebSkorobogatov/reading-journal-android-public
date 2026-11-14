# Читательский дневник

Личное офлайн Android-приложение на Kotlin и Jetpack Compose. Это дневник чтения, не электронная читалка: библиотека, подтверждённые страницы, циклы, записи и достижения реализуются последовательно по утверждённому плану.

Принцип: **Читай в своём темпе. Записывай прочитанное, а не измеряй процесс чтения.** Таймер, продолжительность и скорость чтения исключены из будущих требований; календарные даты сохраняются.

Рабочий публичный репозиторий: [reading-journal-android-public](https://github.com/GlebSkorobogatov/reading-journal-android-public).

**Проверенный статус 14 ноября 2026 г.:** UI-01 принят владельцем и [PR №1](https://github.com/GlebSkorobogatov/reading-journal-android-public/pull/1) объединён; принятая база main `4c14b5dce1b9dc819d76c5c6368236320c442c3c`. Его реальные CI/A55 результаты сохранены в [отчёте](docs/acceptance/UI-01.md).

Старый UI-02 реализован в [PR №2](https://github.com/GlebSkorobogatov/reading-journal-android-public/pull/2), ветка `codex/ui-02-home-screen`, head `e7be6c1a6ffeeb777715624a6e758d9f2387fed0`; автоматические проверки были пройдены, ручная приёмка не проведена, не принят и не объединён. PR/ветку/коммиты сохранять. Переработанный UI-02 новой концепции **не выполнен**. UI-03 не начат.

**Текущая задача REV-01 — только документация:** [реестр решений](docs/decisions/REV-01-reading-without-timer.md), [отчёт](docs/acceptance/REV-01.md). Пакет подготовлен к приёмке, не принят. Реализация/исполняемые тесты/Gradle/resources/manifest/CI не меняются; новая модель пока не реализована. Основной код этой ветки — принятый каркас UI-01, а не работающий дневник.

## Документация и границы

Все семь основных документов сохранены: [AGENTS](AGENTS.md), [PRD](PRD.md), [ARCHITECTURE](ARCHITECTURE.md), [UI_DESIGN_SPEC](UI_DESIGN_SPEC.md), [IMPLEMENTATION_PLAN](IMPLEMENTATION_PLAN.md), [TEST_PLAN](TEST_PLAN.md) и этот README. [Реестр](docs/README.md) перечисляет действующие PRD2.0/спецификацию v3.0/REV-01, сохраняемые У1–У8/А2/А4/TZ-01, пересмотренный каталог А3, ADR и дизайн-референсы. [SHA-256 публичных документов](docs/SHA256SUMS.txt) относится именно к этим редакциям.

Тёплая светлая и графитовая тёмная темы, Lora/Manrope офлайн, собственные токены поверх Material 3. Линейные иконки, медаль достижений, книги библиотеки, симметричная шестерёнка и адаптивный книжный launcher. Заглушки центрированы между панелями; Settings возвращается полноразмерным горизонтальным переходом, включая predictive Back.

Нет аккаунтов, сервера, синхронизации, телеметрии, сетевого каталога или интернет-функциональности. Публичная доступность исходного кода не добавляет функций публикации пользовательских записей. Не добавлять личные книги, обложки, заметки, базы, ключи или локальные настройки в Git и CI.

## Сборка

Один модуль `:app`, JDK 17.0.21+9, Gradle Wrapper 9.9.1, AGP 9.5.1, Kotlin/compiler 2.4.21, Compose BOM 2026.09.00. SDK platform 37.0, Build Tools 36.0.0, platform-tools; minSdk 26, compile/target 37. Фиксированные версии и ограничения — [ADR-10](docs/decisions/adr/ADR-10-ui01-toolchain-and-shell.md). Установить SDK штатными инструментами Android, указать его локально через `ANDROID_HOME` или некоммитимый `local.properties`.

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
python3 .github/scripts/verify_test_results.py app/build/test-results/testDebugUnitTest --minimum 3
python3 .github/scripts/verify_docs.py
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
git diff --check
```

Полный instrumentation suite, на отдельном синтетическом эмуляторе каждой API 26/35/36:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
python3 .github/scripts/verify_test_results.py app/build/outputs/androidTest-results/connected/debug --minimum 10
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, applicationId `ru.gleb.readingjournal`, versionName **0.1.1-ui01**, versionCode **2**. В Windows использовать `gradlew.bat` и Python 3.12+. Debug-сборка не является публичным релизом или готовым рабочим дневником.

## CI и приёмка

[Схема CI](docs/CI.md): быстрый workflow для PR и main, полный — по явному разрешению владельца, включая автоматический перевод PR в Ready через Codex. Повторные полные push/PR-запуски рабочей ветки устранены. Только стандартные Ubuntu runners, `contents: read`, без секретов; в приватном репозитории исполнение jobs заблокировано. До разрешения владельца CI не запускать. Артефакты быстрых запусков хранятся 7 дней, полных — 30.

Для реализации Android полная проверка обязательна на итоговом Head перед приёмкой: Build/lint, минимум 3 JVM и 10 Compose-тестов на **каждой** API 26/35/36, без failures/errors/skips. `Quick checks` не заменяет `Full acceptance`. REV-01 — docs-only: Draft остаётся Draft, Full повторно не запускается, APK/A55 не требуются. Существующий Quick может автоматически проверить документы без изменения политики. Свежие CI-ссылки и APK добавляются только после фактического запуска; старые результаты не выдаются за новые.

[Отчёт UI-01](docs/acceptance/UI-01.md) · [Galaxy A55](docs/acceptance/UI-01-GALAXY-A55.md) · [Подготовка переноса](docs/acceptance/MIGRATION-01.md) · [Публикация и новая среда Codex](docs/PUBLICATION.md).

Устанавливать APK только после сверки SHA-256 самого файла с `SHA256SUMS.txt` из того же артефакта. Подпись новой debug-сборки может отличаться от установленной; при ошибке совместимости остановиться, не удалять приложение и данные автоматически. См. инструкцию A55.

## Следующий шаг

Review и явная приёмка документации REV-01 → отдельное разрешение merge → отдельно поставленное задание на повторную реализацию UI-02 → её CI/визуальные доказательства/A55 → отдельный merge и разрешение UI-03. F-06/F-07 отменены, остальные этапы и контрольные точки — IMPLEMENTATION_PLAN.

В REV-01 разрешены отдельная документационная ветка `codex/rev-01-reading-without-timer` и Draft PR `docs: REV-01 — reading journal without timer` в main. После подготовки остановиться. PR №2 не изменять, merge/UI-02/UI-03/F не начинать. Таймер не возвращать даже дополнительной функцией. Все утверждённые дизайн-токены и общий стиль сохранять точно.
