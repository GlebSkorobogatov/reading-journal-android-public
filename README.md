# Читательский дневник

Личное офлайн Android-приложение Kotlin/Jetpack Compose, дневник, не встроенная читалка. Принцип REV-01: **Читай в своём темпе. Записывай прочитанное, а не измеряй процесс чтения.** Таймер/продолжительность/скорость запрещены, точные календарные даты сохраняются.

Рабочий публичный репозиторий: [reading-journal-android-public](https://github.com/GlebSkorobogatov/reading-journal-android-public).

**Проверенное состояние 23 ноября 2026 г.:** UI-01 принят и объединён, [исторический отчёт](docs/acceptance/UI-01.md)/A55 сохранены. REV-01 принят 14 ноября, PR №3 объединён; его merge-коммит `1073e501cd6b2e835404ce6bd89f5bbd6685d547` сохранён в истории. FIX-01 принят владельцем и объединён через PR №6. Принятый main после FIX-01 — `5ea3f797f42ee1576655871756b7adf33c62c734`.

[PR №2](https://github.com/GlebSkorobogatov/reading-journal-android-public/pull/2), codex/ui-02-home-screen, e7be6c1a6ffeeb777715624a6e758d9f2387fed0, открыт/не принят/не объединён. [PR №4](https://github.com/GlebSkorobogatov/reading-journal-android-public/pull/4), codex/ui-02-home-page-based, 8a5b2e0f36fcaf8f5fddd966fe76afb9834179d4, открыт/Ready/не принят/не объединён, Quick 43672349727 и Full 43672521857 PASS; READY FOR MANUAL ACCEPTANCE, A55 NOT RUN. Это источник компонентов прежней структуры, не окончательный эталон. Оба PR/ветки/история/описания/статусы сохраняются без изменения.

**Текущий этап REV-02 — только документация.** [Реестр D01–D08/A01–A03](docs/decisions/REV-02-ui-navigation.md), [отчёт](docs/acceptance/REV-02.md). REV-02 принят владельцем без замечаний 23 ноября 2026 г. (**ACCEPTED**). PR №5 открыт (**NOT MERGED**). Будущая структура: Читаю сейчас (только активные книги, «Не количество страниц, а глубина понимания.») / Статистика (компактный XP, одна карточка Страницы/Прочтения/Серия, стрелки периода, три перехода) / Библиотека. XP открывает дочерние Достижения; Динамика включает сравнение, Календарь/Рекорды отдельные. Плоские контурные значки, закрытая книга первого пункта, первая буква заглушки; нетронутые цвета/шрифты/числовые токены прежние.

**Новая структура не реализована.** Код этой ветки по-прежнему принятый каркас UI-01 с его прежней навигацией/ресурсами, не готовый дневник. Новая UI-02 не выполнена, UI-03 не начат. Full CI собирает debug APK каркаса UI-01 для технических проверок.

## Документация и границы

[AGENTS](AGENTS.md), [PRD2.1](PRD.md), [ARCHITECTURE2.1](ARCHITECTURE.md), [UI_DESIGN_SPEC2.1](UI_DESIGN_SPEC.md), [IMPLEMENTATION_PLAN2.1](IMPLEMENTATION_PLAN.md), [TEST_PLAN2.1](TEST_PLAN.md), [полный индекс](docs/README.md), [спецификация v3.1](docs/decisions/Читательский_дневник_спецификация_v3.1.md). Принятая v3.0 сохранена как историческая редакция до решения владельца; REV-01/У1–У8/А2/А3/А4/TZ-01 сохраняются вне явно изменённого UI. Внутри книги прежние Чтение / Статистика / Записи. Все функции страниц/XP/истории/аналитики/библиотеки/копии сохраняются.

Нет аккаунтов/сервера/синхронизации/телеметрии/интернет-каталога. Не коммитить личную библиотеку, реальные заметки/обложки/копии/БД/ключи. Тёплая светлая/графитовая тёмная/системная темы, офлайн Lora/Manrope и точные токены UI-01 сохраняются, кроме прямо названных REV-02 элементов представления.

## Сборка

Один модуль `:app`, JDK 17.0.21+9, Gradle Wrapper 9.9.2, AGP 9.5.2, Kotlin/compiler 2.4.21, Compose BOM 2026.11.01, Lifecycle 2.11.1. SDK platform 37.1, Build Tools 36.0.0, platform-tools; minSdk 26, compileSdk 37.1, targetSdk 37. Четыре версии и необходимое, отдельно разрешённое владельцем повышение compileSdk — технический [FIX-01](docs/acceptance/FIX-01.md), без изменения интерфейсного кода; исходный стек UI-01 и его ограничения — [ADR-10](docs/decisions/adr/ADR-10-ui01-toolchain-and-shell.md). Установить SDK штатными инструментами Android, указать его локально через `ANDROID_HOME` или некоммитимый `local.properties`.

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

[Правила CI](docs/CI.md) и .github/ci-policy.json/workflows неизменны: Quick для PR/main, Full Android по отдельному разрешению владельца; API26/35/36, полный suite/XML validator/Full acceptance/owner-public guard/retention. Принятая база имеет минимумы 3 JVM/10 Android на API; результаты PR №4 (22/34) не являются выполнением Android в REV-02.

После синхронизации REV-02 с main выполняются verify_docs, проверка инвентаря, ссылок и SHA-256, CI-script unit tests и diff --check. Quick подтверждает документационный scope. Пропуск Android build в docs-only Quick считается NOT RUN. Полный Android CI запускается через Draft → Ready и проверяет совместное состояние FIX-01 и REV-02 на одном Head. Суммы [SHA256SUMS.txt](docs/SHA256SUMS.txt) относятся к этой редакции документов.

## Следующий шаг

**REV-02 — ACCEPTED / NOT MERGED.** Повторные Quick и Full на PR №5 завершают текущий этап. Для merge PR №5 требуется отдельное задание владельца. UI-02 и UI-03 имеют статус NOT STARTED. Следующий этап UI-02 включает два первых demo-экрана, навигацию и минимальные дочерние заглушки. Полная библиотека UI-03, аналитика UI-09 и достижения UI-12 остаются отдельными этапами. PR №2 и №4 сохраняются. Исторические отчёты [UI-01](docs/acceptance/UI-01.md), [A55](docs/acceptance/UI-01-GALAXY-A55.md) и [REV-01](docs/acceptance/REV-01.md) сохранены в своих редакциях.
