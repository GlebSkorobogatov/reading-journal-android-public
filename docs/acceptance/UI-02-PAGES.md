# UI-02 — Главная по REV-01: отчёт реализации

**Режим:** демонстрационный UI-first. **Приёмка владельцем: NOT RUN.** Merge и UI-03 не разрешены. Нормативные документы REV-01 и дизайн не изменяются.

Этот отчёт коммитится вместе с проверяемым кодом. Точный итоговый Head, фактические Quick/Full/API-результаты, APK/checksum/сертификат и итог проверки изображений фиксируются в описании нового PR после запуска CI, чтобы отчётный коммит не подменял проверенный sourceSha. READY FOR MANUAL ACCEPTANCE допустим только после успешного Full точного итогового Head и просмотра его реальных кадров. Старые результаты не являются приёмкой нового Head.

## 1. Исходное состояние и база

Проверен публичный GlebSkorobogatov/reading-journal-android-public. Main `1073e501cd6b2e835404ce6bd89f5bbd6685d547`; UI-01 принят/объединён; REV-01 принят 14 ноября 2026 г., PR №3 объединён (его Head f84b429296b4026d854b2c25a042f12723e00727). Quick main 43595471492 и Full REV 43595447446 — completed/success. PR №2 открыт, не принят/не объединён, Head `e7be6c1a6ffeeb777715624a6e758d9f2387fed0`; существенных расхождений нет.

## 2. Новая ветка и PR

Ветка `codex/ui-02-home-page-based` создана **от принятого main**, а не старого UI-02. Один новый Draft PR: `feat: UI-02 — page-based home screen`; его итоговые SHA и CI — в метаданных PR. [Сравнение новой ветки](https://github.com/GlebSkorobogatov/reading-journal-android-public/compare/main...codex:ui-02-home-page-based). PR №2 не переиспользуется и не редактируется.

## 3. Переиспользование старого UI-02

| Компонент | Результат |
|---|---|
| HomeArt.kt | Сохранён точно: художественная иллюстрация, XP-карточка/геометрия/оформление |
| HomeScreen.kt | Переиспользованы лента, карточки, pin/порядок, метрики, адаптация fontScale, аккордеон и sheets; управление заменено typed-входом записи |
| HomeAnalytics.kt | Прежние геометрия столбцов, heatmap/горизонтальная прокрутка/палитра, сравнение и карточки рекордов; смысл только страницы |
| HomeUiState.kt | Удалены таймер, PageState/pending, ChartMeasure, скорость и отдельные виды чтения; добавлены четыре секции и typed HomeDestination |
| HomeFixtures.kt | Переработаны статические согласованные снимки страниц/циклов/дней/рекордов; восемь содержательных сценариев |
| HomeDemoViewModel.kt | UDF/StateFlow/SavedStateHandle, pin/period/section/grouping/year; без истории/книг в SavedState и без отменённых состояний |
| HomeRoute.kt, JournalApp.kt | Переиспользованы маршрутизация/Preview/debug-вход и интеграция Главной; мини-панель исключена полностью |
| ComponentCatalog.kt | Сохранён каталог UI-01, добавлен только debug-вход сценариев Главной |
| ShellRegressionActivity и два Shell-теста | Debug-only исходный каркас для неизменённых assertions UI-01; проверки не отключаются |
| Home-тесты/Evidence/CI helper | Адаптированы с картой ниже; sourceSha/стабильный foreground capture и сохранение exit-кода переиспользованы |

Старые продуктовые документы, версии Gradle/SDK и вся старая ветка не переносились. Слепой cherry-pick не выполнялся.

## 4. Отличия от прежней концепции

Исключены mock-таймер/мини-панель/Start-Pause-Resume-Finish, глобальный timer-state, pending/finishing/отложенные страницы, раздельные типы чтения, временные метрики/скорость/прогноз/переключатель времени/шесть рекордов. В UI/state/events/fixtures/Preview/debug-каталоге активной модели измерения нет. Числовой неиспользуемый токен Metric.timer из принятой UI-01 оставлен неизменным для сохранения дизайн-системы; он нигде не создаёт таймер или скрытый сценарий. java.time и animator_duration_scale относятся к календарю/Android-анимациям, не длительности чтения.

## 5. Реализованная Главная

Строго: все активные книги вертикально → прежняя крупная XP-карточка → страницы/завершённые прочтения/глобальная текущая серия → шесть периодов → четыре секции (Динамика/Календарь/Сравнение/Итоги и рекорды). Открыта максимум одна секция. Pin и порядок независимы от библиотеки; нет ограничения числа книг. Закладка, достигнутый рубеж и подтверждённые страницы показаны раздельно.

«Записать чтение» отправляет HomeEvent.Open(HomeDestination.RecordReading(bookId)); явная демонстрационная панель объясняет отсутствие формы UI-08 и сохранения. UI-09 и статистика книги представлены только typed-входами. Нет имитации записи/начисления/изменения прогресса.

Фикстуры: EMPTY, SINGLE, MULTIPLE, PINNED, NO_PAGES, BOOKMARK, HISTORICAL, LONG_TEXT. XP 2520 = 1795 новых страниц + 5 действительных завершений×100 + 225 встроенных ступеней; уровень 8, «Ученик», 70/550. Два исторических завершения добавляют только число прочтений доступной точности, не страницы/XP. Текущая серия 3; лучшая 8. Все значения подготовлены, настоящего calculator нет.

## 6. Трассировка REV-01

| Решение | UI-02 |
|---|---|
| D01/C01/C04/C06 | Раздельные поля позиции/рубежа/подтверждений; typed-кнопка записи. Реальное определение диапазона NOT RUN, F-задачи |
| D02/C05 | Точные календарные дни в фикстурах; нет датированных страниц неизвестной точности, исторические завершения отдельно. Реальная валидация формы NOT RUN |
| D03 | Демонстрационные показатели уже отражают только новые страницы; алгоритм dedup не реализован |
| D04 | Реальное edit/delete/пересчёт NOT RUN; новых редакторов истории нет |
| D05 | Согласованные статические XP, без начисления |
| D06 | Прежняя карточка/иллюстрация, формулы C/T проверяют только демонстрационные значения |
| D07 | Подписи/ячейки от 1 новой страницы, глобальная серия не меняется с периодом; реальный streak-engine NOT RUN |
| D08 | Каталог/награды не реализуются; только согласованность XP-примера, достижения остаются заглушкой UI-01 |
| D09 | Ровно 3 метрики в порядке, 6 периодов, глобальные XP/серия |
| D10 | Только страницы, 3 группировки, подготовленные интервалы; полный год/5 цветов/границы 0, 1, 9, 10, 24, 25, 49, 50; day-вход по книгам |
| D11 | Все активные включая 0, убывание страниц; 4 рекорда и охват/историческая неизвестность |
| D12/C03 | Один typed-контракт RecordReading совместим с UI-08 (страница/точная дата/toggle/save), сама форма не реализована |

Применимые визуальные части REV-T17–23 покрываются Home-тестами и будущей A55-проверкой. Функциональные READ/F-алгоритмы остаются NOT RUN.

## 7. Сохранение дизайна

Theme.kt, Components.kt, LineIcon.kt и ресурсы приложения/шрифтов не изменены относительно main. Все нормативные документы PRD/ARCHITECTURE/UI_DESIGN_SPEC/IMPLEMENTATION_PLAN/TEST_PLAN/спецификация v3.0/REV-01 сохраняются. HomeArt.kt совпадает с точным старым Head. Используются прежние размеры/скругления/поля/палитры/типографика/переходы. Состав меняется только согласно REV-01. Старый UI-02 не объявляется принятым golden; фактические снимки предназначены владельцу для review.

## 8. Файлы и область изменений

Home-компоненты/fixtures/state/route/ViewModel; минимальная интеграция JournalApp/ComponentCatalog; Home-тесты и debug-host; два существующих Shell-теста меняют только host, все методы/assertions сохраняются; app/build.gradle.kts меняет только versionName/versionCode. CI: task/scope/повышенные минимумы и ранее проверенный helper получения кадров с точным sourceSha/exit-кодом. Документы: README, реестр/суммы/инвентарь, этот отчёт, A55 checklist и техническая запись CI. Полный перечень — git diff main...Head и docs/FILES.txt.

## 9. Карта прежних тестов UI-02

Каждый прежний метод сопоставлен ниже. «Не перенесён» означает отмену требования REV-01, а не отключённый обязательный тест. UI-01: прежние 3 JVM / 10 Compose сохраняются, в ShellTest/ShellPolishTest только MainActivity заменена debug-only ShellRegressionActivity без изменения assertions.

### HomeDemoTest.kt

| Прежний метод | Статус | Преемник/основание |
|---|---|---|
| xpFixturesUseApprovedLevelCostAndTitle | сохранён | xpFixturesUseApprovedLevelCostAndTitle |
| pinnedOrderIsIndependentOfLastReadingOrder | адаптирован | pinnedOrderIsIndependentOfLastReadingOrder |
| activeBookListIsNotLimitedToAVisualPageSize | адаптирован | activeBookListHasNoArtificialLimit |
| runningAndPausedTimerBothExcludeASecondTimer | не перенесён | Таймер отменён D01–D12 |
| pendingBlocksOnlyItsBookWhileUnknownAllowsReading | не перенесён | pending/таймерные блокировки отменены |
| finishingDoesNotWriteStatisticsOrXp | заменён | recordEntryUsesTheSelectedBookAndDoesNotChangeData |
| dismissedFinishingReturnsToPauseWithoutInventingTime | не перенесён | finishing отменён |
| allPeriodsKeepGlobalXpAndCurrentStreak | адаптирован | allPeriodsUsePreparedPagesAndCompletedCyclesButKeepGlobals |
| accordionHasOneRememberedSectionAndCanCollapse | адаптирован | accordionHasExactlyFourOrderedSectionsAndCanCollapse |
| unknownZeroAndMinimumHaveDistinctDisplayContracts | заменён | historicalUnknownIsNotZeroAndNeverCreatesPages |
| monthlyQuantityAndDateMarkersAreIndependent | не перенесён | неполные/неточнодатированные страницы отменены C05 |
| impreciseDatesStayOutsideFinerBuckets | не перенесён | C05; исторические завершения проверяются отдельно |
| pageOnlyHeatmapDoesNotCreateTimeOrAStreakDay | заменён | everyHeatmapBoundaryUsesTheApprovedFiveColors |
| savedVisualSelectionsSurviveNewStateHolder | адаптирован | savedVisualSelectionsAndPinOrderSurviveNewStateHolder |
| resolvedTimerDoesNotReappearWhenScenarioInitiallyHadOne | не перенесён | таймерные состояния отсутствуют |
| preparedCalendarTotalsAndRecordsMatchThePeriodFixtures | адаптирован | preparedCalendarTotalsAgreeWithAllPeriodMetrics + preparedBookContributionsAgreeWithCalendarAndComparison |
### HomeInteractionTest.kt

| Прежний метод | Статус | Преемник/основание |
|---|---|---|
| mainBlocksAndEveryActiveBookFollowTheApprovedOrder | адаптирован | одноимённый метод; 4 секции |
| pinAndManualOrderAreInteractive | сохранён | одноимённый метод |
| emptyDiaryShowsRealZeroAndNoBookOrTimer | адаптирован | emptyDiaryShowsRealZeroLevelOneAndNoBookOrTimer |
| sixPeriodsSwitchWithoutChangingGlobalLevelOrStreak | адаптирован | sixPeriodsSwitchMetricsButKeepGlobalXpAndStreak |
| onlyOneAnalyticsSectionStaysExpanded | адаптирован | onlyOneOfFourSectionsCanRemainExpandedAndItCanCollapse |
| recreationKeepsSectionPeriodAndSpeedSelection | заменён | recreationKeepsPeriodGroupingYearAndPinOrder |
| startPauseResumeAndFinishAreVisualAndDeterministic | заменён | everyRecordButtonOpensItsBookWithoutSavingData |
| pausedTimerPreventsStartingAnotherBook | не перенесён | таймер/блокировка отменены |
| miniTimerOnAnotherTabReturnsToItsBook | не перенесён | мини-панель отменена; навигация проверяется otherTabsRemainUi01PlaceholdersWithoutAnyMiniPanel |
| pendingAndUnknownPagesHaveDifferentActions | не перенесён | pending отменён |
| delayingPagesFinishesTheDemoButBlocksOnlyItsBook | не перенесён | отложенное заполнение отменено |
| unknownZeroAndMinimumAreVisibleAsDifferentValues | заменён | historicalUnknownsDoNotBecomePagesOrRecords + empty/no-pages |
| monthQuantityAndDateMarkersRemainIndependent | не перенесён | неточнодатированные страницы/ожидание отменены |
| undatedGroupAndIntervalNavigationDoNotInventDates | адаптирован | intervalNavigationUsesPreparedWindowsAndResetsWithGrouping; нет группы неточных страниц |
| heatmapSeparatesTimePagesOnlyAndTheStreakThreshold | заменён | heatmapUsesEveryApprovedBoundaryAndOnePageQualifies + dayEntryShowsPageRecordsGroupedByBook |
| comparisonIncludesTheBookWithZeroTimeAndHonestHistoricalUnknowns | адаптирован | comparisonIncludesEveryActiveBookAndTheZeroPageBook |
| speedShowsLibraryBookForecastAndInsufficientData | не перенесён | скорость/прогноз отменены |
| sixRecordsAndCoverageStayInTheirFixedOrder | адаптирован | fourRecordsAndCoverageStayInTheirFixedOrder |
| miniTimerReservesSpaceInsteadOfCoveringFeedOrNavigation | заменён | Evidence проверяет feed/navigation/insets без мини-панели |
| debugCatalogSwitchesAllTenScenariosAndSettingsReturnKeepsContext | адаптирован | debugCatalogExposesOnlyThePageBasedScenarios + settingsAndBackPreserveTheHomeContext |

### HomeEvidenceTest

Оба прежних метода адаптированы: сценарии всех секций/обеих тем и матрица темы×fontScale 1.0/1.3/1.5/2.0×портрет/альбом/reduced motion. Отменённые таймерные кадры заменены кнопкой записи, состояниями страниц/исторических завершений, 4 секциями/4 рекордами. Съёмка идёт до видимого конца блока с перекрытием кадров, не ради фиксированного числа PNG. Добавлены явные theme/scenario/capture/размеры в metadata.

## 10. Число тестов

Локально JVM: **22 executed / 0 failed / 0 errors / 0 skipped** (3 UI-01 + 19 Home). Android suite содержит 34 метода: 10 UI-01 + 22 HomeInteraction + 2 HomeEvidence; фактическое исполнение каждой API подтверждается только скачанными XML итогового Full. CI-script regression: 20 PASS (17 прежних + 3 проверки exit-кода/сбора доказательств). Пороги 22/34 не заменяют содержательное покрытие.

## 11. Quick и Full

Локально debug/lint/JVM/AndroidTestAPK сборка выполнена; lint без замечаний. Существующее предупреждение deprecation createAndroidComposeRule сохранено, assertions не ослаблены и стек не обновлялся. Hosted Quick/Full и точный sourceSha проверяются после push и отражаются в описании PR. Full выполняет весь suite без фильтрации на API 26/35/36. После любого изменения Head требуется новый Full; прежний SHA не считается окончательным.

## 12. API26/35/36

Каждый job: полный instrumentation suite, XML-validator без FAIL/errors/skips, metadata/sourceSha/версия/PNG. Доступность API26 не исключается; UiAutomation.takeScreenshot поддерживает полный дисплей. Эмулятор не заменяет A55. Фактические counts/run/artifact ID — итоговый PR и финальный отчёт владельцу.

## 13. Визуальные доказательства

HomeEvidenceTest снимает реальные окна APK: обе темы, непрерывная лента до footer, каждая 4 секция до своего конца, XP/метрики/активные книги, все 8 сценариев, календарь/границы/начало и конец года / вход дня по книгам, сравнение/4 рекорда/историческая неизвестность; полная матрица тем/шрифтов/ориентаций. sourceSha передаётся CI, version/API/theme/fontScale/orientation/density/locale/scenario записываются в metadata. Нативный foreground focus и стабильные пиксели обязательны, REJECTED кадры означают FAIL, не успешное доказательство. Итоговые ZIP скачиваются, metadata сопоставляется 1:1 с PNG и фактическими размерами, изображения просматриваются. Preview не выдаётся за APK, старые кадры не используются.

Промежуточный Full 43602388813 на Head `3e83205befeaa6768474d6abbe7a3ca8d5658189` выявил при просмотре API 26 перекрытие кнопки демонстрационной панели системной навигацией и два избыточных одинаковых кадра. Для Главной добавлен локальный wrapper панели с прежними токенами и явным Activity inset; общий JournalSheet/UI-01 не изменён. Проверка координат кнопки усилена. Временная подсказка handle убирается обычным касанием содержимого до съёмки; кадры не ретушируются. Уже видимое действие не снимается второй раз, а метрики в матрице записываются последовательностью до третьей подписи. Русские переносы метрик используют hyphenation без смены шрифта/размера. Длинное демонстрационное название согласовано также в днях/итогах/рекордах. Этот промежуточный Full не является окончательной приёмкой; требуется новый Full итогового Head.

## 14. APK и версия

`ru.gleb.readingjournal`, debug **0.2.1-ui02-pages (versionCode 4)**. Это следующий patch/код после старой 0.2.0-ui02(3), без смены applicationId/SDK/зависимостей. Владельцу предоставляется только APK успешного Full итогового Head. Агент скачивает ZIP, независимо вычисляет SHA-256 самого APK, сверяет CI checksum/output-metadata и указывает run/artifact/sourceSha. Полные фактические значения — в итоговом PR/сообщении.

## 15. Подпись

Проверить apksigner verify и SHA-256 сертификата относительно принятой UI-01 APK (артефакт 14054258144/run 43457557668). При несовпадении предупредить об отсутствии обычного обновления поверх неё. Не удалять автоматически приложение или данные и не обещать совместимость до сравнения. Результат фиксируется после скачивания итоговой APK.

## 16. Samsung Galaxy A55

[Отдельный checklist](UI-02-PAGES-GALAXY-A55.md). Все пункты владельца первоначально **NOT RUN**. Точный debug-путь: Настройки → Каталог компонентов · debug → Сценарии главной · debug. Выбор сценария возвращает на Главную; это локальная демонстрация, не личные данные.

## 17. Ограничения

Room/хранилище/реальные записи/диапазоны/агрегация/XP/серии/достижения/импорт-экспорт/сеть не реализованы; формы UI-08 и подробности UI-09 не начаты. READ/F-сценарии NOT RUN. Только UI-state и заранее подготовленные снимки. A55/TalkBack речевой проход требуют владельца; эмуляторные semantics не заменяют их. Golden/pixel-diff без утверждённых владельцем эталонов не заявляется.

## 18. Сохранность PR №2

До работы сохранён снимок GitHub PR №2: e7be6c1a6ffeeb777715624a6e758d9f2387fed0, open/Ready/не merged. Ветка/коммиты/описание/статус не изменяются. После выполнения повторно сверяются head/body/title/draft/state/merged/updated_at и удалённый ref. Старый отчёт доступен только по неизменённому старому Head, не копируется поверх REV-01.

## 19. Остановка

После успешного Full точного итогового Head, проверки XML/APK/всех необходимых изображений установить в новом PR **UI-02 — READY FOR MANUAL ACCEPTANCE**. Это не приёмка владельцем. Merge не выполнять, UI-03 не начинать, ждать отдельного решения по A55. Финальные CI и артефактные данные добавляются в описание PR, чтобы не создавать самоссылочный отчётный коммит после последнего Full.
