## Console (CLI) mode

Angry Data Scanner provides a CLI implemented as **subcommands**.
Run the application with one of the commands below.

### Global options

| Option              | Description                 |
|---------------------|-----------------------------|
| `-h`, `--help`      | Show help and exit          |
| `-v`, `--version`   | Show app version and exit   |

> Note: the legacy `-c` / `-console` argument is **not** used by the current CLI. Use subcommands (`scan`, `settings`) instead.

### Commands

- `scan`: run a scan and generate a report
- `settings`: view/modify settings, manage user signatures, import/export settings

## `scan` — run a scan and generate a report

### Options

| Option                        | Short | Parameter          | Description |
|------------------------------|-------|--------------------|-------------|
| `--path`                     | `-p`  | `path[;path...]`   | Paths to scan. Multiple paths are separated by semicolons (`;`). **Required**. |
| `--list`                     | `-l`  | -                  | Treat each `--path` entry as a text file that contains paths to scan (one per line). |
| `--extensions`               | `-e`  | `ext1,ext2,...`    | Comma-separated list of file type IDs to scan (see `scan --help` for supported values). Default: loaded from settings. |
| `--matchers`                 | `-m`  | `m1,m2,...`        | Comma-separated list of matchers to use (see `scan --help` for supported values). Default: loaded from settings. |
| `--user-signatures`          | `-us` | `s1,s2,...`        | Comma-separated list of **existing** user signature names to use (see `scan --help`). Default: loaded from settings. |
| `--report`                   | `-r`  | `dir`              | Directory to save the report into. Must already exist. Default: user data directory. |
| `--report-extension`         | `-re` | `csv|xlsx|xml`      | Report file extension. Default: loaded from settings. |
| `--fast`, `--full`           | -     | -                  | Toggle scan mode (fast/full). Default: loaded from settings. |

### Examples

1. Simple directory scan:
   ```
   AngryDataScanner scan -p /path/to/directory
   ```

2. Scan multiple paths (quote `;` on shells):
   ```
   AngryDataScanner scan -p "/path/one;/path/two"
   ```

3. Scan with explicit matchers and report format:
   ```
   AngryDataScanner scan -p /path/to/directory -m Email,Phone -re csv -r /path/for/report
   ```

4. Scan using a file that contains scan paths (one per line):
   ```
   AngryDataScanner scan -p /path/to/paths.txt --list
   ```

## `settings` — view/modify settings

`settings` can run in two modes:

- If a **real console** is available, running `AngryDataScanner settings` with no options starts the **interactive** settings menu.
- Otherwise (e.g. CI, redirected stdin), `AngryDataScanner settings` prints current settings.

### Options (non-interactive)

| Option                        | Short | Parameter                         | Description |
|------------------------------|-------|-----------------------------------|-------------|
| `--interactive`              | `-i`  | -                                 | Run interactive settings menu (requires a real console). Cannot be combined with other flags. |
| `--thread-count`             | `-tc` | `number`                          | Set thread count used by scanning. |
| `--report-extension`         | `-re` | `csv|xlsx|xml`                    | Set default report extension. |
| `--extensions`               | `-e`  | `ext1,ext2,...`                   | Set default file types to scan. |
| `--matchers`                 | `-m`  | `m1,m2,...`                       | Set default matchers. |
| `--user-signatures`          | `-us` | `s1,s2,...`                       | Set default selected user signatures. |
| `--fast`, `--full`           | -     | -                                 | Set fast/full scan mode. |
| `--engine`                   | `-eng`| `HyperScan|Kotlin`                | Set scan engine. |
| `--user-signature-add`       | -     | -                                 | Add a user signature (requires `--user-signature-name` and `--user-signature-signature`). |
| `--user-signature-remove`    | -     | -                                 | Remove a user signature (requires `--user-signature-name`). |
| `--user-signature-replace`   | -     | -                                 | Replace values of a user signature (requires `--user-signature-name` and `--user-signature-signature`). |
| `--user-signature-name`      | -     | `name`                            | Name used with `--user-signature-*` flags. |
| `--user-signature-signature` | -     | `v1,v2,...`                       | Signature values used with `--user-signature-add/replace`. |
| `--export`                   | -     | `all|app|scan|signatures`         | Export settings to `--dir` (for `all`) or `--file` (for others). |
| `--import`                   | -     | `all|app|scan|signatures`         | Import settings from `--dir` / `--file` (replaces current files). |
| `--dir`                      | -     | `dir`                             | Directory for `--export all` / `--import all`. |
| `--file`                     | -     | `file`                            | File path for `--export app|scan|signatures` / `--import app|scan|signatures`. |

### User signatures

You can manage user signature definitions either via flags on `settings` or using the subcommand:

- `AngryDataScanner settings signatures list`
- `AngryDataScanner settings signatures add --name Name --signature AAA,BBB`
- `AngryDataScanner settings signatures remove --name Name`
- `AngryDataScanner settings signatures replace --name Name --signature AAA`

Equivalent operations via flags:

- Add: `AngryDataScanner settings --user-signature-add --user-signature-name Name --user-signature-signature AAA,BBB`
- Remove: `AngryDataScanner settings --user-signature-remove --user-signature-name Name`
- Replace: `AngryDataScanner settings --user-signature-replace --user-signature-name Name --user-signature-signature AAA`

### Examples

1. Print current settings (non-interactive environment):
   ```
   AngryDataScanner settings
   ```

2. Start interactive settings menu:
   ```
   AngryDataScanner settings --interactive
   ```

3. Change default scan engine and enable fast scan:
   ```
   AngryDataScanner settings --engine HyperScan --fast
   ```

4. Export all settings:
   ```
   AngryDataScanner settings --export all --dir /path/to/backup
   ```

5. Import ScanSettings from a file:
   ```
   AngryDataScanner settings --import scan --file /path/to/ScanSettings.json
   ```
