# Чистка структуры и имён

Дата: 25 октября 2026 года.

## Границы изменений

Проект остаётся монолитом, но пакеты организованы по функциям (feature-first),
а не по глобальным слоям controller/service/repository/entity. Сохранено
разделение обязанностей классов; обновлены ссылки в существующих тестах. HTTP URL,
HTTP-статусы, JSON-поля, конфигурационные ключи, SQL-миграции и порядок
транзакционных блокировок не изменялись. Рабочее приложение для этой задачи
не запускалось; пользовательская БД не изменялась.

## Итоговая структура

```text
ru.nsu.marketplace
├── auth                  # вход, регистрация, JWT и Spring Security
│   └── dto
├── user                  # аккаунты, роли и админское управление пользователями
│   ├── dto
│   └── exception
├── product               # прикладной сервис и репозитории товаров
│   ├── api               # товар, каталог, кабинет продавца
│   ├── dto               # поля товара и операции с фотографиями
│   ├── model             # товар, карточка, изображение, статус продажи
│   ├── exception
│   └── moderation
│       └── dto
├── review                # отзывы и управление своим отзывом
│   ├── dto
│   ├── exception
│   └── moderation
│       └── dto
├── cart                  # корзина и её позиции
│   ├── dto
│   └── exception
├── order                 # заказы, позиции, статусы, управление админом
│   ├── dto
│   └── exception
├── media                 # хранение файлов, раздача и событие удаления
├── bootstrap             # начальный администратор и его настройки
└── common
    ├── phone             # нормализация, валидация и настройки номеров
    ├── web               # общий обработчик и формат ошибок HTTP
    └── moderation        # общие статус модерации и сведения о модераторе
```

### Почему именно так

Это функциональная структура одного приложения, не набор независимых сервисов
и не объявление строгих DDD bounded contexts. Существующие межмодульные вызовы
и JPA-связи сохранены; ограничение импортов через JPMS/ArchUnit не добавлялось.

В небольшой функции связанные классы живут рядом. Например, CartController,
CartService, CartRepository, CartEntity и CartItemEntity находятся в cart.
Суффиксы и Spring/JPA-аннотации обозначают техническую роль, а пакет — функцию.
Это связная группа корзины, а не общий пакет разных прикладных областей.

Большой product разбит дополнительно: у него несколько контроллеров, моделей,
DTO и отдельный сценарий модерации. У review выделена модерация, но не создан
отдельный api ради одного ProductReviewController. Подпакет вводится для
связной группы или самостоятельной границы, а не автоматически для каждого
репозитория, enum, исключения или технического слоя.

Исключения принадлежат своей функции: ошибки заказов — order.exception,
пользователей — user.exception и т. д. Единственное InvalidCredentialsException
остаётся в auth, а не в искусственном auth.exception из одного класса.
Ошибки телефонного номера находятся вместе с общим телефонным компонентом.

Модерация товара находится в product.moderation, модерация отзыва — в
review.moderation. В common.moderation только два действительно общих типа;
контроллеры и сервисы модерации разных ресурсов туда не переносятся.
ProductModerationSnapshot расположен рядом со своей заявкой; это @Embeddable,
не отдельная @Entity. Админский интерфейс не диктует архитектуру: контроллеры
управления пользователями и заказами принадлежат user и order соответственно.

Конфигурация располагается у своего компонента: JWT-настройки и SecurityConfig
в auth, настройка статических изображений в media, начальный администратор в
bootstrap. common не служит складом утилит: его группы уже имеют нескольких
реальных потребителей. Никаких фабричных интерфейсов, ports/adapters-пакетов
или новых зависимостей ради структуры не добавлено.

Тесты повторяют функциональную структуру: auth, user, product, product.api,
cart, order. ProductTestFixtures находится в product вместе с потребляющими
его тестами. Общая проверка коллекций нескольких моделей — common.EntityBuilderTest,
проверка загрузки приложения — bootstrap.MarketplaceApplicationTests.

Переносы выполнены точными патчами. Для всех 127 изменённых Java-файлов
проверены package, отсутствие старых копий и хеш тела файла без импортов:
вне package/import разрешены только точные замены полных имён переносимых
типов, включая enum в JPQL. Предыдущая сетка «слой × область» больше не является
актуальной структурой проекта.

## Основные переименования

| Было | Стало |
| --- | --- |
| `AdminController` | `AdminUserController` |
| `ModerationController` | `ProductModerationController` и `ProductReviewModerationController` |
| `ModerationService` | `ProductModerationService` |
| `ProductCardsRepository` | `ProductCardRepository` |
| `ImagesDetailsResponse` | `ProductDetailsImageResponse` |
| `UpdateImagesPositionsRequest` | `UpdateProductImageOrderRequest` |
| `RejectModerationRequest` | `RejectProductModerationRequest` |
| `ProductModerationSnapshotEntity` | `ProductModerationSnapshot` |
| `NumberAlreadyExistException` | `TelephoneAlreadyExistsException` |
| `EmailAlreadyExistException` | `EmailAlreadyExistsException` |
| `StaticMethods.getTestProductCardEntity` | `ProductTestFixtures.createProductCard` |
| `CartEntity.cartItemEntities` | `CartEntity.items` |
| `ProductCardEntity.imgUrl` | `ProductCardEntity.previewImageUrl`, с явным `@Column(name = "img_url")` |
| `addProductInCart` | `addProductToCart` |
| `updateQuantity` | `updateCartItemQuantity` |
| `getCatalog` / `getCatalogBySeller` | `getCatalogProductCards` / `getSellerProductCards` |
| `getImages` / `addImages` | `getProductDetailsImages` / `addProductDetailsImages` |
| `updateImagesPositions` / `updatePreview` | `updateProductImageOrder` / `updateProductPreview` |
| `toDto` | `toResponse` |
| `toImagesDetailsResponse` / `toMyResponse` | `toDetailsImageResponse` / `toMyReviewResponse` |
| `getModerationByStatus` / `getRequests` | `getRequestsByStatus` |
| `isEmail` | `looksLikeEmail`: метод выбирает ветку входа, а не валидирует email |
| `ensureReviewIsPending` | `ensureModerationRequestIsPending`: проверяется заявка |
| `handleCartNotFound` | `handleResourceNotFound`: обрабатывается несколько видов ресурсов |
| `currentUserId` в проверках уникальности UserService | `excludedUserId`: исключается редактируемый пользователь, а не текущий администратор |

Исправлены `jwtSercetKey`, `newImgeUrl`; зависимости сервисов и контроллеров
названы по их роли (`userRepository`, `reviewRepository`, `passwordEncoder`,
`orderService` и т. п.). Локальные имена товара, карточки, изображения,
идентификаторов и позиции сделаны предметными. Убраны проектные wildcard-импорты
и неиспользуемые импорты; выровнены группы импортов.

JSON-поля `imgUrl`, `uuid`, `imageUrlList` намеренно сохранены: их переименование
сломало бы текущий фронтенд и внешних клиентов. Смена Java-имени поля entity не
изменила колонку PostgreSQL.

## Проверки

- Основной Java-код компилируется.
- Повторно прошли 102 существующих теста в восьми классах: AuthServiceTest,
  UserServiceTest, OrderServiceTest, JwtServiceTest, EntityBuilderTest,
  CartControllerTest, OrderControllerTest, ProductCardRepositoryTest.
- Тест репозитория выполнялся на PostgreSQL через Testcontainers.
- Отдельной временной проверкой подтверждены 19 URL, ограничения ADMIN,
  поля пяти DTO и сохранение имени колонки `img_url`.
- Проверка восьми JS-модулей фронтенда и локальных импортов прошла.
- Новые постоянные тесты не добавлялись, временные инструменты проверки удалены.

Полный набор тестов **уже до изменений не компилировался** с семью ошибками:

1. В ProductControllerTest два конструктора ProductCardResponse не передают status.
2. В CartServiceTest один такой же устаревший конструктор.
3. В ProductControllerTest, ProductModificationSecurityTest и двух местах
   ProductServiceTest вызывается отсутствующий deleteProduct.

После чистки полный compileTestJava сообщает те же семь ошибок. Для запуска
доступных тестов четыре перечисленных исходных файла временно исключались
через отдельный Gradle init-скрипт; постоянный build.gradle.kts не менялся.
Старые сценарии физического удаления товара не заменялись архивированием
автоматически: это разные операции.

## Полный список следующих улучшений — НЕ реализованы этой чисткой

1. **Актуализировать устаревшие тесты.** Передать status в фикстурах и отдельно
   решить судьбу сценариев физического удаления товара. Это первый шаг, чтобы
   снова иметь полностью запускаемый набор проверок.

2. **Разделить ответственности ProductService.** Сейчас один большой класс
   управляет каталогом, полями товара, файлами, порядком фотографий и отправкой
   на модерацию. Выделить прежде всего операции с изображениями и сценарий
   подачи/отзыва заявки, сохранив транзакционные границы и порядок локов.
   Не создавать интерфейс или отдельный сервис для каждого метода.

3. **Вынести повторяющуюся загрузку и поиск в небольшие private-методы.**
   Например, findProductForUpdate, findCartForUpdate, findCartItem,
   findLatestReviewModerationForUpdate. Читается сценарий, а не одинаковые
   orElseThrow/stream-блоки. Не объединять блокирующую и обычную загрузку так,
   чтобы различие стало неявным.

4. **Отделить преобразование в response от прикладных сервисов.**
   Сейчас CartService обращается к публичному mapper-методу ProductService,
   а mapper деталей товара сам запрашивает карточку в БД. Сначала загрузить
   данные, затем передать их в mapper без скрытых запросов. Заодно проверить
   число запросов при преобразовании страницы карточек и корзины.

5. **Упростить GlobalExceptionHandler.** Повторяющееся создание ApiErrorResponse
   вынести в один private-фабричный метод. Специальные статусы, сообщение
   неизвестной ошибки и передачу AccessDeniedException в Spring Security
   сохранить явными.

6. **Разбить проверку порядка изображений на именованные шаги.** В
   updateProductImageOrder отделить проверку полного набора ID, дубликатов
   и принадлежности товару от применения нового порядка. Не подменять ясные
   проверки длинным стримом и не добавлять повторные проверки без причины.

7. **Убрать подтверждённый неиспользуемый код.** Неиспользуемый AdminCreateUserRequest рядом с рабочим
   CreateUserRequest, закомментированный deleteProduct, вызываемый только из
   комментария deleteAllProductDetailsImages. ImageNotFoundException сейчас
   нигде не бросается — есть только обработчик. Перед удалением согласовать,
   нужны ли эти заготовки для следующего шага разработки. Пустые domain.Product
   и domain.ProductCard уже были удалены пользователем перед дополнительной
   группировкой пакетов; эти удаления сохранены, классы не восстанавливались.

8. **Инкапсулировать синхронизацию двусторонних связей там, где она повторяется.**
   Небольшие addItem/removeItem и addDetailsImage могут одновременно менять
   коллекцию и ссылку на владельца. Это убирает необходимость в каждом сервисе
   помнить пару setParent + getChildren().add. Cascade и orphanRemoval сами
   согласованность Java-ссылок не поддерживают.

9. **Собрать повторяемые проверки DTO в переиспользуемые ограничения.**
   Имена пользователя, slug и цена повторяются в create/update. Применять
   составные аннотации, если они действительно устраняют дублирование,
   сохраняя разную обязательность полей POST и PATCH. Не вводить общий
   универсальный request с неясными validation groups без необходимости.

10. **Разбить большие HTML-шаблоны фронтенда на функции рендеринга.**
    В admin.js, admin-reviews.js, reviews.js и seller.js длинные шаблонные строки
    смешаны с обработчиками. Выделить разметку карточки, формы и результата;
    загрузку, состояние и обработчики оставить рядом. Не менять стек ради
    этой чистки и сохранить экранирование пользовательских данных.

11. **Согласовать нормализацию email.** Это уже функциональная правка, а не
    косметика: UserService удаляет пробелы и приводит email к lower-case,
    AuthService регистрирует и ищет исходную строку. Единый нормализатор нужен
    в регистрации, входе и админском создании/редактировании; заранее определить
    правила и обработку существующих совпадений.

12. **Закрепить правило эксклюзивности ADMIN на сервере.** Это тоже отдельная
    функциональная правка: UI не даёт смешивать ADMIN с BUYER/SELLER, но
    CreateUserRequest/UpdateUserRequest и UserService принимают набор ролей
    без проверки этой комбинации. Общее правило проверки ролей устранит
    расхождение концепции и API. После этого отдельно оценить избыточные
    !hasRole('ADMIN'), не убирать их наугад в рамках переименований.

Имена внешних JSON-полей и семантику nullable PATCH-полей можно унифицировать
только отдельным изменением контракта, а не молчаливым переименованием в этой
чистке. Функциональная группировка уже введена; дальнейшее выделение отдельных
сервисов, Maven/Gradle-модулей или жёстких архитектурных границ из неё автоматически
не следует и должно опираться на реальные требования.
