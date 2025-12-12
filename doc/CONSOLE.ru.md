## Консольный режим (CLI)

В Angry Data Scanner консольный интерфейс реализован через **подкоманды**.
Запускайте приложение с одной из команд ниже.

### Глобальные параметры

| Параметр            | Описание                    |
|---------------------|-----------------------------|
| `-h`, `--help`      | Показать справку и выйти    |
| `-v`, `--version`   | Показать версию и выйти     |

> Примечание: старый аргумент `-c` / `-console` **не используется** текущим CLI. Используйте подкоманды (`scan`, `settings`).

### Команды

- `scan`: запуск сканирования и генерация отчёта
- `settings`: просмотр/изменение настроек, управление пользовательскими сигнатурами, импорт/экспорт настроек

## `scan` — сканирование и отчёт

### Параметры

| Параметр                     | Короткий | Значение              | Описание |
|-----------------------------|----------|------------------------|----------|
| `--path`                    | `-p`     | `путь[;путь...]`       | Пути для сканирования. Несколько путей разделяются точкой с запятой (`;`). **Обязательно**. |
| `--list`                    | `-l`     | -                      | Считать, что каждый элемент `--path` — это текстовый файл со списком путей для сканирования (по одному на строку). |
| `--extensions`              | `-e`     | `ext1,ext2,...`        | Список ID типов файлов для сканирования (см. `scan --help`). По умолчанию: из настроек. |
| `--matchers`                | `-m`     | `m1,m2,...`            | Список матчеров (функций обнаружения) (см. `scan --help`). По умолчанию: из настроек. |
| `--user-signatures`         | `-us`    | `s1,s2,...`            | Список **существующих** пользовательских сигнатур (по имени) (см. `scan --help`). По умолчанию: из настроек. |
| `--report`                  | `-r`     | `директория`           | Директория для сохранения отчёта. Должна уже существовать. По умолчанию: директория данных пользователя. |
| `--report-extension`        | `-re`    | `csv|xlsx|xml`          | Формат отчёта. По умолчанию: из настроек. |
| `--fast`, `--full`          | -        | -                      | Переключение режима (быстрый/полный). По умолчанию: из настроек. |

### Примеры

1. Простое сканирование директории:
   ```
   AngryDataScanner scan -p /путь/к/директории
   ```

2. Сканирование нескольких путей (в оболочках `;` лучше экранировать/кавычить):
   ```
   AngryDataScanner scan -p "/путь/1;/путь/2"
   ```

3. Сканирование с явными матчерами и форматом отчёта:
   ```
   AngryDataScanner scan -p /путь/к/директории -m Email,Phone -re csv -r /путь/для/отчета
   ```

4. Сканирование по списку путей из файла (по одному на строку):
   ```
   AngryDataScanner scan -p /путь/к/paths.txt --list
   ```

## `settings` — просмотр/изменение настроек

Команда `settings` работает в двух режимах:

- Если доступна **настоящая консоль**, запуск `AngryDataScanner settings` без параметров открывает **интерактивное** меню настроек.
- Иначе (например, CI или перенаправленный stdin) `AngryDataScanner settings` печатает текущие настройки.

### Параметры (неинтерактивно)

| Параметр           | Короткий | Значение                          | Описание |
|--------------------|----------|-----------------------------------|----------|
| `--interactive`    | `-i`     | -                                 | Интерактивное меню настроек (требует реальную консоль). Нельзя комбинировать с другими флагами. |
| `--thread-count`   | `-tc`    | `число`                           | Количество потоков, используемое при сканировании. |
| `--report-extension` | `-re`  | `csv|xlsx|xml`                    | Формат отчёта по умолчанию. |
| `--extensions`     | `-e`     | `ext1,ext2,...`                   | Типы файлов для сканирования по умолчанию. |
| `--matchers`       | `-m`     | `m1,m2,...`                       | Матчеры по умолчанию. |
| `--user-signatures`| `-us`    | `s1,s2,...`                       | Выбранные пользовательские сигнатуры по умолчанию. |
| `--fast`, `--full` | -        | -                                 | Установить режим сканирования (быстрый/полный). |
| `--engine`         | `-eng`   | `HyperScan|Kotlin`                | Движок сканирования. |
| `--user-signature-add`       | -      | -                                 | Добавить пользовательскую сигнатуру (требует `--user-signature-name` и `--user-signature-signature`). |
| `--user-signature-remove`    | -      | -                                 | Удалить пользовательскую сигнатуру (требует `--user-signature-name`). |
| `--user-signature-replace`   | -      | -                                 | Заменить значения пользовательской сигнатуры (требует `--user-signature-name` и `--user-signature-signature`). |
| `--user-signature-name`      | -      | `имя`                              | Имя для флагов `--user-signature-*`. |
| `--user-signature-signature` | -      | `v1,v2,...`                        | Значения сигнатуры для `--user-signature-add/replace`. |
| `--export`         | -        | `all|app|scan|signatures`         | Экспорт настроек в `--dir` (для `all`) или в `--file` (для остальных). |
| `--import`         | -        | `all|app|scan|signatures`         | Импорт настроек из `--dir` / `--file` (перезаписывает текущие файлы). |
| `--dir`            | -        | `директория`                      | Директория для `--export all` / `--import all`. |
| `--file`           | -        | `файл`                            | Файл для `--export app|scan|signatures` / `--import app|scan|signatures`. |

### Пользовательские сигнатуры

Управлять определениями пользовательских сигнатур можно через подкоманду:

- `AngryDataScanner settings signatures list`
- `AngryDataScanner settings signatures add --name Name --signature AAA,BBB`
- `AngryDataScanner settings signatures remove --name Name`
- `AngryDataScanner settings signatures replace --name Name --signature AAA`

То же самое через флаги:

- Добавить: `AngryDataScanner settings --user-signature-add --user-signature-name Name --user-signature-signature AAA,BBB`
- Удалить: `AngryDataScanner settings --user-signature-remove --user-signature-name Name`
- Заменить: `AngryDataScanner settings --user-signature-replace --user-signature-name Name --user-signature-signature AAA`

### Примеры

1. Печать текущих настроек (неинтерактивная среда):
   ```
   AngryDataScanner settings
   ```

2. Запуск интерактивного меню:
   ```
   AngryDataScanner settings --interactive
   ```

3. Сменить движок и включить быстрый режим:
   ```
   AngryDataScanner settings --engine HyperScan --fast
   ```

4. Экспорт всех настроек:
   ```
   AngryDataScanner settings --export all --dir /путь/к/backup
   ```

5. Импорт ScanSettings из файла:
   ```
   AngryDataScanner settings --import scan --file /путь/к/ScanSettings.json
   ```
