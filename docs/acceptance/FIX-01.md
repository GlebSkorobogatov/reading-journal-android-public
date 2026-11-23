# FIX-01 — устранение пяти lint-ошибок версий

23 ноября 2026 г. Техническая задача, не реализация UI-02. **Контрольная точка этого файла — локальные проверки до GitHub CI.** Итоговые Head, номер единственного PR, Quick/Full, артефакты и статус технической приёмки публикуются после фактической проверки в описании PR и отчёте владельцу, без изменения проверенного Head ради собственных SHA/ссылок. Не считать ожидаемые результаты выполненными.

## 1. Исходное состояние и разрешение

Публичный `GlebSkorobogatov/reading-journal-android-public`, принятый main `1073e501cd6b2e835404ce6bd89f5bbd6685d547` подтверждён GitHub и Git. Новая ветка `codex/fix-01-toolchain-lint` создана строго от этого коммита, не от REV-02. PR №5 открыт/Ready/не merged на `fbade14761190ca9e95aceb0b5219d87ca9090ae`; его документация REV-02 принята владельцем 23 ноября без замечаний, но не вошла в main. Quick45142065506 PASS, Full45142164348 FAIL. PR №2 e7be6c1 и PR №4 8a5b2e0 открыты и сохраняются.

Основание — отдельное задание владельца FIX-01: исправить четыре версии без подавления пяти ошибок, подготовить отдельный проверенный технический PR, остановиться до merge. Изменение compileSdk 37 → 37.1 дополнительно явно разрешено владельцем 23 ноября после предъявления несовместимости Compose BOM. Это не разрешение на изменение targetSdk/minSdk/Kotlin/Java/CI/UI или на merge любого PR.

## 2. Пять исходных ошибок, четыре версии

Источник: `lint-results-debug.xml` из [build-reports 14803200465](https://github.com/GlebSkorobogatov/reading-journal-android-public/actions/runs/45142164348/artifacts/14803200465). Все пять диагностик подтверждены владельцем полным отчётом; агент дополнительно прочитал job log, где видны пять ошибок и первая причина. Недоступное скачивание старого архива не выдано за независимый разбор XML.

| Lint ID / объект | Было | Стало | Изменение |
|---|---|---|---|
| AndroidGradlePluginVersion / Gradle | 9.9.1 | 9.9.2 | Wrapper URL + официальная SHA-256 |
| AndroidGradlePluginVersion / AGP | 9.5.1 | 9.5.2 | agp в catalog |
| GradleDependency / Compose BOM | 2026.09.00 | 2026.11.01 | compose-bom в catalog |
| GradleDependency / lifecycle-runtime-compose | 2.11.0 | 2.11.1 | общая lifecycle в catalog |
| GradleDependency / lifecycle-viewmodel-compose | 2.11.0 | 2.11.1 | та же общая lifecycle |

`abortOnError = true` и `warningsAsErrors = true` сохранены буквально. Не добавлены suppression/baseline/исключения lint, динамические версии или пропуски CI. Остальные версии/алиасы/структура зависимостей сохранены.

## 3. Проверка официальной совместимости до изменения версий

| Проверка | Результат и первичный источник |
|---|---|
| Gradle 9.9.2 | Стабильный patch; [release notes](https://docs.gradle.org/9.9.2/release-notes.html), [Java compatibility](https://docs.gradle.org/9.9.2/userguide/compatibility.html): Java 17 поддерживается. |
| AGP 9.5.2 | [Официальный stable API index](https://developer.android.com/reference/tools/gradle-api), [Patch 2](https://developer.android.com/studio/releases/fixed-bugs/studio/2026.2.2.html) и [Google Maven POM](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/9.5.2/gradle-9.5.2.pom) подтверждают выпуск. [Совместимость AGP 9.5](https://developer.android.com/build/releases/agp-9-5-0-release-notes): Gradle≥9.6.0, JDK17, Build Tools36.0.0, семейство API37. |
| Compile SDK 37.1 | [Матрица SDK/AGP](https://developer.android.com/studio/releases) требует AGP≥9.3.1 для37.1; 9.5.2 удовлетворяет. Стабильная platform37.1 revision1 установлена штатным Android CLI; ApiLevel37.1, PreviewSdkInt0, Codename/Beta пусты. |
| Kotlin / compiler plugin | 2.4.21/2.4.21 сохраняются; AGP built-in Kotlin + существующий явный KGP. [Матрица Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html) полностью тестирует внешний KGP2.4.20–21 только до Gradle9.7.0/AGP9.3.1; выбранная комбинация вне этой таблицы, как и принятая база. Документ допускает более новые версии с ограничениями. Полная сертификация JetBrains не заявляется; конкретное сочетание должно пройти сборку/lint/JVM/API26/35/36. |
| Compose BOM | [Google Maven POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.11.01/compose-bom-2026.11.01.pom) скачан, HTTP200: UI/ui-test-junit4 1.13.0, Material3 1.5.0; только stable. [Официальный ноябрьский выпуск](https://developer.android.com/blog/posts/jetpack-compose-november-2026-release) требует Kotlin≥2.2 и compileSdk37.1. Compiler plugin следует Kotlin, не BOM. |
| Подтверждение SDK из AAR | Скачан [ui-android-1.13.0.aar](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.13.0/ui-android-1.13.0.aar), прочитаны aar-metadata: minCompileSdk37, minCompileMinorSdk1, minAndroidGradlePluginVersion9.1.0; manifest minSdk24. Поэтому BOM с прежним compileSdk37.0 несовместим; minSdk26 удовлетворяет. |
| Lifecycle | Оба [runtime POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose/2.11.1/lifecycle-runtime-compose-2.11.1.pom) / [viewmodel POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/2.11.1/lifecycle-viewmodel-compose-2.11.1.pom) скачаны HTTP200; [release notes](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.1) подтверждают stable patch 18 ноября. |

Минимальный согласованный SDK diff: `compileSdk { version = release(37) { minorApiLevel = 1 } }` по [официальному DSL](https://developer.android.com/reference/tools/gradle-api/9.5/com/android/build/api/dsl/CompileSdkSpec). `targetSdk = 37`, `minSdk = 26`, bytecode/JDK17, Kotlin/compiler2.4.21, SDK Build Tools36.0.0, API26/35/36 CI не изменены. SDK37.1 установлен в среду сборки, не добавлен в Git. Штатная автоустановка недостающей платформы AGP проверяется существующим CI без изменения workflow. Код не адаптируется к новым API; если выявится обязательное изменение кода/тестов/CI, это блокер за пределами текущего разрешения.

## 4. Gradle Wrapper и контрольные суммы

Штатная процедура выполнена дважды: сначала прежним Gradle9.9.1, затем новым9.9.2:

```sh
./gradlew wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum 971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b
./gradlew --version
```

Обе wrapper-задачи PASS. Wrapper скачал официальный ZIP по TLS, проверил закреплённую SHA-256 перед распаковкой; новый Gradle реально запущен (9.9.2, revision53873705aa44e5f145f335eefd2a254f2adff009). Launcher/daemon: Temurin17.0.21+9.

- [Официальная SHA-256 ZIP](https://services.gradle.org/distributions/gradle-9.9.2-bin.zip.sha256): `971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b`, закреплена в distributionSha256Sum.
- [Официальная SHA-256 Wrapper JAR](https://services.gradle.org/distributions/gradle-9.9.2-wrapper.jar.sha256): `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8`, совпала с фактическим JAR после повторной генерации. JAR и оба launcher-скрипта идентичны базе; их изменение не требуется. Прочие wrapper properties, URL validation и timeout сохранены.

## 5. Локальные проверки

Среда: Linux, официальный JDK17.0.21+9/Gradle9.9.2, стабильная SDK37.1. Команда:

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
python3 .github/scripts/verify_docs.py --write
python3 .github/scripts/verify_docs.py
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
git diff --check
```

| Проверка | Фактический результат на локальной контрольной точке |
|---|---|
| Gradle --version / Wrapper | PASS: 9.9.2, Java17.0.21+9; обе wrapper-задачи успешны |
| Полная команда Gradle выше | PASS: exit0, BUILD SUCCESSFUL, 86 tasks executed |
| Assemble Debug / AndroidTest | PASS: оба APK собраны |
| Полный lint-results-debug.xml | PASS: разобран XML целиком, 0 issue; все пять исходных диагностик отсутствуют, новых lint errors/warnings нет |
| JVM XML | PASS: 3 testcase, 0 failures/errors/skips |
| Instrumentation API26/35/36 локально | NOT RUN: проверяются обязательным GitHub Full, локальная сборка AndroidTest не равна запуску |
| Документы/inventory/SHA-256/ссылки, CI-script tests/diff | PASS перед коммитом: штатные verify_docs, 17 существующих CI-script tests, diff --check; итоговые CI-подтверждения в PR |

Отдельно классифицированы **7 предупреждений Kotlin compiler** в неизменённых Android-тестах: прежний createAndroidComposeRule помечен deprecated в Compose1.13. Это не lint issue, не ошибка компиляции и не основание переписывать тесты в FIX-01. Предупреждения не скрыты/не подавлены; миграция к v2 API меняет coroutine semantics и требует отдельного scope, если понадобится. Обязательное прежнее покрытие и assertions сохранены; достаточность конкретной комбинации проверяется полным runtime suite в CI.

## 6. Изменённые пути и сохранность

Разрешённый diff относительно main:

1. gradle/wrapper/gradle-wrapper.properties — версия9.9.2/официальная SHA-256.
2. gradle/libs.versions.toml — AGP/BOM/Lifecycle, остальные версии и все алиасы прежние.
3. app/build.gradle.kts — только отдельно разрешённый compileSdk37.1.
4. README.md — актуальные технические версии/SDK и ссылка на отчёт.
5. docs/decisions/adr/ADR-10-ui01-toolchain-and-shell.md — отдельная техническая пометка; исходное историческое тело не переписано.
6. docs/acceptance/FIX-01.md — этот отчёт.
7. docs/FILES.txt — штатный инвентарь нового отчёта.
8. docs/SHA256SUMS.txt — штатные суммы документов, без самоссылки.

Проверка границ PASS: каталог отличается только тремя значениями versions; app/build.gradle.kts — только compileSdk minor; Wrapper properties — только URL/сумма. Все 79 прочих исходных tracked files/режимы идентичны принятой базе, включая весь app/src (код/тесты/ресурсы/Manifest), workflows/policy, Kotlin/JVM/target/minSdk и все продуктовые документы/токены. Wrapper JAR/launchers не менялись. ADR-10 получил только префикс, исходное тело сохранено. Новые документы REV-02/v3.1/ADR-12 в ветку не переносились. Состояния и Head PR №2/№4/№5 повторно проверяются после CI; никаких мутаций этих PR в FIX-01 не выполняется.

## 7. GitHub CI и остановка

Один новый Draft PR из `codex/fix-01-toolchain-lint` в main; точный номер/Head/ссылки — [единственный PR ветки](https://github.com/GlebSkorobogatov/reading-journal-android-public/pulls?q=head%3Acodex%2Ffix-01-toolchain-lint). На контрольной точке записи этого файла Quick/Full/Compose/Full acceptance ещё **NOT RUN**, не PASS. После push Quick точного Head, затем Ready только после PASS, автоматически запущенный Full не отменять. Минимумы остаются3 JVM/10 Compose на каждой26/35/36; полный suite/строгие валидаторы и Full acceptance неизменны. Исходная policy task UI-01 сохраняется по запрету менять CI policy; это не новая UI-задача.

Только после фактического PASS всех обязательных jobs, проверки полного lint/XML/артефактов/контрольной суммы APK допускается **FIX-01 — READY FOR TECHNICAL ACCEPTANCE** в итоговом отчёте/описании PR. APK служебная debug из точного Full Head; версия приложения остаётся0.1.1-ui01/code2/applicationId ru.gleb.readingjournal, новая функциональность не заявляется. SHA-256 конкретного файла и опубликованная сумма сверяются после выполнения CI; локальная APK не подменяет APK CI.

Merge FIX-01/REV-02 не разрешён. После технических проверок остановиться; отдельная приёмка/разрешение владельца нужны для FIX-01 merge, затем отдельная синхронизация PR №5 с новым main/повторные Quick и Full совместного состояния. PR №5 не менять в этом задании. UI-02/UI-03 не начинать; программная реализация REV-02 не выполняется. A55/бизнес-логика/новый UI — NOT RUN, не входят в FIX-01.
