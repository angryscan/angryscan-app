🇷🇺 [Русский](README.ru.md)

[![Latest release](https://img.shields.io/github/v/release/angryscan/angrydata-app?sort=semver)](https://github.com/angryscan/angrydata-app/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/angryscan/angrydata-app/total.svg)](https://github.com/angryscan/angrydata-app/releases)
[![Release date](https://img.shields.io/github/release-date/angryscan/angrydata-app?label=release%20date&display_date=published_at&color=orange)](https://github.com/angryscan/angrydata-app/releases/latest)

# A tool with friendly UI to discover sensitive data in 2 clicks
**Angry Data Scanner** is a data security and privacy tool that uses pattern matching to automatically discover sensitive data stored in folders, web pages, S3, database.  
It helps organizations by identifying where sensitive data such as personally identifiable information (PII) and intellectual property is stored.   
The tool provides visibility where your sensitive data is stored.  

- Sensitive data (PII, payments cards etc) can be discover with 2 click
- No administrator rights required to run Angry Data Scanner  
- No additional software installation required  
- Works on Linux, Mac, and Windows

## Discovered sensitive data
The scanner detects the following types of data: 

### Personal Data (numbers)

| Data type           | Country | Example                    |
|---------------------|---------|----------------------------|
| Phone number        | RU      | +7 926 123456             |
| Passport number     | RU      | 4505 857555               |
| Taxpayers number    | RU      | 123456789012              |
| Car number          | RU      | A120AA23                  |
| SNILS              | RU      | 123-456-789 00            |
| OMS                | RU      | 1234567890123456          |

### Personal Data (text)

| Data type | Country | Example                    |
|-----------|---------|----------------------------|
| Full name | RU      | Иван Иванович Иванов       |
| Full name | US      | John Fitzgerald Kennedy    |
| Address   | RU      | Москва, ул. Ленина, д. 1   |
| Address   | US      | Work in progress           |
| E-mail    | International | captainbull@gmail.com |
| Login     | -       | username, user123          |
| Password  | -       | password123, secret        |
| Valuable info | -   | Custom keywords search     |

### Banking Secrecy

| Data type                        | Example                    |
|---------------------------------|----------------------------|
| Payment card number             | 4400 5678 1234 5678       |
| CVV                             | 123, 1234                 |
| Account number                  | 40 817 810 099 910 000 000|
| Cryptocurrency wallet number    | Work in progress           |
| Cryptocurrency recovery-codes   | Work in progress           |

### IT Assets

| Data type        | Example                    |
|------------------|----------------------------|
| Source code files | Finds files with source-code. Source code should be placed in git repository. If source code just lies somewhere is files, this could be a security issue. |
| Passwords       | Finds files with passwords, secrets, API-keys |
| TLS certificates| Finds folders with the most amount of TLS certificates |
| Synthetic data  | Work in progress. General idea if to identity that the data is synthetic. For examples, an excel is generated via Faker. |
| AI-models       | Work in progress. Finds AI-models embedded in files. Goal is to identify hidden AI in your infrastructure. |

### Network & Infrastructure

| Data type        | Example                    |
|------------------|----------------------------|
| IPv4             | 192.168.1.1               |
| IPv6             | 2001:db8::1               |
| Blocked domains  | example.ru                |

### Custom Signatures

| Data type           | Example                    |
|---------------------|----------------------------|
| User-defined patterns| Custom patterns            |

## Supported file types
The scanner supports the following file formats:

| File Type                 | File Format                                          |
|---------------------------|------------------------------------------------------|
| MS Office (tables)        | `.xlsx` `.xls`                                       |
| MS Office (text)          | `.docx` `.doc`                                       |
| MS Office (presentation)  | `.pptx` `.potx` `.ppsx` `.pptm` `.ppt` `.pps` `.pot` |
| Open Office (tables)      | `.ods`                                               |
| Open Office (text)        | `.odt`                                               | 
| Open Office (presentation)| `.odp` `.otp`                                        |
| Adobe                     | `.pdf`                                               |
| Archives                  | `.zip` `.rar`                                        |
| Plain text                | `.txt` `.csv` `.xml` `.json` `.log`                  |

## Supported data sources
The scanner is intended to be a universal tool for scanning everything. Currently, the scanner can connect to the following resources:

| Connector                | Description                                              |
|--------------------------|----------------------------------------------------------|
| Network Folder/Directory | Scans files on remote directory like Windows environment |
| HDD/SDD                  | Scan local hard drive                                    |
| S3                       | Scan files  in S3                                        |
| HTTP/HTTPS               | Scans web site content                                   |
| Database                 | `work in progress`                                       |

## Use cases
We share some practical use cases how Angry Data Scanner is used in real world.

- Leak Hunting team need to scan a network folder and ensure that it does not contain a source code
- An employee scans the network file resource and deletes files containing card numbers to ensure compliance with PCI DSS requirements
- A banking employee scans network file resource to ensure that it does not contain personal data of VIP clients
- A boss scans a file resource of the sales team so they don’t have client contacts on a shared folder
- Law enforcements need to discover a traces of cryptocurrency on a laptop
- A cybersecurity officer need to validate that the database does not contain a personal data

## Key features
- **Ranking**: scanner puts high-value files (with most PII etc) first in the list
- View scanning history
- Download results of a scan in a SCV files
- You can right-clock on a folder and run a scanner to discovery sensitive dat in a floder
- Scanner can run via command line
- You can schedule a scan
- You can stop the scanning process if criteria is met
- You can move files with sensitive data into specified folder
- You can configure a number of CPU cores used for scanning

### Console Mode

AngryData can also be launched in [console mode](https://github.com/angryscan/angrydata-app/blob/main/doc/CONSOLE.md), which is convenient for automation and running tasks without a graphical interface.

## System Requirements
`Windows`, `Linux `
`400MB HDD` `4GB RAM` `1.3Ghz CPU`

## Download

|             |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Windows** | <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner.exe"><img src="https://img.shields.io/badge/Setup-x64-0078D6?style=for-the-badge&logo=windows" alt="Windows stable .exe"></a><br/> <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner-1.3.0-windows-amd64.zip"><img src="https://img.shields.io/badge/portable-x64-0078D6?style=for-the-badge&logo=windows" alt="Windows portable .zip"></a>     |
| **Linux**   | <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner_1.3.0_amd64.deb"><img src="https://img.shields.io/badge/DEB-X64-A81D33?style=for-the-badge&logo=debian" alt="Linux .deb (amd64)"></a><br/> <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner-1.3.0-linux-amd64.tar.gz"><img src="https://img.shields.io/badge/portable-x64-333?style=for-the-badge&logo=linux" alt="Linux portable binary"></a> |
| **MacOS**   | <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner_1.3.0_mac-amd64.zip"><img src="https://img.shields.io/badge/macOS-X64-000000?style=for-the-badge&logo=apple" alt="App amd64"></a> <br/> <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner_1.3.0_mac-amd64.zip"><img src="https://img.shields.io/badge/macOS-ARM64-000000?style=for-the-badge&logo=apple" alt="App Arm64"></a>                   |

