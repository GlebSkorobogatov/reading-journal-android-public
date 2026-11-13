# ADR-11 — уровни CI и свежие доказательства

MIGRATION-01, 13 ноября 2026 г. Только инфраструктура; приложение, зависимости и тестовые assertions не меняются.

Решение: отдельный quick workflow для обновлений PR и полный workflow по owner ready_for_review. Один reusable Build сохраняет фиксированные инструменты. Полный gate создаётся только полным workflow и не принимает skipped/failed/cancelled jobs. Docs-only оптимизация относится к quick; full всегда запускает Build и матрицу 26/35/36. Общая concurrency group отменяет устаревшие запуски одного PR, retention 7/30 дней, минимальные read-only permissions и безопасные uploads. Подробные условия — [CI.md](../../CI.md).

Отклонены: полная матрица и на branch push, и на PR synchronize; выдача skipped матрицы за приёмку; уменьшение тестовых порогов; исключение API 26; retry assertions; использование pull_request_target для кода fork; larger runners и приватные CI credentials.

Ограничения: workflow_dispatch до появления workflow в main недоступен штатно, dispatch не обязательно удовлетворяет PR checks. Поэтому первый финальный запуск — owner ready_for_review. Нативные jobs ещё не проверены на GitHub; до разрешения владельца не запускать. Static actionlint и локальные тесты решений дополняют, но не заменяют hosted проверку. Документированные ограничения toolchain из ADR-10 сохраняются.

Никакие настройки репозитория этим ADR автоматически не применяются. После Public и разрешения выполнить полный workflow на точном финальном Head и записать новые ссылки/числа; при неуспехе сохранить BLOCKED.
