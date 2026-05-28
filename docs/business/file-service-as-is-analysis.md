# File Service As-Is Analysis

## 1. Назначение и границы сервиса

`file-service` - микросервис управления файлами для учебной платформы. Его текущая роль: создавать заявки на загрузку файлов, выдавать presigned URL для прямой загрузки в MinIO/S3, хранить метаданные файлов, подтверждать факт загрузки через `HeadObject`, выдавать ссылки на скачивание и удалять объекты по интеграционным событиям.

Ключевая архитектурная идея уже заложена правильно: сервис не принимает и не отдает байты файлов через backend. Клиент загружает и скачивает содержимое напрямую из MinIO по S3 URL. Backend работает только с метаданными, правами, ключами объектов и presigned URL.

Текущие внешние границы:

- Frontend вызывает публичный REST API `file-service` с JWT.
- `users-service` вызывает `GET /api/v1/files/{fileId}` для получения информации о файле, например при привязке аватара.
- `users-service` публикует Kafka-события `FileLoadedEvent` и `FileDeletedEvent` в `file-topic`.
- `file-service` валидирует JWT через JWKS endpoint `users-service`.
- `file-service` работает с PostgreSQL, MinIO и Kafka.

Сервис не является полноценным бизнес-сервисом курсов, заданий или пользователей. Однако в текущей реализации в нем уже есть часть доменной авторизации: проверка ролей и владельца при скачивании приватных файлов.

## 2. Технологический стек

Текущая реализация:

- Java 21.
- Spring Boot 3.3.5.
- Spring Web.
- Spring Security OAuth2 Resource Server.
- Spring Data JPA.
- PostgreSQL.
- Liquibase.
- AWS SDK for Java v2, модуль `software.amazon.awssdk:s3`.
- Spring Kafka.
- Micrometer Prometheus registry.
- Actuator.
- Lombok.
- Gradle Kotlin DSL.
- Docker multi-stage build.

В `build.gradle.kts` нет Spring Cloud и сервис-дискавери. Сервис запускается как отдельное Spring Boot приложение.

## 3. Структура проекта

Основной модуль находится в `file-service`.

```text
file-service/
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
├── docker-compose.yaml
├── docs/
│   └── business/
│       └── file-service-as-is-analysis.md
└── src/
    ├── main/
    │   ├── java/ru/sandr/fileservice/
    │   │   ├── FileServiceApplication.java
    │   │   ├── config/
    │   │   ├── consumer/
    │   │   ├── controller/
    │   │   ├── dao/
    │   │   ├── dto/
    │   │   ├── entity/
    │   │   ├── enums/
    │   │   ├── exception/
    │   │   ├── handler/
    │   │   ├── mapper/
    │   │   ├── service/
    │   │   └── storage/
    │   └── resources/
    │       ├── application.yml
    │       ├── db/changelog/
    │       └── keys/public.pem
    └── test/
```

Пакеты и ответственность:

- `config` - настройки S3, Security, CORS и properties.
- `controller` - публичный REST контроллер файлов.
- `service` - прикладная логика работы с файлами, построение S3 ключей, user context.
- `service/policy` - role-based upload policy.
- `storage` - обертка над AWS SDK S3/S3Presigner.
- `dao` - Spring Data JPA repository.
- `entity` - JPA модель метаданных файла и статус.
- `dto` - REST DTO и upload context DTO.
- `mapper` - маппинг request -> entity.
- `consumer` - Kafka listener и события.
- `consumer/config` - Kafka consumer/error handler/deserialization config.
- `exception` и `handler` - доменные исключения и глобальная обработка ошибок.

Архитектурно код следует layered подходу: Controller -> Service -> Repository/Storage. Репозиторий напрямую в контроллер не внедряется.

## 4. Runtime-конфигурация

`application.yml` задает:

- `spring.application.name`: `file-service`.
- HTTP port: `8087`.
- PostgreSQL URL по умолчанию: `jdbc:postgresql://localhost:5432/users`.
- Liquibase changelog: `classpath:db/changelog/files-changelog-master.yml`.
- JPA `ddl-auto: validate`.
- JWT JWKS URI по умолчанию: `http://localhost:8080/.well-known/jwks.json`.
- Kafka bootstrap по умолчанию: `localhost:9092`.
- Kafka topic: `file-topic`.
- Kafka group id: `files-consumer-group`.
- S3 internal endpoint: `http://minio:9000`.
- S3 presigned/external endpoint: `http://localhost:9000`.
- S3 buckets по умолчанию: `university-public`, `university-private`.
- CORS origins: `http://localhost:5173`, `http://127.0.0.1:5173`.
- Actuator exposure: `health`, `info`, `prometheus`.

В корневом `docker-compose.yml` значения для `file-app` переопределяются:

- DB идет в контейнер `users-db`.
- Kafka bootstrap: `kafka:29092`.
- JWKS URI: `http://users-app:8080/.well-known/jwks.json`.
- S3 endpoint: `http://minio:9000`.
- S3 presigned endpoint: `http://localhost:9000`.
- Buckets: `public-content`, `private-content`.

Важная особенность: локальный `file-service/docker-compose.yaml` создает бакеты `university-public` и `university-private`, а общий compose создает `public-content` и `private-content`. Это не баг само по себе, но два режима окружения имеют разные bucket names.

## 5. Функциональные возможности as-is

### 5.1 Создание заявки на загрузку

Endpoint:

```http
POST /api/v1/files/upload-request
Authorization: Bearer <JWT>
```

Request DTO:

```json
{
  "originalFilename": "avatar.png",
  "domain": "USER_AVATAR",
  "contentType": "image/png",
  "contentLength": 12345,
  "context": {
    "domain": "USER_AVATAR",
    "userId": "..."
  }
}
```

Текущий flow:

1. Контроллер берет `userId` из `Authentication.getName()`.
2. Контроллер передает request, `userId` и authorities в `FileService`.
3. Сервис берет `domain` из запроса.
4. Вызывается `domain.validateContentType(contentType)`.
5. Создается `UserContext`.
6. Через `UploadPolicyFactory` выбирается upload policy по роли.
7. Policy проверяет, может ли пользователь загружать файл выбранного домена.
8. Генерируется новый `fileId`.
9. `S3KeyBuilder` строит S3 key.
10. `S3KeyBuilder` выбирает bucket: public или private.
11. В БД сохраняется `FileMetadata` со статусом `PENDING`.
12. Через `S3Presigner` создается presigned PUT URL на 15 минут.
13. Клиент получает `fileId` и `uploadUrl`.

Response:

```json
{
  "fileId": "...",
  "uploadUrl": "http://localhost:9000/..."
}
```

Важный дефект: результат `domain.validateContentType(...)` сейчас игнорируется. Метод возвращает `boolean`, но при `false` исключение не выбрасывается. Поэтому фактически content type whitelist не enforced.

Второй важный дефект: `contentLength` сохраняется, но ограничение `FileDomain.maxSizeBytes` в сервисном flow не проверяется. Сейчас лимиты объявлены в enum, но не enforced.

### 5.2 Получение ссылки на скачивание

Endpoint:

```http
GET /api/v1/files/download-url/{fileId}
Authorization: Bearer <JWT>
```

Текущий flow:

1. Контроллер строит `UserContext` из JWT subject и authorities.
2. `FileService` ищет файл по `fileId`.
3. Если файла нет, выбрасывается `ObjectNotFoundException`.
4. Если статус не `ACTIVE`, выбрасывается `BadRequestException(FILE_IS_NOT_ACTIVE)`.
5. Если файл лежит в public bucket, сервис возвращает прямой public URL.
6. Если файл лежит в private bucket, сервис проверяет доступ:
   - `ROLE_ADMIN` разрешен;
   - `ROLE_TEACHER` разрешен;
   - `ROLE_STUDENT` разрешен только если `ownerId == currentUserId`.
7. Для private bucket генерируется presigned GET URL на 2 часа.

Response:

```json
{
  "downloadUrl": "http://localhost:9000/..."
}
```

Особенность: endpoint публичный с точки зрения API namespace (`/api/v1/files/...`), но требует JWT. Целевой internal API `/api/v1/internal/files/{fileId}/download-url` сейчас отсутствует.

Архитектурный trade-off текущей реализации: сервис быстрее закрывает минимальный сценарий скачивания, но принимает на себя бизнес-авторизацию. Для domain-agnostic file-service это спорно, потому что знание о том, кто может смотреть материал курса, должно жить в course/homework сервисах.

### 5.3 Получение метаданных файла

Endpoint:

```http
GET /api/v1/files/{fileId}
Authorization: Bearer <JWT>
```

Response:

```json
{
  "fileId": "...",
  "fileName": "avatar.png",
  "mimeType": "image/png",
  "status": "PENDING"
}
```

Endpoint используется внешними сервисами, например `users-service`, для проверки файла перед привязкой аватара.

Особенность: ownership/role checks на этом endpoint отсутствуют. Любой аутентифицированный пользователь, знающий `fileId`, может получить базовые метаданные.

### 5.4 Commit загруженного файла

REST endpoint для commit сейчас не реализован. Commit выполняется через Kafka event `FileLoadedEvent`.

Consumer:

```text
topic: file-topic
event_type header: FileLoadedEvent
payload: {"fileId": "..."}
```

Текущий flow:

1. `FileEventListener` получает `FileLoadedEvent`.
2. Парсит `fileId` как UUID.
3. Вызывает `FileService.commitFile(fileId)`.
4. Сервис ищет запись в БД.
5. Если статус `PENDING`, выполняет `s3Client.headObject(bucket, key)`.
6. Если объект существует, статус меняется на `ACTIVE`.
7. Если объекта нет, выбрасывается `ObjectNotFoundException`.

Положительная сторона: это соответствует подходу `trust but verify` - статус становится `ACTIVE` только после проверки объекта в S3.

Ограничение: commit не идемпотентен на уровне ответа, потому что это Kafka consumer без явного ответа. Повторное событие для уже `ACTIVE` файла просто ничего не меняет, что практически идемпотентно.

### 5.5 Удаление файла

Удаление выполняется через Kafka event `FileDeletedEvent`.

Consumer:

```text
topic: file-topic
event_type header: FileDeletedEvent
payload: {"fileId": "..."}
```

Текущий flow:

1. `FileEventListener` получает `FileDeletedEvent`.
2. Парсит `fileId`.
3. Вызывает `FileService.deleteFile(fileId)`.
4. Сервис ищет запись в БД.
5. Удаляет запись из БД.
6. Проверяет существование объекта через `HeadObject`.
7. Если объект существует, вызывает `DeleteObject`.

Важная особенность: запись удаляется из БД до удаления объекта из S3. Если `deleteObject` упадет после удаления записи, метаданные будут потеряны, а объект может остаться в bucket как orphan. С точки зрения надежности лучше сначала удалить объект или использовать статус/ретраи/outbox для удаления.

### 5.6 Garbage collection PENDING файлов

Scheduled garbage collection сейчас не реализован.

В `FileRepository` уже есть метод:

```java
List<FileMetadata> findByStatusAndCreatedAtBefore(FileStatus status, Instant createdAt);
```

В Liquibase есть индекс `idx_file_metadata_status_created_at`, который подходит для поиска старых `PENDING` записей.

Но в коде нет:

- `@EnableScheduling`;
- scheduled job;
- настройки периода запуска;
- настройки TTL для `PENDING`;
- batch deletion;
- метрик по cleanup.

Фактически Step 6 из целевой архитектуры частично реализован только в части Kafka deletion listener, но не реализован в части hourly sweeper.

## 6. Upload domains и S3 key structure

В текущей модели вместо контрактного `context: USER_AVATAR | COURSE_VIDEO | ...` используется `FileDomain`.

Текущие значения `FileDomain`:

| Domain | Bucket | S3 key pattern | Max size в enum | Allowed content types в enum |
| --- | --- | --- | --- | --- |
| `USER_AVATAR` | public | `users/{userId}/avatars/{fileId}.ext` | 2 MB | `image/jpeg`, `image/png` |
| `COURSE_AVATAR` | public | `courses/{courseId}/avatars/{fileId}.ext` | 2 MB | `image/jpeg`, `image/png` |
| `COURSE_MATERIAL` | private | `courses/{courseId}/materials/{fileId}.ext` | 200 MB | `image/jpeg`, `image/png` |
| `ANSWER_FILE` | private | `courses/{courseId}/answers/{userId}/{fileId}.ext` | 5 MB | `image/jpeg`, `image/png` |

Фактические S3 key patterns задаются в `S3KeyBuilder`.

Отличия от целевой архитектуры:

- Целевой avatar prefix: `avatars/{user_id}/{uuid}.ext`; текущий: `users/{userId}/avatars/{fileId}.ext`.
- Целевой course cover prefix: `course-covers/{course_id}/{uuid}.ext`; текущий: `courses/{courseId}/avatars/{fileId}.ext`.
- Целевой video prefix: `courses/{course_id}/lessons/{lesson_id}/{uuid}.ext`; текущего `COURSE_VIDEO` нет.
- Целевой homework prefix: `homeworks/{assignment_id}/students/{student_id}/{uuid}.ext`; текущий ответ: `courses/{courseId}/answers/{userId}/{fileId}.ext`.
- В `dto/UploadContext.java` есть старый enum `USER_AVATAR`, `COURSE_COVER`, `COURSE_VIDEO`, `HOMEWORK`, но он не участвует в текущем flow.

В текущем коде `context` является polymorphic DTO на Jackson sealed interface `FileContext`. При этом и верхний request, и вложенный context содержат domain. Это усложняет контракт: клиент должен корректно передать `domain` дважды.

## 7. Модель данных

JPA entity: `FileMetadata`.

Таблица: `files.file_metadata`.

Поля:

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | UUID | Идентификатор файла/заявки |
| `owner_id` | UUID | ID пользователя из JWT |
| `file_name` | VARCHAR(255) | Исходное имя файла |
| `bucket_name` | VARCHAR(100) | Bucket, куда должен быть загружен объект |
| `s3_key` | VARCHAR(1024) | Полный key объекта в bucket |
| `size_bytes` | BIGINT | Размер из upload request |
| `content_type` | VARCHAR(255) | MIME type из upload request |
| `status` | VARCHAR | `PENDING` или `ACTIVE` |
| `created_at` | TIMESTAMP WITH TIME ZONE | Время создания |
| `updated_at` | TIMESTAMP WITH TIME ZONE | Время обновления |

Индексы/constraints:

- Primary key по `id`.
- Unique constraint `uk_file_metadata_s3_key` по `s3_key`.
- Index `idx_file_metadata_owner_id`.
- Index `idx_file_metadata_status_created_at`.

Статусы:

- `PENDING` - запись создана, presigned PUT URL выдан, объект еще не подтвержден.
- `ACTIVE` - объект подтвержден через S3 `HeadObject`.

Отличия от целевой схемы:

- Целевая таблица называлась `files`, текущая - `files.file_metadata`.
- Целевое поле `original_filename`, текущее - `file_name`.
- Сервис использует общую БД `users`, но отдельную PostgreSQL schema `files`.
- Entity называется `FileMetadata`, не `FileEntity`.

С точки зрения production readiness отдельная schema лучше, чем смешивание таблиц в одной schema, но полноценная изоляция микросервиса обычно предполагает отдельную database/owner credentials, а не общую DB users service.

## 8. Работа с S3/MinIO

### 8.1 Клиенты AWS SDK

`S3Configuration` создает два bean:

- `S3Client` для backend operations: `HeadObject`, `DeleteObject`.
- `S3Presigner` для генерации presigned URL.

Оба клиента:

- используют static credentials из `s3.*`;
- используют region из config;
- используют path-style access, что важно для MinIO;
- используют endpoint override.

Разделение endpoint:

- `s3.endpoint` - внутренний endpoint, доступный контейнеру backend, например `http://minio:9000`.
- `s3.presigned-endpoint` - endpoint, который попадает в URL для браузера, например `http://localhost:9000`.

Это важная и правильная особенность. В Docker browser не сможет открыть `http://minio:9000`, потому что `minio` - имя контейнера во внутренней сети. Поэтому presigned URL должен содержать внешний адрес, доступный frontend/browser.

### 8.2 Presigned PUT

Метод `generatePresignedPutUrl` строит `PutObjectRequest` с:

- bucket;
- key;
- content type.

Затем создает `PutObjectPresignRequest` с TTL 15 минут.

Последствия:

- Backend не проксирует payload.
- Клиент обязан загрузить файл напрямую в MinIO.
- Content-Type входит в подпись request. Клиент при PUT должен отправить совместимый `Content-Type`, иначе S3 signature validation может не пройти.
- Размер файла не ограничивается самим presigned URL. Проверка размера должна быть сделана до выдачи URL и/или через bucket policy/proxy limits, но сейчас она не enforced в сервисе.

### 8.3 Presigned GET

Метод `generatePresignedGetUrl`:

- берет `FileMetadata`;
- кодирует исходное имя файла через UTF-8;
- задает `responseContentType`;
- задает `responseContentDisposition`;
- для safe MIME types использует `inline`;
- для остальных использует `attachment`;
- создает URL с TTL 2 часа.

Safe inline MIME types:

- `application/pdf`;
- `image/jpeg`;
- `image/png`;
- `image/webp`;
- `image/gif`;
- `video/mp4`;
- `video/webm`;
- `audio/mpeg`;
- `audio/ogg`.

Это разумная защита от нежелательного inline открытия потенциально опасных типов. При этом whitelist upload content types сейчас уже, чем inline whitelist: upload enum разрешает только jpeg/png.

### 8.4 Public bucket URL

Для public bucket сервис не генерирует presigned GET. Он возвращает:

```text
{externalEndpoint}/{bucketName}/{s3Key}
```

Это соответствует идее public read bucket. В общем compose `minio-init` выполняет:

```text
mc anonymous set download local/public-content
```

В локальном compose аналогично настраивается public read для `university-public`.

### 8.5 HeadObject

`objectExists` выполняет `HeadObject`.

Поведение:

- success -> `true`;
- S3Exception 404 -> `false`;
- другие S3Exception пробрасываются выше.

Это используется для commit и delete.

### 8.6 DeleteObject

`deleteObject` вызывает S3 `DeleteObject`.

Текущий delete flow перед удалением вызывает `objectExists`. Для S3 это дополнительный round-trip. С учетом того, что S3 delete обычно идемпотентен, можно удалять без предварительного HeadObject, но HeadObject позволяет отличать missing object от успешного удаления, если это важно для доменной диагностики.

## 9. Security as-is

Сервис настроен как stateless OAuth2 Resource Server.

Security правила:

- CSRF disabled.
- CORS включен.
- Session policy: `STATELESS`.
- `/actuator/health`, `/actuator/prometheus`, `/actuator/info` доступны без аутентификации.
- `/api/v1/**` требует JWT.
- `/error` разрешен.
- Все остальное deny all.

JWT:

- Валидация идет через JWKS URI.
- Principal name берется из JWT subject.
- Роли берутся из claim `roles`.
- Prefix не добавляется: ожидаются роли вида `ROLE_ADMIN`, `ROLE_TEACHER`, `ROLE_STUDENT`.

Файл `src/main/resources/keys/public.pem` присутствует, но текущая конфигурация его не использует. Это отличие от варианта с локальной RS256 public key validation.

Ролевая модель:

- `ROLE_STUDENT`.
- `ROLE_TEACHER`.
- `ROLE_ADMIN`.

Upload policy:

- Student не может загружать `COURSE_AVATAR` и `COURSE_MATERIAL`.
- Student может загружать `USER_AVATAR` и `ANSWER_FILE`.
- Teacher может загружать все текущие domains.
- Admin может загружать все текущие domains.

Проблема: `UploadPolicyFactory` выбирает первую policy, которая поддерживает пользователя. Если у пользователя несколько ролей, порядок Spring list может стать значимым. Например, пользователь с `ROLE_STUDENT` и `ROLE_TEACHER` может попасть в student policy и получить отказ на teacher action. Для production лучше явно задать приоритеты или рассчитывать effective permissions иначе.

## 10. Kafka integration

Kafka consumer слушает topic из `file-consumer.file-event-topic-name`, по умолчанию `file-topic`.

Поддерживаемые events:

- `FileLoadedEvent`.
- `FileDeletedEvent`.

События определяются через Kafka header `event_type`:

- `FileLoadedEvent` -> `ru.sandr.fileservice.consumer.events.FileLoadedEvent`.
- `FileDeletedEvent` -> `ru.sandr.fileservice.consumer.events.FileDeletedEvent`.

Consumer config:

- `JsonDeserializer`.
- `ErrorHandlingDeserializer`.
- `DefaultJackson2JavaTypeMapper`.
- trusted packages: `*`.
- retry через `DefaultErrorHandler`.
- fixed backoff.
- Dead letter publishing через `DeadLetterPublishingRecoverer`.
- `DeserializationException` помечен как non-retryable.

Настройки:

- `delay-between-retries`: 1000 ms.
- `max-attempts`: 5.
- `group-id`: `files-consumer-group`.
- `auto-offset-reset`: `earliest`.
- Kafka admin auto-create disabled.

Важный deployment gap: в корневом `docker-compose.yml` `kafka-init` создает `my-custom-topic` и `user-topic`, но не создает `file-topic`. При `auto-create: false` это может привести к тому, что `file-service` не сможет нормально подписаться на нужный topic в чистом окружении.

В локальном `file-service/docker-compose.yaml` также создается `my-custom-topic`, но не `file-topic`.

## 11. Error handling

В сервисе есть hierarchy domain exceptions:

- `CustomException`.
- `UnauthorizedException`.
- `AccessDeniedException`.
- `BadRequestException`.
- `ObjectNotFoundException`.
- `ConflictException`.
- `MissedRequiredArgument`.

`GlobalExceptionHandler` возвращает `ApiErrorResponse`.

HTTP mappings:

- `UnauthorizedException` -> 401.
- `BadCredentialsException` -> 401.
- `AccessDeniedException` -> 403.
- `ObjectNotFoundException` -> 404.
- `ConflictException` -> 409.
- `MethodArgumentNotValidException` -> 400 with violations.
- `HttpMessageNotReadableException` -> 400.
- `MethodArgumentTypeMismatchException` -> 400.
- `MissedRequiredArgument` -> 400.
- `BadRequestException` -> 400.

Security filter exceptions обрабатываются через `FilterChainExceptionHandler`.

Ограничение: S3/Kafka/DB infrastructure exceptions не нормализуются в доменные ошибки. Для внутренних операций это допустимо, но публичные endpoints могут вернуть generic 500 без контролируемого error code.

## 12. Observability and operations

Реализовано:

- Actuator endpoints `health`, `info`, `prometheus`.
- Prometheus metrics registry.
- HTTP server request histograms and SLO buckets.
- Application tag для metrics.
- Docker image на non-root user.
- Multi-stage Docker build.

В корневом compose присутствуют:

- Prometheus.
- Grafana.
- Loki.
- Node exporter.
- Postgres exporter.
- Kafka exporter.
- Kafka JMX exporter.
- Blackbox exporter.

Ограничения:

- В `application.yml` включены DEBUG/TRACE логи для security/web/catalina, что нежелательно для production по объему логов и риску утечки деталей.
- Нет custom business metrics: количество upload requests, commit success/failure, stale pending cleanup, S3 operation latency, Kafka DLQ count.
- Нет readiness/liveness групп actuator health.
- Нет structured logging/correlation id.
- Нет tracing/OpenTelemetry.

## 13. Функциональные требования as-is

Из текущей реализации можно вывести следующие фактические функциональные требования:

1. Сервис должен принимать authenticated upload request и создавать запись о файле в статусе `PENDING`.
2. Сервис должен генерировать presigned PUT URL сроком на 15 минут.
3. Сервис должен строить S3 key на основе domain-specific context.
4. Сервис должен разделять файлы по public/private bucket.
5. Сервис должен сохранять метаданные файла: owner, filename, bucket, key, size, content type, status, timestamps.
6. Сервис должен позволять загрузку только пользователям с подходящей ролью.
7. Сервис должен подтверждать загрузку файла через Kafka `FileLoadedEvent`.
8. При commit сервис должен проверять наличие объекта в S3 через `HeadObject`.
9. После успешного `HeadObject` сервис должен переводить файл из `PENDING` в `ACTIVE`.
10. Сервис должен выдавать URL на скачивание только для `ACTIVE` файлов.
11. Для public bucket сервис должен возвращать прямой static URL.
12. Для private bucket сервис должен генерировать presigned GET URL сроком на 2 часа.
13. Для private download сервис должен проверять роль и владельца файла.
14. Сервис должен удалять файл по Kafka `FileDeletedEvent`.
15. При удалении сервис должен удалить запись в БД и объект в S3, если объект существует.
16. Сервис должен отдавать базовые метаданные файла по `fileId`.
17. Сервис должен валидировать JWT через JWKS.
18. Сервис должен публиковать метрики в Prometheus format.

## 14. Нефункциональные требования as-is

Фактически реализованные или подразумеваемые NFR:

### Security

- Stateless JWT authentication.
- RS256 validation через JWKS.
- No backend payload proxying.
- Public/private bucket separation.
- Role-based upload authorization.
- Private download authorization.
- Non-root Docker runtime user.

### Performance

- Файловые payload не проходят через JVM, поэтому нагрузка на heap/network backend ограничена метаданными и S3 control-plane calls.
- Presigned URL снижает latency и backend bandwidth cost.
- JPA open-in-view disabled.
- DB indexes есть для owner lookup и stale pending lookup.

### Scalability

- Service stateless относительно HTTP session.
- Горизонтальное масштабирование возможно при общей PostgreSQL/Kafka/MinIO.
- Kafka consumer group позволяет масштабировать обработку событий при достаточном числе partitions.

### Consistency

- Upload request сначала создает `PENDING` запись.
- Commit через `HeadObject` переводит запись в `ACTIVE`.
- Подтверждение eventually consistent относительно момента фактической загрузки объекта.
- Delete сейчас не полностью атомарен между DB и S3.

### Availability and resilience

- Kafka retry + DLQ для consumer errors.
- S3 404 при HeadObject переводится в `false`.
- Нет circuit breaker/timeouts/retry policy на S3 SDK уровне в явной конфигурации.
- Нет graceful degradation для MinIO unavailability.

### Operability

- Actuator health/prometheus.
- Docker compose для локальной инфраструктуры.
- Liquibase migrations.
- Но нет полноценного alerting набора именно для file-service.

## 15. Соответствие целевой архитектуре

### Что соответствует

- Использование presigned PUT/GET URL.
- Backend не обрабатывает file bytes.
- JWT resource server.
- PostgreSQL + JPA + Liquibase.
- S3/MinIO через AWS SDK v2.
- Отдельные public/private buckets.
- `PENDING` -> `ACTIVE` lifecycle.
- `HeadObject` перед активацией.
- Kafka listener для file deletion.
- Role-based upload policy.
- Public bucket отдается как static URL.
- Private bucket отдается через presigned GET.

### Основные расхождения

| Целевая архитектура | As-is |
| --- | --- |
| Buckets строго `public-content`, `private-content` | В общем compose да, в app defaults/local compose `university-public`, `university-private` |
| Table `files` | Table `files.file_metadata` |
| `original_filename` | `file_name` |
| Contexts `USER_AVATAR`, `COURSE_VIDEO`, `COURSE_COVER`, `HOMEWORK` | `FileDomain`: `USER_AVATAR`, `COURSE_AVATAR`, `COURSE_MATERIAL`, `ANSWER_FILE` |
| Avatar key `avatars/{user_id}/{uuid}.ext` | `users/{userId}/avatars/{fileId}.ext` |
| Course cover key `course-covers/{course_id}/{uuid}.ext` | `courses/{courseId}/avatars/{fileId}.ext` |
| Course videos by lesson id | Not implemented |
| Homework by assignment/student id | Implemented as course answer key, not target format |
| Global max 300 MB | Enum has per-domain limits, but not enforced |
| MIME validation required | Method exists, result ignored |
| Internal commit REST API | Not implemented |
| Internal private download REST API | Not implemented |
| Domain agnostic download authorization | File-service does owner/role checks |
| Scheduled GC every hour for stale PENDING > 24h | Not implemented |
| Kafka `file-topic` provisioned | Not provisioned in compose |
| Public key resource validation possible | `public.pem` exists but unused; JWKS is used |

## 16. Риски и проблемные места

### High severity

1. Content type validation is ineffective.

`FileDomain.validateContentType` returns `boolean`, but `FileService.createUploadRequest` ignores the result. Any content type can be accepted and persisted.

2. Size limits are not enforced.

`FileDomain.maxSizeBytes` exists, but `contentLength` is never compared against it. This breaks quota/limit requirements and can allow files larger than expected.

3. `file-topic` is not created by compose.

Kafka auto-create is disabled, while init scripts do not create `file-topic`. Clean environment startup can fail or consumer can be non-functional.

4. Delete order can create orphan S3 objects.

`deleteFile` deletes DB metadata before S3 object deletion. If S3 deletion fails, retrying by `fileId` becomes impossible because metadata is already gone.

### Medium severity

1. Missing scheduled cleanup for stale `PENDING` files.

Abandoned upload requests will accumulate in DB, and optionally uploaded-but-uncommitted objects may remain in S3.

2. Internal REST API contract is absent.

Commit/download are not exposed under `/api/v1/internal/files/...`, so other services must rely on Kafka for commit and public API for download behavior.

3. Authorization model is mixed into file-service.

Private download allows teacher/admin broadly and student by owner. This may not match course purchase/enrollment rules and violates domain-agnostic boundary.

4. Multi-role policy resolution can produce unexpected denials.

`UploadPolicyFactory` picks first matching policy from injected list. A user with multiple roles can be resolved to a stricter policy depending on bean order.

5. Metadata endpoint has no object-level authorization.

Any authenticated caller with a `fileId` can read filename, MIME type and status.

6. Domain/context duplication in request.

`domain` exists at top level and inside `context`. Mismatches are possible unless explicitly validated, which currently is not done.

### Low severity / hygiene

1. Unused `UploadContext` enum.
2. Unused `keys/public.pem`.
3. Local compose and root compose use different bucket naming.
4. DEBUG/TRACE logging is too verbose for production.
5. `CourseMaterialContext.courseId` is `String`, while other IDs are `UUID`.
6. `TaskAnswerContext` fields have no `@NotNull`.
7. `Dockerfile` exposes `8080`, while app port is `8087`; compose maps `8087`, so this is mostly metadata mismatch.
8. No meaningful automated tests beyond `contextLoads`.

## 17. Recommended target backlog

### P0

- Enforce content type validation with explicit `BadRequestException`.
- Enforce size limits from `FileDomain.maxSizeBytes` or global 300 MB rule.
- Add `file-topic` creation to both compose init scripts.
- Change delete flow to avoid orphan objects: delete S3 first, then DB, or introduce deletion status with retry.

### P1

- Implement scheduled GC for stale `PENDING` records older than 24 hours.
- Add internal APIs:
  - `POST /api/v1/internal/files/commit`;
  - `GET /api/v1/internal/files/{fileId}/download-url`.
- Decide whether download authorization belongs in file-service or business services. For strict domain-agnostic design, move business permission checks out and let file-service only generate URL after trusted internal request.
- Align S3 key structure with target contract.
- Align domain names with target contexts or document why current vocabulary is preferred.

### P2

- Add business metrics and alerts.
- Add integration tests with Testcontainers for PostgreSQL/MinIO/Kafka.
- Add OpenAPI documentation.
- Remove unused `UploadContext` and `public.pem`, or wire them intentionally.
- Normalize ID types in context DTOs.
- Add explicit S3 SDK timeout/retry configuration.

## 18. Итоговая оценка

`file-service` уже содержит ядро правильной архитектуры для file management microservice: presigned URL модель, zero payload proxying, S3 key generation, metadata persistence, public/private buckets, JWT validation, Kafka-based commit/delete и `HeadObject` verification.

Главные недоработки сейчас не в базовой структуре, а в enforcement и operational completeness: не применяются MIME/size ограничения, отсутствует scheduled garbage collection, не создан Kafka topic, нет internal REST API из целевого контракта, а часть бизнес-авторизации находится внутри file-service.

Сервис можно считать рабочим MVP для avatar/material upload flow, но пока не production-complete для заявленной архитектуры File Management Microservice.
