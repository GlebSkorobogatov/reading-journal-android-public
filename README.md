# Читательский дневник

Личное офлайн Android-приложение на Kotlin и Jetpack Compose. Это дневник чтения, не электронная читалка: библиотека, сеансы, прогресс, записи и достижения реализуются последовательно по утверждённому плану.

Репозиторий назначения: [reading-journal-android-public](https://github.com/GlebSkorobogatov/reading-journal-android-public). Копия подготовлена отдельно, с новой независимой Git-историей и подтверждённой noreply-идентичностью автора/коммиттера. Перевод видимости выполняет только владелец после проверки Draft PR и отчёта MIGRATION-01.

**Статус:** UI-01 — дизайн-система, каталог компонентов и навигационные заглушки. Все шесть замечаний визуальной полировки повторно проверены владельцем на Galaxy A55. Это не формальная приёмка UI-01 и не разрешение merge. Полный CI новой копии разрешён владельцем; результаты фиксируются только после фактического выполнения. UI-02/UI-03 не начаты; полноценной библиотеки, таймера, БД, статистики и пользовательского резервирования нет.

## Документация и границы

Все семь основных документов сохранены: [AGENTS](AGENTS.md), [PRD](PRD.md), [ARCHITECTURE](ARCHITECTURE.md), [UI_DESIGN_SPEC](UI_DESIGN_SPEC.md), [IMPLEMENTATION_PLAN](IMPLEMENTATION_PLAN.md), [TEST_PLAN](TEST_PLAN.md) и этот README. [Реестр](docs/README.md) перечисляет действующие v2.04, У1–У8, А1–А4, TZ-01, каталог А3, ADR и дизайн-референсы. [SHA-256 публичных документов](docs/SHA256SUMS.txt) относится именно к этим редакциям.

Тёплая светлая и графитовая тёмная темы, Lora/Manrope офлайн, собственные токены поверх Material 3. Линейные иконки, медаль достижений, книги библиотеки, симметричная шестерёнка и адаптивный книжный launcher. Заглушки центрированы между панелями; Settings возвращается полноразмерным горизонтальным переходом, включая predictive Back.

Нет аккаунтов, сервера, синхронизации, телеметрии, сетевого каталога или интернет-функциональности. Публичная доступность исходного кода не добавляет функций публикации пользовательских записей. Не добавлять личные книги, обложки, заметки, базы, ключи или локальные настройки в Git и CI.

Код UI-01 добавляется в отдельной рабочей ветке; первоначальная main содержит только документацию до разрешённого merge.

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

Полная проверка обязательна на итоговом Head перед приёмкой: Build/lint, минимум 3 JVM и 10 Compose-тестов на **каждой** API 26/35/36, без failures/errors/skips. `Quick checks` не заменяет `Full acceptance`. Свежие CI-ссылки и APK добавляются только после фактического запуска; старые результаты не выдаются за новые.

[Отчёт UI-01](docs/acceptance/UI-01.md) · [Galaxy A55](docs/acceptance/UI-01-GALAXY-A55.md) · [Подготовка переноса](docs/acceptance/MIGRATION-01.md) · [Публикация и новая среда Codex](docs/PUBLICATION.md).

Устанавливать APK только после сверки SHA-256 самого файла с `SHA256SUMS.txt` из того же артефакта. Подпись новой debug-сборки может отличаться от установленной; при ошибке совместимости остановиться, не удалять приложение и данные автоматически. См. инструкцию A55.

## Следующий шаг

Завершить MIGRATION-01: владелец сообщил о Public, включении Actions и разрешил Codex полный CI PR №1. Затем выполнить полный CI нового Head и отдельно решить вопрос приёмки UI-01. Порядок DOC → REPO → UI-01 → UI-02 → UI-03 → остальные UI → функциональная фаза → QA → REL сохраняется. Для merge и любого следующего задания требуется отдельное явное разрешение.

Codex самостоятельно управляет веткой и PR в scope задания, запускает проверки, анализирует CI, исправляет согласованные ошибки и предоставляет готовый APK с SHA-256 и отчётом. Ручные GitHub-действия владельца нужны только при подтверждённом ограничении инструментов. Merge и следующее задание требуют отдельных явных разрешений. Подробности — AGENTS.md и docs/CI.md.
