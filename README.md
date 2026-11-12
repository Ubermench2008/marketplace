# Рядом

PET-проект маркетплейса на Java 21, Spring Boot и PostgreSQL.

## Бизнес-логика

- **Пользователи.** Покупатель работает с корзиной и заказами. Продавец управляет своими товарами и остатками. Администратор управляет пользователями, модерирует товары и отзывы.
- **Товары и каталог.** Продавец создаёт товар с категорией и начальным остатком, отправляет его на модерацию и после одобрения выставляет на продажу. В публичном каталоге видны только одобренные товары в продаже с положительным остатком. Доступны фильтры по категории и цене, сортировка и пагинация.
- **Остатки и заказы.** Корзина не резервирует товар. При оформлении заказа остатки проверяются и списываются под блокировкой, а цены фиксируются в позициях заказа. Списание, создание заказа и очистка корзины выполняются в одной транзакции. При отмене до доставки товар возвращается на склад и в корзину покупателя. Повторная отмена запрещена.
- **Отзывы.** Отзыв публикуется после одобрения. Под отзывами доступны ветки ответов и реакции. Модератор может скрыть ответ, сохранив видимую часть его ветки.

## Слои приложения

- Контроллеры принимают REST-запросы и проверяют входные данные.
- Сервисы отвечают за бизнес-правила и границы транзакций.
- Репозитории работают с PostgreSQL через Spring Data JPA. Схему обновляет Flyway.

## Локальный запуск

Нужны JDK 21 и запущенный Docker с Docker Compose. Команды выполняются из корня репозитория.

```sh
docker compose up -d postgres
```

PostgreSQL доступен на порту `15432`. При первом запуске Flyway создаёт схему и заполняет справочник категорий. В новой базе каталог пуст.

Для запуска нужны переменные окружения с секретом JWT и данными первого администратора. Команда `bootRun` выполняется в том же сеансе терминала, в котором заданы переменные. Секрет — 32 случайных байта в Base64; номер телефона — в международном формате (`+` и код страны). Секрет и пароль не должны попадать в репозиторий.

### Windows (PowerShell)

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
$jwtRandom.Dispose()
$env:MARKETPLACE_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:MARKETPLACE_ADMIN_NAME = 'admin'
$env:MARKETPLACE_ADMIN_TELEPHONE = Read-Host 'Admin phone number'
$env:MARKETPLACE_ADMIN_PASSWORD = Read-Host 'Local admin password'

.\gradlew.bat bootRun '--args=--data.storage.location=./data/media'
```

### Linux и macOS (Bash, с OpenSSL)

```bash
export MARKETPLACE_JWT_SECRET="$(openssl rand -base64 32)"
export MARKETPLACE_ADMIN_NAME=admin
read -r -p 'Admin phone number: ' MARKETPLACE_ADMIN_TELEPHONE
read -r -s -p 'Local admin password: ' MARKETPLACE_ADMIN_PASSWORD
printf '\n'
export MARKETPLACE_ADMIN_TELEPHONE MARKETPLACE_ADMIN_PASSWORD

bash ./gradlew bootRun --args='--data.storage.location=./data/media'
```

Параметр `data.storage.location` переопределяет локальный путь из конфига: загруженные изображения сохраняются в `data/media` относительно корня проекта. На чистой базе приложение создаёт администратора; вход выполняется по номеру телефона и паролю. Если номер уже есть в базе, пароль не обновляется. При повторном запуске в новом терминале переменные нужно задать снова; смена JWT-секрета делает ранее выданные токены недействительными.

## Приложение и API

- [Веб-интерфейс](http://localhost:8080/) — раздаётся бэкендом, отдельная сборка фронтенда не нужна.
- [Swagger UI](http://localhost:8080/swagger-ui/index.html) · [OpenAPI JSON](http://localhost:8080/v3/api-docs). `POST /api/auth/login` возвращает `accessToken`. Для защищённых запросов в Swagger UI токен указывается в **Authorize** без префикса `Bearer`.
- [Соглашения API](docs/api-conventions.md) · [Отдельный запуск фронтенда](frontend/README.md).

## Тесты

`bash ./gradlew test` (`.\gradlew.bat test` в PowerShell). Для интеграционных тестов нужен Docker: Testcontainers запускает отдельный PostgreSQL.
