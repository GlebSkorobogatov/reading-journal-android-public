# Читательский дневник

Личное офлайн Android-приложение Kotlin/Jetpack Compose, дневник, не встроенная читалка. Принцип REV-01: **Читай в своём темпе. Записывай прочитанное, а не измеряй процесс чтения.** Таймер/продолжительность/скорость запрещены, точные календарные даты сохраняются.

Рабочий публичный репозиторий: [reading-journal-android-public](https://github.com/GlebSkorobogatov/reading-journal-android-public).

**Проверенное состояние 23 ноября 2026 г.:** UI-01 принят и объединён, [исторический отчёт](docs/acceptance/UI-01.md)/A55 сохранены. REV-01 принят 14 ноября, PR №3 объединён; его merge-коммит `1073e501cd6b2e835404ce6bd89f5bbd6685d547` сохранён в истории. FIX-01 принят владельцем и объединён через PR №6. Принятый main после FIX-01 — `5ea3f797f42ee1576655871756b7adf33c62c734`.

[PR №2](https://github.com/GlebSkorobogatov/reading-journal-android-public/pull/2), codex/ui-02-home-screen, e7be6c1a6ffeeb777715624a6e758d9f2387fed0, открыт/не принят/не объединён. [PR №4](https://github.com/GlebSkorobogatov/reading-journal-android-public/pull/4), codex/ui-02-home-page-based, 8a5b2e0f36fcaf8f5fddd966fe76afb9834179d4, открыт/Ready/не принят/не объединён, Quick 43672349727 и Full 43672521857 PASS; READY FOR MANUAL ACCEPTANCE, A55 NOT RUN. Это источник компонентов прежней структуры, не окончательный эталон. Оба PR/ветки/история/описания/статусы сохраняются без изменения.

**REV-02 — ACCEPTED / MERGED.** Принятая база UI-02 — `52bf62914dcdddd95cc60aaf3a2369efaff9d3fd`. [Реестр решений](docs/decisions/REV-02-ui-navigation.md) и [история приёмки](docs/acceptance/REV-02.md).

**UI-02 — «Читаю сейчас» и компактный обзор «Статистика».** В демонстрационной сборке доступны обе страницы, три нижних пункта и дочерние заглушки. [Отчёт, доказательства и сценарий ручной приёмки](docs/acceptance/UI-02.md). Полная библиотека (UI-03), аналитика (UI-09) и достижения (UI-12) остаются будущими этапами.

## Документация и границы

[AGENTS](AGENTS.md), [PRD2.1](PRD.md), [ARCHITECTURE2.1](ARCHITECTURE.md), [UI_DESIGN_SPEC2.1](UI_DESIGN_SPEC.md), [IMPLEMENTATION_PLAN2.1](IMPLEMENTATION_PLAN.md), [TEST_PLAN2.1](TEST_PLAN.md), [полный индекс](docs/README.md), [спецификация v3.1](docs/decisions/Читательский_дневник_спецификация_v3.1.md). Принятая v3.0 сохранена как историческая редакция до решения владельца; REV-01/У1–У8/А2/А3/А4/TZ-01 сохраняются вне явно изменённого UI. Внутри книги прежние Чтение / Статистика / Записи. Все функции страниц/XP/истории/аналитики/библиотеки/копии сохраняются.

Нет аккаунтов/сервера/синхронизации/телеметрии/интернет-каталога. Не коммитить личную библиотеку, реальные заметки/обложки/копии/БД/ключи. Тёплая светлая/графитовая тёмная/системная темы, офлайн Lora/Manrope и точные токены UI-01 сохраняются, кроме прямо названных REV-02 элементов представления.

## Сборка

Один модуль `:app`, JDK 17.0.21+9, Gradle Wrapper 9.9.2, AGP 9.5.3, Kotlin/compiler 2.4.21, Compose BOM 2026.11.01, Lifecycle 2.11.1. SDK platform 37.1, Build Tools 36.0.0, platform-tools; minSdk 26, compileSdk 37.1, targetSdk 37. Обновление AGP до 9.5.3 для UI-02 описано в [отчёте UI-02](docs/acceptance/UI-02.md). Четыре версии и необходимое, отдельно разрешённое владельцем повышение compileSdk — технический [FIX-01](docs/acceptance/FIX-01.md), без изменения интерфейсного кода; исходный стек UI-01 и его ограничения — [ADR-10](docs/decisions/adr/ADR-10-ui01-toolchain-and-shell.md). Установить SDK штатными инструментами Android, указать его локально через `ANDROID_HOME` или некоммитимый `local.properties`.

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
python3 .github/scripts/verify_test_results.py app/build/test-results/testDebugUnitTest --minimum 8
python3 .github/scripts/verify_docs.py
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
git diff --check
```

Полный instrumentation suite, на отдельном синтетическом эмуляторе каждой API 26/35/36:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
python3 .github/scripts/verify_test_results.py app/build/outputs/androidTest-results/connected/debug --minimum 17
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, applicationId `ru.gleb.readingjournal`, versionName **0.2.0-rev02**, versionCode **5**. В Windows использовать `gradlew.bat` и Python 3.12+. Debug-сборка не является публичным релизом или готовым рабочим дневником.

## CI и приёмка

[Правила CI](docs/CI.md) описывают Quick, Full Android, API 26/35/36, XML validator и Full acceptance. Сохранены обязательные минимумы 3 JVM и 10 Compose на API. Порог локальной проверки UI-02 — 8 JVM и 17 Compose на API. Полный suite запускается без фильтров. [SHA256SUMS.txt](docs/SHA256SUMS.txt) соответствует этой редакции документов.

Ручная приёмка UI-02 выполняется владельцем на Samsung Galaxy A55. Исторические результаты сохранены в отчётах [UI-01](docs/acceptance/UI-01.md), [A55](docs/acceptance/UI-01-GALAXY-A55.md) и [REV-01](docs/acceptance/REV-01.md).
