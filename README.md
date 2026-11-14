# Читательский дневник

Личное офлайн Android-приложение на Kotlin и Jetpack Compose. Это дневник чтения, не электронная читалка: библиотека, сеансы, прогресс, записи и достижения реализуются последовательно по утверждённому плану.

Репозиторий назначения: [reading-journal-android-public](https://github.com/GlebSkorobogatov/reading-journal-android-public). Копия подготовлена отдельно, с новой независимой Git-историей и подтверждённой noreply-идентичностью автора/коммиттера. Перевод видимости выполняет только владелец после проверки Draft PR и отчёта MIGRATION-01.

**Статус:** UI-01 — COMPLETE, принят владельцем и объединён в main (PR №1). UI-02 — Главная на детерминированных демонстрационных данных, код прошёл Full acceptance; ожидает ручной приёмки. Итоговый отчётный Head и его обязательный CI/APK указаны в [PR №2](https://github.com/GlebSkorobogatov/reading-journal-android-public/pull/2). Реализованы читаемые книги/pin, mock-таймер/мини-панель, иллюстрированная XP-карточка, фиксированные метрики/периоды и пять секций аналитики. [Отчёт UI-02](docs/acceptance/UI-02.md) · [ручной checklist A55](docs/acceptance/UI-02-GALAXY-A55.md). Реальные таймер, аналитика, XP и хранилище не реализованы. Merge UI-02 и начало UI-03 не разрешены.

## Документация и границы

Все семь основных документов сохранены: [AGENTS](AGENTS.md), [PRD](PRD.md), [ARCHITECTURE](ARCHITECTURE.md), [UI_DESIGN_SPEC](UI_DESIGN_SPEC.md), [IMPLEMENTATION_PLAN](IMPLEMENTATION_PLAN.md), [TEST_PLAN](TEST_PLAN.md) и этот README. [Реестр](docs/README.md) перечисляет действующие v2.04, У1–У8, А1–А4, TZ-01, каталог А3, ADR и дизайн-референсы. [SHA-256 публичных документов](docs/SHA256SUMS.txt) относится именно к этим редакциям.

Тёплая светлая и графитовая тёмная темы, Lora/Manrope офлайн, собственные токены поверх Material 3. Линейные иконки, медаль достижений, книги библиотеки, симметричная шестерёнка и адаптивный книжный launcher. Заглушки центрированы между панелями; Settings возвращается полноразмерным горизонтальным переходом, включая predictive Back.

Нет аккаунтов, сервера, синхронизации, телеметрии, сетевого каталога или интернет-функциональности. Публичная доступность исходного кода не добавляет функций публикации пользовательских записей. Не добавлять личные книги, обложки, заметки, базы, ключи или локальные настройки в Git и CI.

Принятая база UI-02: `4c14b5dce1b9dc819d76c5c6368236320c442c3c`, main после [PR №1](https://github.com/GlebSkorobogatov/reading-journal-android-public/pull/1). Рабочая ветка: `codex/ui-02-home-screen`; старую ветку UI-01 и исходный проект не изменять.

## Сборка

Один модуль `:app`, JDK 17.0.21+9, Gradle Wrapper 9.9.1, AGP 9.5.1, Kotlin/compiler 2.4.21, Compose BOM 2026.09.00. SDK platform 37.0, Build Tools 36.0.0, platform-tools; minSdk 26, compile/target 37. Фиксированные версии и ограничения — [ADR-10](docs/decisions/adr/ADR-10-ui01-toolchain-and-shell.md). Установить SDK штатными инструментами Android, указать его локально через `ANDROID_HOME` или некоммитимый `local.properties`.

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
python3 .github/scripts/verify_test_results.py app/build/test-results/testDebugUnitTest --minimum 19
python3 .github/scripts/verify_docs.py
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
git diff --check
```

Полный instrumentation suite, на отдельном синтетическом эмуляторе каждой API 26/35/36:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
python3 .github/scripts/verify_test_results.py app/build/outputs/androidTest-results/connected/debug --minimum 32
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, applicationId `ru.gleb.readingjournal`, versionName **0.2.0-ui02**, versionCode **3**. В Windows использовать `gradlew.bat` и Python 3.12+. Debug-сборка не является публичным релизом или готовым рабочим дневником.

## CI и приёмка

[Схема CI](docs/CI.md): быстрый workflow для PR и main, полный — по явному разрешению владельца, включая автоматический перевод PR в Ready через Codex. Повторные полные push/PR-запуски рабочей ветки устранены. Только стандартные Ubuntu runners, `contents: read`, без секретов; в приватном репозитории исполнение jobs заблокировано. До разрешения владельца CI не запускать. Артефакты быстрых запусков хранятся 7 дней, полных — 30.

Полная проверка обязательна на итоговом Head перед приёмкой: Build/lint, минимум 19 JVM и 32 Compose-теста на **каждой** API 26/35/36, без failures/errors/skips. `Quick checks` не заменяет `Full acceptance`. Свежие CI-ссылки и APK добавляются только после фактического запуска; старые результаты не выдаются за новые.

[Отчёт UI-01](docs/acceptance/UI-01.md) · [Galaxy A55](docs/acceptance/UI-01-GALAXY-A55.md) · [Подготовка переноса](docs/acceptance/MIGRATION-01.md) · [Публикация и новая среда Codex](docs/PUBLICATION.md).

Устанавливать APK только после сверки SHA-256 самого файла с `SHA256SUMS.txt` из того же артефакта. Подпись новой debug-сборки может отличаться от установленной; при ошибке совместимости остановиться, не удалять приложение и данные автоматически. См. инструкцию A55.

## Следующий шаг

Завершить проверки итогового Head UI-02 и предоставить проверенную APK/кадры для отдельной ручной приёмки владельцем. После успешного полного CI и доказательств остановиться; merge и UI-03 не выполнять. Сценарии: Настройки → Каталог компонентов · debug → Сценарии главной · debug. Выбор сценария сбрасывает только mock-state. Длительность таймера фиксирована, реальные записи и начисления не создаются.

Codex самостоятельно управляет веткой и PR в scope задания, запускает проверки, анализирует CI, исправляет согласованные ошибки и предоставляет готовый APK с SHA-256 и отчётом. Ручные GitHub-действия владельца нужны только при подтверждённом ограничении инструментов. Merge и следующее задание требуют отдельных явных разрешений. Подробности — AGENTS.md и docs/CI.md.
