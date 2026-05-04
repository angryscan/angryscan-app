-- Test data for ConnectorClickHouseIntegrationTest
-- All data is fake/generated, no real PII
-- Mirrors postgres/init-test-postgres.sql translated to ClickHouse dialect

-- ============================================================
-- 1. employees (small table, ~11 rows)
--    Sources: TestText.txt, first.csv, cardNumber, inns, snils
-- ============================================================
CREATE TABLE employees
(
    id            UInt64,
    full_name     Nullable(String),
    email         Nullable(String),
    phone         Nullable(String),
    passport      Nullable(String),
    snils         Nullable(String),
    inn           Nullable(String),
    card_number   Nullable(String),
    address       Nullable(String),
    oms           Nullable(String),
    vehicle_reg   Nullable(String),
    login         Nullable(String),
    password_hash Nullable(String)
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO employees (id, full_name, email, phone, passport, snils, inn, card_number, address, oms, vehicle_reg, login, password_hash)
VALUES (1, 'Анатолий Анатольевич Анатоль', 'test@gmail.com', '88005553535', '4132 234231', '87647163459', '732557279170',
        '4276 8070 1492 7948', 'г. Санкт-Петербург, пр. Бульварный, д. 4 стр. 86', '7750900873002404', 'в404сх 99',
        'Voldemort643s', 'assword432'),
       (2, 'Иванов Анатолий Анатольевич', 'test2@gmail.com', '88005555353', '4131 234232', NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL),
       (3, 'Смирнов Анатолий Анатольевич', 'test@gmail.com', '88005553535', '4132 234231', '87647163459', '732557279170',
        '4276 8070 1492 7948', 'г. Санкт-Петербург, пр. Бульварный, д. 4 стр. 86', '7750900873002404', 'в404сх799',
        'Voldemort643s', 'assword432'),
       (4, 'Козлов Дмитрий Петрович', 'kozlov.dp@yandex.ru', '+79161234567', '4501 987654', '126 029 036 24',
        '772459534170', '4303 8013 0890 0569', 'г. Москва, ул. Ленина, д. 10', NULL, NULL, NULL, NULL),
       (5, 'Новикова Елена Васильевна', 'novikova@mail.ru', '+79035551234', '4510 112233', '162-834-652 79',
        '771907341008', '4662 9949 5547 2514', 'г. Новосибирск, ул. Красный проспект, д. 50', NULL, NULL, NULL, NULL),
       (6, 'Морозов Алексей Игоревич', 'morozov.ai@inbox.ru', '+74951234567', '4515 445566', '187-220-276-69',
        '772403587357', '4298 4987 4439 0799', 'г. Екатеринбург, ул. Мира, д. 25 кв. 3', NULL, NULL, NULL, NULL),
       (7, 'Волкова Ольга Николаевна', 'volkova@company.com', '+78126543210', '4520 778899', '057 033 028 27',
        '771871997347', '4468 2727 8411 6362', 'г. Казань, ул. Баумана, д. 15', NULL, NULL, NULL, NULL),
       (8, 'Соловьёв Михаил Александрович', 'solovev@test.org', '+79997776655', '4525 334455', '13378907489',
        '771822622916', '4861 0368 6714 3293', 'г. Самара, ул. Победы, д. 100', NULL, NULL, NULL, NULL),
       (9, 'Васильева Анна Дмитриевна', 'vasileva.ad@gmail.com', '+79261112233', '4530 667788', '152232898 47',
        '583410778676', '4583984644874606', 'г. Краснодар, ул. Северная, д. 7', '7755320882002755', NULL, NULL, NULL),
       (10, 'Зайцев Сергей Владимирович', 'zaytsev@proton.me', '+79031239876', '4535 990011', '199-510-399 13', NULL,
        '1234432112344321', 'г. Ростов-на-Дону, пр. Ворошиловский, д. 33', NULL, NULL, NULL, NULL),
       (11, 'Иванов Иван Иванович', 'ivanov@example.com', '+79001234567', '1213 141516', NULL, '849600020355', NULL,
        'Владимир, ул.Мира д 11 кв 22', NULL, NULL, NULL, NULL);


-- ============================================================
-- 2. network_logs (small table, ~10 rows)
--    Sources: ip.txt, ipv6.txt
-- ============================================================
CREATE TABLE network_logs
(
    id             UInt64,
    source_ip      Nullable(String),
    destination_ip Nullable(String),
    log_message    Nullable(String),
    logged_at      DateTime DEFAULT now()
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO network_logs (id, source_ip, destination_ip, log_message)
VALUES (1, '192.168.1.1', '10.31.129.98', 'Internal traffic detected'),
       (2, '172.20.20.20', '84.252.149.90', 'Outbound connection established'),
       (3, '10.31.129.98', '192.168.1.1', 'Response received from gateway'),
       (4, '84.252.149.90', '172.20.20.20', 'External service response'),
       (5, '2001:0db8:0000:0000:0000:ff00:0042:8329', '::1', 'IPv6 loopback test'),
       (6, '2001:db8::ff00:42:8329', '0000:0000:0000:0000:0000:0000:0000:0001', 'IPv6 full address test'),
       (7, '::ffff:192.1.56.10', 'ABCD:ABCD:ABCD:ABCD:ABCD:ABCD:192.168.158.190', 'IPv6-mapped IPv4 address'),
       (8, '1080:0:0:0:8:800:200C:417A', '::FFFF:129.144.52.38', 'Mixed format test'),
       (9, '::192.1.56.10', '0:0:0:0:0:ffff:192.1.56.10', 'Tunneling format test'),
       (10, '0:0:0:0:0:0:192.1.56.10', '::129.144.52.38', 'IPv4-compatible IPv6 test');


-- ============================================================
-- 3. payments (small table, ~8 rows)
--    Sources: cardNumber/smth.txt, TestText.txt
-- ============================================================
CREATE TABLE payments
(
    id              UInt64,
    cardholder_name Nullable(String),
    card_number     Nullable(String),
    account_number  Nullable(String),
    cvv             Nullable(String),
    amount          Nullable(Decimal(10, 2))
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO payments (id, cardholder_name, card_number, account_number, cvv, amount)
VALUES (1, 'Анатоль А.А.', '4276 8070 1492 7948', '40802810335300540583', '435', 15000.50),
       (2, 'Козлов Д.П.', '4303 8013 0890 0569', '40817810099910004321', NULL, 3200.00),
       (3, 'Новикова Е.В.', '4662 9949 5547 2514', '40702810500000012345', NULL, 87650.75),
       (4, 'Морозов А.И.', '4298 4987 4439 0799', '40817810200000054321', NULL, 420.00),
       (5, 'Волкова О.Н.', '4468 2727 8411 6362', '40820810100000099876', '112', 9999.99),
       (6, 'Соловьёв М.А.', '4861 0368 6714 3293', NULL, NULL, 1500.00),
       (7, 'Васильева А.Д.', '4583984644874606', NULL, '789', 55000.00),
       (8, 'Зайцев С.В.', '1234432112344321', '40702810999000067890', NULL, 100.00);


-- ============================================================
-- 4. bulk_emails (large table, ~5000 rows via numbers())
--    Inspired by: veryLong/very_long.csv (60K sample@mail.ru)
-- ============================================================
CREATE TABLE bulk_emails
(
    id         UInt64,
    email      Nullable(String),
    phone      Nullable(String),
    created_at Nullable(DateTime)
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO bulk_emails (id, email, phone, created_at)
SELECT (number + 1) AS id,
       concat('user', toString(number + 1), '@mail.ru') AS email,
       concat('+7927572', leftPad(toString(number + 1), 4, '0')) AS phone,
       addMinutes(toDateTime('2022-01-01 00:00:00'), toInt32(number + 1)) AS created_at
FROM numbers(5000);


-- ============================================================
-- 5. system_logs (large table, ~2000 rows via numbers())
--    Inspired by: veryLong/very_long.log (10K log entries)
-- ============================================================
CREATE TABLE system_logs
(
    id            UInt64,
    log_timestamp Nullable(DateTime),
    process_id    Nullable(String),
    thread_id     Nullable(String),
    phone         Nullable(String),
    message       Nullable(String)
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO system_logs (id, log_timestamp, process_id, thread_id, phone, message)
SELECT (number + 1) AS id,
       addSeconds(toDateTime('2022-11-07 20:23:04'), toInt32(number + 1)) AS log_timestamp,
       '00001A50' AS process_id,
       '000024BC' AS thread_id,
       '+79123455667' AS phone,
       concat('CFG: Check DB integrity... Element (idx=', toString(number % 100), ') processed') AS message
FROM numbers(2000);


-- ============================================================
-- 6. documents (large table, ~2000 rows via numbers())
--    Inspired by: veryLong/very_long.txt (Lorem ipsum + PII)
-- ============================================================
CREATE TABLE documents
(
    id           UInt64,
    title        Nullable(String),
    body         Nullable(String),
    author_email Nullable(String),
    author_inn   Nullable(String)
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO documents (id, title, body, author_email, author_inn)
SELECT (number + 1) AS id,
       concat('Document #', toString(number + 1)) AS title,
       concat('Lorem ipsum dolor sit amet, consectetur adipiscing elit. ',
              'Ut venenatis dapibus nibh, ut consectetur elit. ',
              'Contact: user', toString(number + 1), '@example.com, INN: 7724',
              leftPad(toString(number + 1), 8, '0')) AS body,
       concat('user', toString(number + 1), '@example.com') AS author_email,
       concat('7724', leftPad(toString(number + 1), 8, '0')) AS author_inn
FROM numbers(2000);


-- ============================================================
-- 7. citizens_ru (~15 rows, Russian documents)
--    Generated data for: Birthday, FullName, DriverLicense,
--    MilitaryID, OGRNIP, OKPO, OSAGOPolicy, SberBook,
--    CadastralNumber, StateRegContract, LegalEntityId,
--    LegalEntityName
-- ============================================================
CREATE TABLE citizens_ru
(
    id                 UInt64,
    full_name          Nullable(String),
    birthday           Nullable(Date),
    driver_license     Nullable(String),
    military_id        Nullable(String),
    ogrnip             Nullable(String),
    okpo               Nullable(String),
    osago_policy       Nullable(String),
    sberbook           Nullable(String),
    cadastral_number   Nullable(String),
    state_reg_contract Nullable(String),
    legal_entity_id    Nullable(String),
    legal_entity_name  Nullable(String),
    notes              Nullable(String)
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO citizens_ru (id, full_name, birthday, driver_license, military_id, ogrnip, okpo, osago_policy, sberbook,
                         cadastral_number, state_reg_contract, legal_entity_id, legal_entity_name, notes)
VALUES (1, 'Иванов Иван Иванович', '1990-05-15', '77 14 567890', 'АБ 1234567', '312774600000012', '12345678',
        'ХХХ 0123456789', '42307810100000012345', '77:01:0001234:56', '77-77/001-77/001/001/2020-1234', '1027700132195',
        'ООО "Ромашка"', 'Дата рождения: 15.05.1990'),
       (2, 'Петрова Мария Сергеевна', '1985-12-03', '50 22 123456', NULL, '315503400000045', '98765432', 'ЕЕЕ 9876543210',
        '42307810200000098765', '50:03:0004567:89', '50-50/002-50/002/002/2021-5678', '1035000567890', 'ООО "Василёк"',
        'Дата рождения: 03.12.1985'),
       (3, 'Сидоров Алексей Петрович', '1978-03-22', '99 08 345678', 'ВГ 7654321', '316770100000078', '45678901',
        'ККК 1122334455', '42307810300000045678', '77:05:0007890:12', '77-77/003-77/003/003/2019-9012', '1027739123456',
        'АО "Подсолнух"', 'Дата рождения: 22.03.1978'),
       (4, 'Кузнецова Елена Александровна', '1995-07-10', '78 15 901234', NULL, '318784700000034', '23456789', NULL,
        '42307810400000023456', '78:06:0001122:34', NULL, '1027800987654', 'ИП Кузнецова Е.А.',
        'Дата рождения: 10.07.1995'),
       (5, 'Попов Дмитрий Николаевич', '1982-11-28', '39 03 567891', 'ДЕ 9876543', '312390300000056', '67890123',
        'МММ 5544332211', '42307810500000067890', '39:01:0003344:56', '39-39/004-39/004/004/2022-3456', '1023900654321',
        'ООО "Калина"', 'Дата рождения: 28.11.1982'),
       (6, 'Новикова Татьяна Олеговна', '2000-01-05', '63 19 234567', NULL, '319630500000089', '34567890',
        'ННН 6677889900', NULL, '63:02:0005566:78', '63-63/005-63/005/005/2023-7890', '1036300112233', 'ООО "Берёзка"',
        'Дата рождения: 05.01.2000'),
       (7, 'Федоров Максим Викторович', '1973-09-17', '47 06 789012', 'ЖЗ 3456789', '314470200000023', '78901234',
        'РРР 1234509876', '42307810600000078901', '47:03:0009988:90', NULL, '1024700445566', 'ПАО "Кедр"',
        'Дата рождения: 17.09.1973'),
       (8, 'Морозова Анна Викторовна', '1998-06-21', '16 11 456789', NULL, '311160800000067', '56789012', NULL,
        '42307810700000056789', '16:04:0002233:45', '16-16/006-16/006/006/2020-2345', '1021600778899', 'ООО "Ландыш"',
        'Дата рождения: 21.06.1998'),
       (9, 'Волков Артём Сергеевич', '1988-04-30', '24 07 123098', 'ИК 6543210', '313240900000045', '89012345',
        'ССС 9988776655', '42307810800000089012', '24:05:0006677:01', '24-24/007-24/007/007/2021-6789', '1022400990011',
        'АО "Рассвет"', 'Дата рождения: 30.04.1988'),
       (10, 'Лебедева Ирина Дмитриевна', '1991-08-14', '54 12 890123', NULL, '315540100000012', '01234567',
        'ТТТ 4455667788', NULL, '54:06:0008899:23', NULL, '1025400223344', 'ООО "Сирень"', 'Дата рождения: 14.08.1991'),
       (11, 'Козлов Владимир Андреевич', '1975-02-09', '66 04 345670', 'ЛМ 1122334', '316660700000078', '12340987', NULL,
        '42307810900000012340', '66:07:0001100:45', '66-66/008-66/008/008/2022-0123', '1026600556677', 'ИП Козлов В.А.',
        'Дата рождения: 09.02.1975'),
       (12, 'Соколова Наталья Игоревна', '2001-10-25', '02 16 678901', NULL, '310020400000034', '43210987',
        'УУУ 7766554433', '42307811000000043210', '02:08:0004455:67', NULL, '1020200889900', 'ООО "Одуванчик"',
        'Дата рождения: 25.10.2001'),
       (13, 'Михайлов Роман Юрьевич', '1969-12-31', '34 09 012345', 'НП 8765432', '313340600000056', '54321098', NULL, NULL,
        '34:09:0007788:89', '34-34/009-34/009/009/2023-4567', '1023400112244', 'ПАО "Заря"',
        'Дата рождения: 31.12.1969'),
       (14, 'Андреева Светлана Павловна', '1993-03-08', '61 13 567890', NULL, '316610200000089', '76543210',
        'ФФФ 3322110099', '42307811100000076543', '61:10:0000011:12', NULL, '1026100334455', 'ООО "Фиалка"',
        'Дата рождения: 08.03.1993'),
       (15, 'Николаев Григорий Константинович', '1980-07-19', '42 05 234561', 'РС 4455667', '314420800000023', '87654321',
        'ХХХ 1100998877', NULL, '42:11:0003322:34', '42-42/010-42/010/010/2020-8901', '1024200667788', 'АО "Лотос"',
        'Дата рождения: 19.07.1980');


-- ============================================================
-- 8. citizens_us (~15 rows, US documents)
--    Generated data for: SSN, EIN, ITIN, RTN, DriverLicenseUS,
--    PassportUS, PhoneUS, FullNameUS, AddressUS, MedicareUS,
--    VisaNumberUS, AlienRegistrationNumber, USCIS, SEVISID, NPI
-- ============================================================
CREATE TABLE citizens_us
(
    id               UInt64,
    full_name        Nullable(String),
    ssn              Nullable(String),
    ein              Nullable(String),
    itin             Nullable(String),
    rtn              Nullable(String),
    driver_license   Nullable(String),
    passport         Nullable(String),
    phone            Nullable(String),
    address          Nullable(String),
    medicare         Nullable(String),
    visa_number      Nullable(String),
    alien_reg_number Nullable(String),
    uscis            Nullable(String),
    sevis_id         Nullable(String),
    npi              Nullable(String)
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO citizens_us (id, full_name, ssn, ein, itin, rtn, driver_license, passport, phone, address, medicare,
                         visa_number, alien_reg_number, uscis, sevis_id, npi)
VALUES (1, 'John Michael Smith', '123-45-6789', '12-3456789', '912-34-5678', '021000021', 'D123-4567-8901', 'C12345678',
        '+1 (555) 123-4567', '123 Main St, Springfield, IL 62704', '1EG4-TE5-MK72', 'A12345678', 'A012345678',
        'MSC1234567890', 'N0012345678', '1234567893'),
       (2, 'Emily Rose Johnson', '987-65-4321', '98-7654321', '913-87-6543', '021200025', 'S987-6543-2100', 'C98765432',
        '+1 (555) 987-6543', '456 Oak Ave, Portland, OR 97201', '2FH5-UF6-NL83', 'B98765432', 'A987654321',
        'MSC9876543210', 'N0098765432', '1234567901'),
       (3, 'Robert James Williams', '456-78-9012', '45-6789012', '914-56-7890', '021300077', 'W456-7890-1234', 'C45678901',
        '+1 (555) 456-7890', '789 Pine Rd, Austin, TX 73301', '3GI6-VG7-OM94', 'C45678901', 'A456789012',
        'MSC4567890123', 'N0045678901', '1234567919'),
       (4, 'Sarah Ann Davis', '234-56-7890', '23-4567890', '915-23-4567', '021407912', 'D234-5678-9012', 'C23456789',
        '+1 (555) 234-5678', '321 Elm St, Seattle, WA 98101', '4HJ7-WH8-PN05', 'D23456789', 'A234567890',
        'MSC2345678901', 'N0023456789', '1234567927'),
       (5, 'Michael Thomas Brown', '345-67-8901', '34-5678901', '916-34-5678', '021502011', 'B345-6789-0123', 'C34567890',
        '+1 (555) 345-6789', '654 Maple Dr, Denver, CO 80201', '5IK8-XI9-QO16', 'E34567890', 'A345678901',
        'MSC3456789012', 'N0034567890', '1234567935'),
       (6, 'Jessica Marie Wilson', '567-89-0123', '56-7890123', '917-56-7890', '021606030', 'W567-8901-2345', 'C56789012',
        '+1 (555) 567-8901', '987 Cedar Ln, Miami, FL 33101', '6JL9-YJ0-RP27', 'F56789012', 'A567890123',
        'MSC5678901234', 'N0056789012', '1234567943'),
       (7, 'David Alexander Miller', '678-90-1234', '67-8901234', '918-67-8901', '021707052', 'M678-9012-3456',
        'C67890123', '+1 (555) 678-9012', '147 Birch Ave, Chicago, IL 60601', '7KM0-ZK1-SQ38', 'G67890123',
        'A678901234', 'MSC6789012345', 'N0067890123', '1234567950'),
       (8, 'Ashley Nicole Taylor', '789-01-2345', '78-9012345', '919-78-9012', '021808070', 'T789-0123-4567', 'C78901234',
        '+1 (555) 789-0123', '258 Spruce St, Boston, MA 02101', '8LN1-AL2-TR49', 'H78901234', 'A789012345',
        'MSC7890123456', 'N0078901234', '1234567968'),
       (9, 'Christopher Lee Anderson', '890-12-3456', '89-0123456', '920-89-0123', '021909090', 'A890-1234-5678',
        'C89012345', '+1 (555) 890-1234', '369 Walnut Blvd, Phoenix, AZ 85001', '9MO2-BM3-US50', 'I89012345',
        'A890123456', 'MSC8901234567', 'N0089012345', '1234567976'),
       (10, 'Amanda Joy Thomas', '901-23-4567', '90-1234567', '921-90-1234', '022000013', 'T901-2345-6789', 'C90123456',
        '+1 (555) 901-2345', '480 Chestnut Way, Atlanta, GA 30301', '0NP3-CN4-VT61', 'J90123456', 'A901234567',
        'MSC9012345678', 'N0090123456', '1234567984'),
       (11, 'Daniel Patrick Jackson', '012-34-5678', '01-2345678', '922-01-2345', '022100003', 'J012-3456-7890',
        'C01234567', '+1 (555) 012-3456', '591 Willow Ct, San Diego, CA 92101', '1OQ4-DO5-WU72', 'K01234567',
        'A012345670', 'MSC0123456789', 'N0001234567', '1234567992'),
       (12, 'Megan Claire White', '135-79-2468', '13-5792468', '923-13-5792', '022200041', 'W135-7924-6801', 'C13579246',
        '+1 (555) 135-7924', '702 Aspen Rd, Nashville, TN 37201', '2PR5-EP6-XV83', 'L13579246', 'A135792468',
        'MSC1357924680', 'N0013579246', '1234568008'),
       (13, 'William Edward Harris', '246-80-1357', '24-6801357', '924-24-6801', '022300056', 'H246-8013-5790', 'C24680135',
        '+1 (555) 246-8013', '813 Poplar Ave, Minneapolis, MN 55401', '3QS6-FQ7-YW94', 'M24680135', 'A246801357',
        'MSC2468013579', 'N0024680135', '1234568016'),
       (14, 'Lauren Elizabeth Martin', '357-91-2460', '35-7912460', '925-35-7912', '022400068', 'M357-9124-6080',
        'C35791246', '+1 (555) 357-9124', '924 Hickory Ln, Charlotte, NC 28201', '4RT7-GR8-ZX05', 'N35791246',
        'A357912460', 'MSC3579124600', 'N0035791246', '1234568024'),
       (15, 'James Richard Garcia', '468-02-3571', '46-8023571', '926-46-8023', '022500072', 'G468-0235-7190', 'C46802357',
        '+1 (555) 468-0235', '1035 Magnolia Blvd, San Antonio, TX 78201', '5SU8-HS9-AY16', 'O46802357', 'A468023571',
        'MSC4680235710', 'N0046802357', '1234568032');


-- ============================================================
-- 9. vehicles_crypto (~13 rows)
--    Generated data for: VIN, CryptoWallet, CryptoSeedPhrase,
--    HashData
-- ============================================================
CREATE TABLE vehicles_crypto
(
    id            UInt64,
    vin           Nullable(String),
    crypto_wallet Nullable(String),
    seed_phrase   Nullable(String),
    hash_value    Nullable(String),
    hash_type     Nullable(String)
)
ENGINE = MergeTree()
ORDER BY id;

INSERT INTO vehicles_crypto (id, vin, crypto_wallet, seed_phrase, hash_value, hash_type)
VALUES (1, 'WBA3A5C55CF256789', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
        'abandon ability able about above absent absorb abstract absurd abuse access accident',
        '5d41402abc4b2a76b9719d911017c592', 'MD5'),
       (2, '1HGBH41JXMN109186', '0x742d35Cc6634C0532925a3b844Bc9e7595f2bD18',
        'army birth comic decide effort fiction galaxy house invest joke kitchen legend',
        '2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824', 'SHA-256'),
       (3, 'WVWZZZ3CZWE123456', '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy',
        'morning narrow ocean pattern quality render solar timber unique velvet winter xenon',
        'e99a18c428cb38d5f260853678922e03', 'MD5'),
       (4, 'JN1TBNT30Z0123456', 'bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq',
        'angle bonus canal desert eagle flame guitar harbor island jungle kite lemon',
        'd7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592', 'SHA-256'),
       (5, 'SALGA2EF8FA123456', '0xdAC17F958D2ee523a2206206994597C13D831ec7',
        'mirror novel opera puzzle quest ribbon storm tunnel unity voyage wisdom youth',
        '098f6bcd4621d373cade4e832627b4f6', 'MD5'),
       (6, 'WDBRF61J31F123456', '1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2',
        'apple banana cherry dragon eclipse forest glacier horizon ivory jasmine kettle lunar',
        'b1d5781111d84f7b3fe45a0852e59758cd7a87e5', 'SHA-1'),
       (7, 'JTDKN3DU8A0123456', '0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48',
        'nebula orchid phoenix quartz rainbow stellar tornado umbrella vortex whisper xenon zenith',
        '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'SHA-256'),
       (8, '5UXWX7C54BL123456', 'LQB2fMHBrmo7ouN6nQnBxLHKvgYfeh9PEE',
        'alpha bravo charlie delta echo foxtrot golf hotel india juliet kilo lima', 'fcea920f7412b5da7be0cf42b8c93759',
        'MD5'),
       (9, 'WBAPH5C55BA123456', 'TN2YqTv12NRCitKMjhEg5rLoMdq8XoFMcj',
        'metro novice omega presto quilt radiance summit tundra utopia vivid wander xylophone',
        'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'SHA-256'),
       (10, '1G1YY22G965123456', '0x6B175474E89094C44Da98b954EedeAC495271d0F',
        'artist beacon crystal dolphin ember falcon glacier harbor ignite jester knapsack lantern',
        'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'SHA-256'),
       (11, '2T1KR32E94C123456', '1BoatSLRHtKNngkdXEeobR76b53LETtpyT',
        'bamboo carnival daydream eucalyptus firefly gondola hammock igloo jackpot kaleidoscope lighthouse moonbeam',
        '7c6a180b36896a65c4cb4167fae9d71544e0bbe328ffc4e0aa4e1a3e8bce85c0', 'SHA-256'),
       (12, 'WVWZZZ3CZWE789012', 'rPfV8ykdPpsFeSmxZoEi4dCEFmXiVvfy3C',
        'nectarine origami pagoda quasar rhapsody sapphire telescope universe valentine waterfall xanadu yearning',
        'ef797c8118f02dfb649607dd5d3f8c76', 'MD5'),
       (13, '3VWFE21C04M123456', '0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984', NULL,
        '15e2b0d3c33891ebb0f1ef609ec419420c20e320ce94c65fbc8c3312448eb225', 'SHA-256');
