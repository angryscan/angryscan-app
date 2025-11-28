🇷🇺 [Русский](README.ru.md)

[![Latest release](https://img.shields.io/github/v/release/angryscan/angrydata-app?sort=semver)](https://github.com/angryscan/angrydata-app/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/angryscan/angrydata-app/total.svg)](https://github.com/angryscan/angrydata-app/releases)
[![Release date](https://img.shields.io/github/release-date/angryscan/angrydata-app?label=release%20date&display_date=published_at&color=orange)](https://github.com/angryscan/angrydata-app/releases/latest)

# Free Open Source Sensitive Data Discovery Tool
**Angry Data Scanner** is a sensitive data discovery tool that uses pattern matching to automatically discover sensitive data stored in folders, web pages, S3, database.  
It helps organizations by identifying where sensitive data such as personally identifiable information (PII) and intellectual property is stored.   
The tool provides visibility where your sensitive data is stored.  

- Simple user interface
- Sensitive data discovered with 2 clicks
- No admin rights required to run Angry
- Works on Linux, Mac, and Windows

## Sensitive data discovery

### Personal Data (numbers)

| Data type | Local name | Country | Example |
|-----------|---------------|---------|---------|
| Phone number | - | RU | +7 926 3847291 |
| Phone number | - | US | +1 212 5550198 |
| Taxpayer number | ИНН | RU | 7707083893 |
| Taxpayer number | SSN | US | 536-90-4399 |
| Taxpayer number | RIN | CN | 110101199003078912 |
| Passport | - | RU | 4505 857555 |
| Passport | - | US | 847293641 |
| Pension insurance number | СНИЛС | RU | 234-567-890 12 |
| Medical insurance number | ОМС | RU | 9876543210987654 |
| Medical insurance number | Medicare | US | 1A2B3C4D5E |
| Car insurance number | полис ОСАГО | RU | ААА3847291847 |
| Driver license | Водительские права | RU | 77АВ987654 |
| Military ID | Удостоверение личности военнослужащего | RU | 3847291847 |
| Temporary identity document | ВУЛ | RU | 2938475629 |
| Temporary residence permit | РВП | RU | 8472936418 |
| Sberbank book number  | Сберкнижка | RU | 2938475629 |
| Birthday | - | - | 15.03.1985 |
| Death date | - | RU | 22.11.2023 |
| User identifier | Social user ID | - | @3847291847 |
| VIN | - | - | 1HGBH41JXMN109186 |
| Vehicle registration number | Номер авто | RU | A120AA23 |
| Legal entity ID | LEI, BIC/SWIFT | - | 7707083893 |
| Individual entrepreneur identification number  | ОГРНИП | RU | 315774600001234 |
| Tax classifier of enterprises and organizations  | ОКПО | RU | 38472918 |
| State registration number of the contract | Номер записи государственной регистрации договора | RU | 293847 |
| Digital signature certificate number | Сертификат ЭП | RU | 84729364182938475629 |
| Enforcement document number | Номер исполнительного документа | RU | 384729 |
| Cadastral number | Кадастровый номер | RU | 77:01:0001001:1001 |

### Personal Data (text)

| Data type | Local name | Country | Example |
|-----------|---------------|---------|---------|
| Full name | ФИО | RU | Иван Иванович Иванов |
| Full name | Full name | US | John Smith |
| E-mail | - | - | captainbull@gmail.com |
| Address | Адрес | RU | Москва, ул. Ленина, д. 1 |
| Login | - | - | username |
| Password | - | - | password123 |
| Birth certificate | Свидетельство о рождении | RU | I-АБ 384729 |
| Marriage certificate | Свидетельство о браке | RU | II-АБ 384729 |
| Education document | Номер диплома | RU | 847293 |
| Education level | Степень образования | RU | Высшее образование |
| Education license | Образовательная лицензия | RU | 384729 |
| Identity document type | Тип ДУЛ | RU | Паспорт |
| Inheritance certificate number | Cв-во о праве на наследство | RU | 847293 |
| Marital status | Семейное положение | RU | Женат/Замужем |
| Military rank | Воинское звание | RU | Рядовой |
| Geographic coordinates | Geo | - | 55.7558 |
| Legal entity name | Наименование юридического лица | RU | ООО "Компания" |

### PCI DSS
|Data type             | Example              |
|----------------------|----------------------|
| Payment card number  | 4400 5678 9012 3456  |
| CVV                  | 456                  |

### Banking Secrecy

| Data type                   | Country | Example                     |
|------------------------------|---------|-----------------------------|
| Bank account (Individual)    | RU      | 408 028 103 3 5300 5405 83  |
| Bank account (Legal entity)  | RU      | 407 028 103 3 5300 5405 83  |
| UID contract bank BKI        | RU      | 3847291847                  |

### IT Assets

| Data type | Example |
|---------------|---------|
| IPv4 | 192.168.1.1 |
| IPv6 | 2001:db8::1 |
| Source code files | Finds files with source-code. Source code should be placed in git repository. If source code just lies somewhere is files, this could be a security issue. |
| TLS certificates | Finds folders with the most amount of TLS certificates |
| Hash data | SHA-256, MD5, NTLM (NT hash), SHA-1, SHA-512 |

### Custom Signatures

It is possible to add custom data search signatures using plain text: `Secret`, `Password`, `Central bank` or any other.

## Supported file types

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

| Connector                | Description                                              |
|--------------------------|----------------------------------------------------------|
| Network Folder           | Scans files on remote directory like Windows environment |
| HDD/SDD                  | Scan local hard drive                                    |
| S3                       | Scan files  in S3                                        |
| HTTP/HTTPS               | Scans web site content                                   |

## Real life use cases
- A leak рunting team scans network folder and ensure that it does not contain source code
- An employee finds and deletes files containing card numbers to comply with PCI DSS
- A banking employee scans network folder to ensure that it does not contain PII of VIP clients
- A boss scans a shared folder of the sales team so they don’t have client contacts there
- Law enforcements need to discover a traces of cryptocurrency on a laptop
- A cybersecurity officer need to validate that the database does not contain a personal data

## Key features
- **Ranking**: scanner shows high-value files first
- View scanning history
- Download results in a SCV file
- Right-click on a folder to scan it
- Scanner can run via command line
- You can schedule a scan
- The scan can be stopped ic criteria is met
- Move sensitive files into specified folder
- Change a number of CPU cores used in scan
- Configure matchers (PII, PCI DSS …)
- Configure file formats (pdf, excel …)

### Console Mode

AngryData can also be launched in [console mode](https://github.com/angryscan/angrydata-app/blob/main/doc/CONSOLE.md), which is convenient for automation and running tasks without a graphical interface.

## System Requirements
`Windows`, `Linux `
`400MB HDD` `4GB RAM` `1.3Ghz CPU`

## Download

|             |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Windows** | <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner.exe"><img src="https://img.shields.io/badge/Setup-x64-0078D6?style=for-the-badge&logo=windows" alt="Windows stable .exe"></a><br/> <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner-1.4.0-windows-amd64.zip"><img src="https://img.shields.io/badge/portable-x64-0078D6?style=for-the-badge&logo=windows" alt="Windows portable .zip"></a>     |
| **Linux**   | <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner_1.4.0_amd64.deb"><img src="https://img.shields.io/badge/DEB-X64-A81D33?style=for-the-badge&logo=debian" alt="Linux .deb (amd64)"></a><br/> <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner-1.4.0-linux-amd64.tar.gz"><img src="https://img.shields.io/badge/portable-x64-333?style=for-the-badge&logo=linux" alt="Linux portable binary"></a> |
| **MacOS**   | <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner-1.4.0-mac-amd64.zip"><img src="https://img.shields.io/badge/macOS-X64-000000?style=for-the-badge&logo=apple" alt="App amd64"></a> <br/> <a href="https://github.com/angryscan/angrydata-app/releases/latest/download/angry-data-scanner-1.4.0-mac-aarch64.zip"><img src="https://img.shields.io/badge/macOS-ARM64-000000?style=for-the-badge&logo=apple" alt="App Arm64"></a>                   |

