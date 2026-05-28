# File Service As-Is Context (LLM Handoff)

## 1) What this service does now

`file-service` is a Spring Boot microservice that manages file metadata and S3/MinIO URL orchestration.

Current responsibilities:
- create upload requests and return presigned PUT URLs;
- persist file metadata with lifecycle status (`PENDING`, `ACTIVE`);
- confirm uploaded objects via S3 `HeadObject` (triggered by Kafka event);
- generate download URLs (public static URL or private presigned GET);
- process file deletion events from Kafka and remove metadata/object.

It does **not** proxy file bytes through backend API. File content goes directly between client and MinIO.

---

## 2) Runtime stack

- Java 21
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA + PostgreSQL
- Liquibase
- Spring Security OAuth2 Resource Server (JWT validation via JWKS URI)
- AWS SDK v2 (`software.amazon.awssdk:s3`)
- Spring Kafka
- Actuator + Prometheus metrics

Main module path: `file-service`

---

## 3) High-level package map

- `controller` - REST endpoints (`FileController`)
- `service` - business logic (`FileService`, `S3KeyBuilder`, `UserContext`)
- `service/policy` - role-based upload policy selection
- `storage` - S3/MinIO access and presigned URL generation (`S3ServiceImpl`)
- `dao` - JPA repository (`FileRepository`)
- `entity` - persistence model (`FileMetadata`, `FileStatus`)
- `consumer` - Kafka listener (`FileEventListener`)
- `consumer/config` - Kafka deserialization/retry/DLQ config
- `config` - security, S3 beans/properties, CORS
- `exception`, `handler` - custom exceptions and global error handling

---

## 4) External dependencies and infra

- PostgreSQL (metadata storage)
- MinIO/S3-compatible storage (object storage)
- Kafka (file lifecycle events)
- JWKS endpoint from auth service (`users-service`) for JWT signature validation

In root platform compose, the service is `file-app` on port `8087`.

---

## 5) Current config model

### HTTP and DB
- Port: `8087` (`server.port`)
- Datasource: PostgreSQL (`spring.datasource.*`)
- JPA: `ddl-auto=validate`
- Liquibase changelog: `db/changelog/files-changelog-master.yml`

### Security
- OAuth2 Resource Server JWT mode
- JWT validator source: `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
- Roles are extracted from JWT claim `roles`
- Authority prefix is empty (`""`), so role strings are expected as-is (`ROLE_ADMIN`, etc.)

### Kafka
- Bootstrap: `spring.kafka.bootstrap-servers`
- Consumer group: `files-consumer-group`
- Topic: `file-topic` (`file-consumer.file-event-topic-name`)
- Type header: `event_type`
- Retry: fixed backoff, DLQ recoverer

### S3/MinIO
- Internal endpoint: `s3.endpoint` (backend-to-MinIO)
- External/presigned endpoint: `s3.presigned-endpoint` (used in URLs returned to clients)
- Buckets configured by:
  - `s3.public-bucket`
  - `s3.private-bucket`

---

## 6) Persistence model

Entity: `FileMetadata`  
Table: `files.file_metadata`

Columns:
- `id` (UUID, PK)
- `owner_id` (UUID, indexed)
- `file_name` (original filename)
- `bucket_name`
- `s3_key` (unique)
- `size_bytes`
- `content_type`
- `status` (`PENDING` / `ACTIVE`)
- `created_at`
- `updated_at`

Indexes/constraints:
- unique on `s3_key`
- index on `owner_id`
- composite index on `(status, created_at)`

---

## 7) Upload domains and key patterns (as implemented)

Upload domain enum: `FileDomain`

Implemented domains:
- `USER_AVATAR`
- `COURSE_AVATAR`
- `COURSE_MATERIAL`
- `ANSWER_FILE`

S3 key builder patterns:
- `users/{userId}/avatars/{fileId}.{ext}`
- `courses/{courseId}/avatars/{fileId}.{ext}`
- `courses/{courseId}/materials/{fileId}.{ext}`
- `courses/{courseId}/answers/{userId}/{fileId}.{ext}`

Bucket routing:
- `USER_AVATAR`, `COURSE_AVATAR` -> public bucket
- `COURSE_MATERIAL`, `ANSWER_FILE` -> private bucket

---

## 8) REST API (as-is)

Base path: `/api/v1/files`

All `/api/v1/**` endpoints require authenticated JWT.

### 8.1 `POST /api/v1/files/upload-request`

Purpose:
- register a new file upload request;
- persist metadata in `PENDING`;
- return presigned PUT URL.

Request DTO (`UploadUrlRequest`):
- `originalFilename` (string, required)
- `domain` (`FileDomain`, required)
- `contentType` (string, required)
- `contentLength` (positive long, required)
- `context` (`FileContext`, polymorphic, required)

Response DTO (`UploadUrlResponse`):
- `fileId` (UUID)
- `uploadUrl` (string, presigned PUT)

### 8.2 `GET /api/v1/files/download-url/{fileId}`

Purpose:
- return a download URL for active file.

Behavior:
- file must exist and be `ACTIVE`;
- if file is in public bucket -> direct static URL;
- if file is in private bucket -> presigned GET URL (2h) after permission check.

Response DTO (`DownloadUrlResponse`):
- `downloadUrl` (string)

### 8.3 `GET /api/v1/files/{fileId}`

Purpose:
- return basic file metadata by ID.

Response DTO (`FileInfoResponse`):
- `fileId`
- `fileName`
- `mimeType`
- `status`

---

## 9) Authorization behavior in service logic

### Upload authorization
`UploadPolicyFactory` picks policy by user roles:
- `StudentUploadPolicy`
- `TeacherUploadPolicy`
- `AdminUploadPolicy`

Current policy behavior:
- student cannot upload `COURSE_AVATAR` and `COURSE_MATERIAL`;
- teacher/admin can upload all current domains.

### Download authorization for private bucket
`FileService.generateDownloadUrl(...)` allows:
- admin -> allowed
- teacher -> allowed
- student -> only if `file.ownerId == currentUserId`

---

## 10) Kafka event handling (as-is)

Listener: `FileEventListener`  
Topic: `file-topic`

Supported event payloads:
- `FileLoadedEvent { fileId: String }`
- `FileDeletedEvent { fileId: String }`

Routing uses header `event_type`.

Processing:
- `FileLoadedEvent` -> `commitFile(fileId)`
- `FileDeletedEvent` -> `deleteFile(fileId)`

Consumer resilience:
- `DefaultErrorHandler` with fixed backoff retries
- `DeadLetterPublishingRecoverer` for failed records
- deserialization errors marked non-retryable

---

## 11) S3/MinIO integration details

`S3Configuration` provides:
- `S3Client` for operational calls (`HeadObject`, `DeleteObject`)
- `S3Presigner` for URL generation

Both are configured with:
- static credentials (`s3.access-key`, `s3.secret-key`)
- region (`s3.region`)
- path-style addressing enabled (important for MinIO)

### Presigned PUT flow
- `PutObjectRequest` includes bucket, key, and content-type
- URL TTL: 15 minutes

### Presigned GET flow
- URL TTL: 2 hours
- service sets `responseContentType`
- service sets content disposition:
  - `inline` for safe MIME whitelist (pdf/images/video/audio)
  - `attachment` for other MIME types
- filename is UTF-8 encoded in content-disposition

### Object existence check
- uses `HeadObject`
- returns `false` on 404
- rethrows non-404 S3 errors

### Delete operation
- uses `DeleteObject`

---

## 12) Lifecycle status model

### `PENDING`
Created when upload request is issued and metadata saved.

### `ACTIVE`
Set during commit flow after Kafka `FileLoadedEvent` and successful S3 `HeadObject`.

---

## 13) End-to-end flows

### Flow A: Upload request -> direct upload
1. Client calls `POST /api/v1/files/upload-request` with JWT and upload metadata.
2. Service validates auth + resolves role policy.
3. Service builds file ID, S3 key, bucket, saves metadata (`PENDING`).
4. Service returns presigned PUT URL.
5. Client uploads bytes directly to MinIO using returned URL.

### Flow B: Commit uploaded file
1. Upstream service publishes `FileLoadedEvent(fileId)` to Kafka (`file-topic`).
2. `file-service` consumes event.
3. Service loads metadata by `fileId`.
4. If status is `PENDING`, service checks object via `HeadObject`.
5. If object exists, status changes to `ACTIVE`.

### Flow C: Download URL generation
1. Client calls `GET /api/v1/files/download-url/{fileId}`.
2. Service verifies file exists and status is `ACTIVE`.
3. If bucket is public: return direct static URL.
4. If bucket is private: check access rules and return presigned GET URL.

### Flow D: Hard delete
1. Upstream service publishes `FileDeletedEvent(fileId)` to Kafka.
2. `file-service` consumes event.
3. Service loads metadata and deletes DB row.
4. Service checks object existence in S3 and deletes object if present.

---

## 14) Error model

Global handler: `GlobalExceptionHandler` returning `ApiErrorResponse`.

Typical mappings:
- `AccessDeniedException` -> 403
- `ObjectNotFoundException` -> 404
- `BadRequestException` -> 400
- validation/type/parsing errors -> 400
- unauthorized/auth errors -> 401

---

## 15) Observability and ops

- Actuator endpoints exposed: `health`, `info`, `prometheus`
- Prometheus metrics enabled with HTTP request histograms/SLO buckets
- Dockerized service (`Dockerfile`, compose integration)

---

## 16) Practical notes for other LLMs

- File bytes are never uploaded through this backend API.
- The source of truth for file lifecycle is DB metadata + Kafka events + S3 object existence.
- `PENDING -> ACTIVE` transition is event-driven, not synchronous with upload request.
- Download behavior differs by bucket type:
  - public bucket -> direct URL
  - private bucket -> presigned URL + access check
- Key construction is domain/context-driven in `S3KeyBuilder`.
- Main orchestration logic is in `FileService`.
