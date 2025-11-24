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
The scanner detects the following types of data: 

### Personal Data (numbers)

| Data type | Local name | Country | Example |
|-----------|---------------|---------|---------|
| Phone number | - | RU | +7 926 3847291 |
| Phone number | - | US | +1 212 5550198 |
| Taxpayer number | INN | RU | 7707083893 |
| Taxpayer number | SSN | US | 536-90-4399 |
| Taxpayer number | RIN | CN | 110101199003078912 |
| Passport | - | RU | 4505 857555 |
| Passport | - | US | 847293641 |
| Insurance number | SNILS | RU | 234-567-890 12 |
| Insurance number | OMS | RU | 9876543210987654 |
| Insurance number | Medicare | US | 1A2B3C4D5E |
| Insurance number | OSAGO policy | RU | ААА3847291847 |
| Driver license | - | RU | 77АВ987654 |
| ID document | Military ID | RU | 3847291847 |
| ID document | Temporary ID | RU | 2938475629 |
| ID document | Residence permit | RU | 8472936418 |
| ID document | Sberknizhka | RU | 2938475629 |
| Date | Birthday | - | 15.03.1985 |
| Date | Death date | RU | 22.11.2023 |
| User identifier | Social user ID | - | 3847291847 |
| Vehicle identifier | VIN | - | 1HGBH41JXMN109186 |
| Vehicle identifier | Vehicle registration number | RU | A120AA23 |
| Legal entity identifier | Legal entity ID | RU | 7707083893 |
| Legal entity identifier | OGRNIP | RU | 315774600001234 |
| Legal entity identifier | OKPO | RU | 38472918 |
| Document number | State registration contract | RU | 293847 |
| Document number | EP certificate number | RU | 84729364182938475629 |
| Document number | Executive document number | RU | 384729 |
| Document number | Cadastral number | RU | 77:01:0001001:1001 |

### Personal Data (text)

| Data type | Local name | Country | Example |
|-----------|---------------|---------|---------|
| Full name | - | RU | Иван Иванович Иванов |
| Full name | - | US | John Smith |
| Contact information | E-mail | - | captainbull@gmail.com |
| Contact information | Address | RU | Москва, ул. Ленина, д. 1 |
| Contact information | Address | US | Work in progress |
| Account credentials | Login | - | username |
| Account credentials | Password | - | 	password123 |
| Certificate | Birth certificate | RU | I-АБ 384729 |
| Certificate | Marriage certificate | RU | II-АБ 384729 |
| Education document | - | RU | 847293 |
| Education document | Education level | RU | Высшее образование |
| Education document | Education license | RU | 384729 |
| Document | Identity document type | RU | Паспорт |
| Document | Inheritance document | RU | 847293 |
| Personal status | Marital status | RU | Женат/Замужем |
| Military information | Military rank | RU | Рядовой |
| Security information | Security affiliation | RU | Допуск |
| Location | Geographic coordinates | - | 55.7558 |
| Legal entity | Legal entity name | RU | ООО "Компания" |

### Banking Secrecy

| Data type | Local name | Country | Example |
|-----------|---------------|---------|---------|
| Payment card | Payment card number | - | 4400 5678 9012 3456 |
| Payment card | CVV | - | 456 |
| Bank account | Bank account (Individual) | RU | 408 028 103 3 5300 5405 83 |
| Bank account | Bank account (Legal entity) | RU | 407 028 103 3 5300 5405 83 |
| Bank account | UID contract bank BKI | RU | 3847291847 |
| Cryptocurrency | Cryptocurrency wallet number | - | Work in progress |
| Cryptocurrency | Cryptocurrency recovery-codes | - | Work in progress |

### IT Assets

| Data type | Local name | Example |
|-----------|---------------|---------|
| IP address | IPv4 | 192.168.1.1 |
| IP address | IPv6 | 2001:db8::1 |
| Source code | Source code files | Finds files with source-code. Source code should be placed in git repository. If source code just lies somewhere is files, this could be a security issue. |
| Certificate | TLS certificates | Finds folders with the most amount of TLS certificates |
| Domain | Blocked domains (RKN) | example.ru |
| Hash | Hash data | SHA256 |
| Synthetic data | - | Work in progress. General idea if to identity that the data is synthetic. For examples, an excel is generated via Faker. |
| AI-models | - | Work in progress. Finds AI-models embedded in files. Goal is to identify hidden AI in your infrastructure. |

### Custom Signatures

It is possible to add custom data search signatures using plain text: `FBR`, `FSB`, `Central bank` or any other.

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
| Database                 | `Work in progress`                                       |

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

