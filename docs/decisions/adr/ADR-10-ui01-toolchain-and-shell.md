# ADR-10 — UI-01: инструменты, шрифты и временная навигация

Дата: 12 ноября 2026 г. Задача: только UI-01. Уточняет ADR-01/02/09, не изменяет продуктовые требования.

## Решение

Один `:app`, `applicationId`/namespace `ru.gleb.readingjournal`, одна Activity,
Compose + Material 3 с собственными семантическими токенами, Navigation Compose.
ViewModel/StateFlow хранит только демонстрационный выбор темы; компоненты stateless.
Нет application/domain/data-реализаций без соответствующих задач.

| Инструмент | Зафиксировано | Причина |
|---|---|---|
| JDK | Temurin 17.0.21+9; JVM bytecode 17 | LTS, javac; поддерживается Gradle 9.9.1 и AGP 9.5 |
| Gradle | 9.9.1 | Стабильный дистрибутив, Wrapper/JAR/launchers и официальная SHA-256 |
| AGP | 9.5.1 | Стабильный patch, поддержка API 37; устранение фактического NewerVersionAvailable lint FAIL |
| Kotlin / Compose compiler plugin | 2.4.21 / 2.4.21 | AGP built-in Kotlin с явно закреплённым KGP в buildscript; одинаковая версия compiler plugin |
| Compose BOM | 2026.09.00 | Стабильный BOM: UI/testing 1.12.1, Material 3 1.4.0 |
| Activity / Lifecycle / Navigation | 1.13.0 / 2.11.0 / 2.10.2 | Тот же минимальный набор, фиксированные стабильные версии |
| compileSdk / targetSdk | 37 / 37 | Стабильный SDK platform 37.0 revision 2, channel 0, PreviewSdkInt=0 |
| Build Tools / Command-line tools | 36.0.0 / 23.0 (16111833) | Официальные стабильные пакеты; native android sdk install без устаревшего tools |
| minSdk | 26 | Manifest merge и runtime-матрица сохраняют API 26; необходимость повышения не выявлена |
| Версия APK | 0.1.1-ui01 (2), debug | Визуальный прототип, не личный выпуск |

## Источники и предел проверки

Проверены непосредственно официальные страницы и SDK/Maven metadata. Это технический
выбор в рамках UI-01, а не изменение утверждённых продуктовых требований.

- [AGP 9.5 release notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes): maximum API 37, minimum/default Gradle 9.6.0, JDK 17, Build Tools 36.0.0. Google Maven metadata подтверждает стабильный patch 9.5.1.
- [Built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) и [AGP 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes): внешний kotlin-android plugin удалён, compilerOptions настроен в kotlin DSL; более новый KGP задаётся top-level buildscript classpath. Так закреплён KGP 2.4.21 вместо встроенного default, Compose compiler plugin имеет ту же версию.
- [Матрица Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html): диапазон полностью протестированных JetBrains сочетаний внешнего KGP 2.4.20–2.4.21 — Gradle 7.6.3–9.7.0 / AGP 8.5.2–9.3.1. Наши Gradle 9.9.1 / AGP 9.5.1 выходят за эту таблицу. Не заявляем полную сертификацию сочетания JetBrains: применён документированный AGP built-in режим, совместимость конкретного набора проверяется сборкой, lint, JVM и всей Android-матрицей. Это остаётся ограничением документальной матрицы.
- [Gradle 9.9.1 compatibility](https://docs.gradle.org/9.9.1/userguide/compatibility.html): JDK 17 поддерживается; дистрибутив проверен по официальной SHA-256. Минимум AGP 9.5 (9.6.0) соблюдён.
- [Android Kotlin/D8/R8](https://developer.android.com/build/kotlin-support): Kotlin 2.4 требует R8 не ниже 9.1.29; AGP 9.5 соответствует этому требованию.
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping): BOM 2026.09.00 соответствует UI/ui-test-junit4 1.12.1 и Material 3 1.4.0; версии дополнительно сверены с официальным BOM POM Google Maven. [BOM guide](https://developer.android.com/develop/ui/compose/bom) и [Compose/Kotlin](https://developer.android.com/jetpack/androidx/releases/compose-kotlin): compiler plugin следует версии Kotlin и не управляется BOM.
- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk): compile/target 37. Стабильность конкретной platform подтверждена официальным repository2-3.xml: channel-0, пакет platforms;android-37.0 revision 2; в установленном source.properties PreviewSdkInt=0, Codename/Beta пусты. Не используется preview SDK.
- JDK/Gradle проверены по официальной SHA-256, SDK/CLI — по checksum Google repository. TLS-проверка сохранена.

Runtime-матрица UI-01: API 26/35/36. Runtime API 37 и release/R8 отдельно не проверены. Владелец подтвердил повторную визуальную проверку шести исправлений на Galaxy A55; формальная приёмка UI-01 и merge остаются отдельными решениями. Новые проверки после переноса фиксируются в отчёте UI-01.

## Шрифты и иконки

Lora variable 400–700 и Manrope variable 200–800 из `google/fonts`, офлайн в `res/font`.
Нужные веса задаются через `FontVariation.weight`; локальный `ExperimentalTextApi`
opt-in ограничен загрузчиком шрифтов. Это экспериментальный API стабильной версии
Compose, а не предварительная версия зависимости. Кириллица, Ё/ё и цифры проверены
по cmap обоих файлов. OFL лежат в `licenses/`; файлы не выдаются через UI.
Числа Manrope используют OpenType `tnum`, если шрифт поддерживает.

Линейные иконки нарисованы в проекте геометрическими контурами, внешней библиотеки
иконок нет. Иконка запуска — тот же простой книжный контур с утверждёнными цветами,
не новый брендовый логотип. Исходный manifest не запрашивает разрешений. AndroidX добавляет собственное signature-разрешение `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` для безопасных внутренних receiver; INTERNET, storage и runtime permissions отсутствуют.

## Навигация и жизненный цикл

NavHost содержит три корневые вкладки и демонстрационные настройки. Debug-каталог
доступен из настроек, четвёртой вкладки нет. Back из другой корневой вкладки ведёт
на Главную; Back с Главной передаётся Android. Настройки возвращают в исходную вкладку.

При повороте Compose/Navigation восстанавливают временный контекст, ViewModel — тему.
Activity передаёт сохранённый Bundle Android только при совпадении идентификатора
текущего процесса. Новый процесс получает чистую навигацию с Главной (NAV-12), без
постоянного хранения настроек. Уведомлений, deep links книг и таймера здесь нет.

Поле неизвестного прогресса — статичная дорожка с «—» и явной семантикой неизвестности,
не нулевой прогресс и не бесконечная загрузка. Все состояния статусов подписаны.

## Визуальная проверка

Точные палитры, heatmap-образцы, 5 фонов заглушек, 11 типографических ролей,
размеры, формы, 48 dp targets и 150/200/250 мс вынесены в дизайн-систему.
Переходы NavHost — 250 мс, Compose использует Android animator duration scale.
Системные панели и горизонтальные cutout-insets учитываются отдельно.

Первые снимки — диагностические результаты Android-эмулятора, не утверждённые
эталоны и не A55. Pixel-diff тест против ещё не утверждённого golden не вводится;
рендеринг Preview, оба режима и состояния проверяются инструментально.
После ручной приёмки можно отдельно зафиксировать эталоны с API/плотностью/масштабом.
Статусы build/lint/JVM/Compose и ограничения находятся в отчёте UI-01.

## Контрольные суммы ресурсов шрифтов

- `lora.ttf`: `822a6621ccbe8d97d20ac88c1c41f5615c9c2c202eaa75f272cd452aac6475a7`.
- `manrope.ttf`: `3ae11c49db0455a3cc33e37d380f20fdb8c7f8b41dc07625c177e3d87a9d6ae6`.

На API 26 Compose `captureToImage` не поддерживает окна диалогов (требуется API 28).
Снимки инструментальных проверок снимаются поддерживаемым Android UiAutomation
`takeScreenshot()` для **всего дисплея** после проверки UI. Диалоги продолжают
проходить те же проверки; API 26 и проверки не исключаются из матрицы.

Для экспорта снимков после UTP используется поддерживаемая AGP опция
`-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`: по умолчанию
runner удаляет APK и приватные файлы после тестов. Опция сохраняет только
тестовую установку на синтетическом эмуляторе, не отключает ни одну проверку.
CI экспортирует снимки после успешного runner, затем сохраняет их артефактом.


## Возврат настроек и полировка UI-01

Для Settings явно заданы обычный и predictive переходы без масштаба: полноразмерная непрозрачная страница смещается по горизонтали за 250 мс; предыдущая вкладка неподвижна. Лёгкий Scaffold каждого destination сохраняет viewport до конца перехода. Predictive Back учитывает край жеста, отмену и завершение. Основание: стандартный predictivePopExitTransition Navigation Compose использует scaleOut(0.7); только popEnter/popExit его не заменяют. Иконки медали/книг/шестерёнки согласованы по сетке и линии; launcher — адаптивный скруглённый книжный знак. Дизайн-токены сохранены.

CI-организация определена в [ADR-11](ADR-11-ci-levels.md). Текущие минимумы — 3 JVM и 10 Android-тестов на каждой API; отсутствие результатов, failures/errors/skips запрещены.
