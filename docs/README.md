# Документация — REV-02

Редакция 2.1 · 23 ноября 2026 г. Владелец принял D01–D08/A01–A03 и документацию REV-02 без замечаний. PR №5 объединён. FIX-01 принят и объединён через PR №6. UI-02 принят и объединён через PR №7, main `56341fbcb97f932b7c53e7e6982b9934453fb19a`. UI-02.1 ожидает приёмки. Технические отчёты — [FIX-01](acceptance/FIX-01.md), [UI-02](acceptance/UI-02.md), [UI-02.1](acceptance/UI-02.1.md).

## Нормативный комплект этой редакции

| Документ | Назначение |
|---|---|
| [README](../README.md), [AGENTS2.1](../AGENTS.md) | Фактические статусы/границы Codex/новая последовательность; docs-only |
| [REV-02](decisions/REV-02-ui-navigation.md) | Восемь решений/дополнения/перемещение/сохранения/маршруты/trace/Q01–Q03 |
| [v3.1](decisions/Читательский_дневник_спецификация_v3.1.md), [PRD2.1](../PRD.md) | Новый UI-контракт, прежняя бизнес-модель; каталог устойчивых ID/AC |
| [REV-01](decisions/REV-01-reading-without-timer.md) | Принятые страницы/даты/XP/данные и C01–C06; только UI-расположение заменено |
| [ARCHITECTURE2.1](../ARCHITECTURE.md), [ADR-12](decisions/adr/ADR-12-rev02-navigation.md) | Три корня/четыре дочерних/typed events/один UI-state/Back; без физической схемы |
| [UI_DESIGN_SPEC2.1](../UI_DESIGN_SPEC.md) | Новая структура/общая карточка/стрелки/значки/буква; нетронутые точные токены, синхронное приложение А |
| [IMPLEMENTATION_PLAN2.1](../IMPLEMENTATION_PLAN.md) | 33 карточки: REV-02, принятый UI-02, ожидающий приёмки UI-02.1; полная аналитика UI-09, достижения UI-12, библиотека UI-03 |
| [TEST_PLAN2.1](../TEST_PLAN.md) | 27 сохранённых REV-T и 22 будущих REV2-T, маршруты/AC/NFR/QA/A55, без изменения Kotlin-тестов |
| [Реестр решений](decisions/DECISION_REGISTER.md), [У1–У8](decisions/APPROVED_SIMPLIFICATIONS.md) | Приоритет/происхождение, замена только представления |
| [Аудит](audit/PRODUCT_AUDIT.md), [каталог А3](audit/Читательский_дневник_каталог_достижений_А3_утверждён.md) | Сохранённые инварианты и анализ дублирования/технических деталей; пороги/награды прежние |
| [ADR index](decisions/adr/README.md), [дизайн](design/README.md) | Сохранённые принципы/история UI-01 и границы REV-02 |
| [CI](CI.md) | Неизменные workflows/policy/API26/35/36/guard/валидаторы |
| [Отчёт REV-02](acceptance/REV-02.md) | Проверки/полный diff/неизменность кода/токенов/PR №2 и №4/результат Quick |

## История и сохранённые источники

[v3.0](decisions/Читательский_дневник_спецификация_v3.0.md) сохранена с явным историческим статусом и [принятым снимком main](https://github.com/GlebSkorobogatov/reading-journal-android-public/blob/1073e501cd6b2e835404ce6bd89f5bbd6685d547/docs/decisions/Читательский_дневник_спецификация_v3.0.md) как исторический источник после принятия REV-02. [v2.04](decisions/Читательский_дневник_спецификация_v2.04.md), [дорожная карта v2](decisions/Дорожная_карта_Читательский_дневник_UI_first_v2.md), [точный референс](design/APPROVED_REFERENCE.md), историческое тело У1–У8 и ADR-04 доступны для происхождения. Их отменённые UI/таймерные формулировки не второй активный договор. Точные нетронутые токены перенесены в UI_DESIGN_SPEC без изменений.

[UI-01](acceptance/UI-01.md), [A55](acceptance/UI-01-GALAXY-A55.md), [REV-01](acceptance/REV-01.md), [MIGRATION-01](acceptance/MIGRATION-01.md), [публикация](PUBLICATION.md) сохраняют факты/результаты своих дат без переписывания. ADR-10/11 имеют только новые пометки о статусе UI-части/текущем документационном scope, исходное тело сохранено.

PR №2 e7be6c1 и PR №4 8a5b2e0 открыты, не приняты/не объединены, их ветки/история/описания/статусы неприкосновенны. PR №4 Quick/Full PASS/READY FOR MANUAL ACCEPTANCE/A55 NOT RUN относится к прежней структуре; его код/отчёт читаются по точному SHA, не копируются поверх действующей документации. UI-02 принят владельцем 27 ноября 2026 г. UI-03 не начат.

## Контроль целостности и остановка

[FILES.txt](FILES.txt) — все отслеживаемые файлы; [SHA256SUMS.txt](SHA256SUMS.txt) — семь корневых Markdown, все docs Markdown и FILES.txt, без собственной суммы. После добавления разрешённых документов обновить индекс Git и inventory из git ls-files, затем:

```sh
python3 .github/scripts/verify_docs.py --write
python3 .github/scripts/verify_docs.py
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
git diff --check
```

Контроль целостности сохраняется: Markdown/TXT, токены, приложение А, ссылки, инвентарь и суммы. REV-02 объединён через PR №5. Перед merge UI-02 итоговый Head проходит Quick и Full через Draft → Ready.
