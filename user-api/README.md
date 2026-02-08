# User API

User CRUD REST API，基於 Spring Boot 3.2 建構，具備角色管理與審計日誌功能。
架構已為未來 OAuth2.0 / JWT / HTTP Basic 認證預留擴展空間。

---

## 技術棧

| 項目 | 版本 / 技術 |
|------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| API 文件 | SpringDoc OpenAPI 2.5 (Swagger UI) |
| 驗證 | Jakarta Bean Validation |
| 建構工具 | Maven |
| 容器化 | Docker (PostgreSQL 16 Alpine) |

---

## 快速開始

### 環境需求

- JDK 17+
- Maven 3.8+
- Docker（用於啟動 PostgreSQL）

### 1. 啟動 PostgreSQL（Docker）

**拉取映像檔：**

```bash
docker pull postgres:16-alpine
```

**方式 A — 使用 docker-compose（推薦）：**

專案已內附 `docker-compose.yml`，一鍵啟動：

```bash
docker-compose up -d
```

這會自動建立 `user_api` 資料庫、設定帳號密碼、掛載持久化 volume。

停止：

```bash
docker-compose down
```

停止並清除資料（volume 一併刪除）：

```bash
docker-compose down -v
```

**方式 B — 使用 docker run：**

```bash
docker run -d \
  --name user-api-postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=user_api \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -v user-api-pgdata:/var/lib/postgresql/data \
  postgres:16-alpine
```

停止與移除：

```bash
docker stop user-api-postgres
docker rm user-api-postgres
```

**驗證資料庫是否就緒：**

```bash
docker exec -it user-api-postgres psql -U postgres -d user_api -c "\conninfo"
```

### 2. 設定資料庫連線

預設配置已對應 Docker 容器設定，無需修改。若需調整，編輯 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_api
    username: postgres
    password: postgres
```

### 3. 編譯與啟動

```bash
# 編譯
mvn compile

# 啟動
mvn spring-boot:run
```

### 4. 存取 Swagger UI

啟動後開啟瀏覽器：

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON 端點：

```
http://localhost:8080/v3/api-docs
```

---

## 專案結構

```
user-api/
├── pom.xml
├── docker-compose.yml                   # PostgreSQL 容器編排
└── src/
    ├── main/
    │   ├── java/com/example/userapi/
    │   │   ├── UserApiApplication.java          # 應用程式進入點
    │   │   ├── config/
    │   │   │   ├── JpaAuditingConfig.java       # 啟用 @CreatedDate / @LastModifiedDate
    │   │   │   └── OpenApiConfig.java           # Swagger/OpenAPI 設定
    │   │   ├── controller/
    │   │   │   ├── UserController.java          # User REST 端點
    │   │   │   └── RoleController.java          # Role REST 端點
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── CreateUserRequest.java   # 建立使用者請求
    │   │   │   │   ├── UpdateUserRequest.java   # 更新使用者請求
    │   │   │   │   └── CreateRoleRequest.java   # 建立角色請求
    │   │   │   └── response/
    │   │   │       ├── UserResponse.java        # 使用者回應（不含密碼）
    │   │   │       ├── RoleResponse.java        # 角色回應
    │   │   │       ├── AuditLogResponse.java    # 審計日誌回應
    │   │   │       └── ApiErrorResponse.java    # 統一錯誤回應
    │   │   ├── entity/
    │   │   │   ├── BaseEntity.java              # 抽象父類（id, createdAt, updatedAt）
    │   │   │   ├── User.java                    # 使用者實體
    │   │   │   ├── Role.java                    # 角色實體
    │   │   │   └── AuditLog.java                # 審計日誌實體
    │   │   ├── enums/
    │   │   │   └── AuditEventType.java          # 審計事件類型列舉
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java  # 全域例外處理
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── DuplicateResourceException.java
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   ├── RoleRepository.java
    │   │   │   └── AuditLogRepository.java
    │   │   └── service/
    │   │       ├── UserService.java             # 使用者業務邏輯
    │   │       ├── RoleService.java             # 角色業務邏輯
    │   │       └── AuditLogService.java         # 審計日誌業務邏輯
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/example/userapi/
            └── UserApiApplicationTests.java
```

---

## API 端點

### User (`/api/v1/users`)

| 方法 | 路徑 | 說明 | 成功狀態碼 |
|------|------|------|-----------|
| POST | `/api/v1/users` | 建立使用者 | 201 Created |
| GET | `/api/v1/users` | 查詢所有使用者（分頁） | 200 OK |
| GET | `/api/v1/users/{id}` | 查詢單一使用者 | 200 OK |
| PUT | `/api/v1/users/{id}` | 更新使用者 | 200 OK |
| DELETE | `/api/v1/users/{id}` | 刪除使用者 | 204 No Content |
| PUT | `/api/v1/users/{id}/roles` | 指派角色 | 200 OK |
| GET | `/api/v1/users/{id}/audit-logs` | 查詢審計日誌（分頁） | 200 OK |

### Role (`/api/v1/roles`)

| 方法 | 路徑 | 說明 | 成功狀態碼 |
|------|------|------|-----------|
| POST | `/api/v1/roles` | 建立角色 | 201 Created |
| GET | `/api/v1/roles` | 查詢所有角色 | 200 OK |
| GET | `/api/v1/roles/{id}` | 查詢單一角色 | 200 OK |
| DELETE | `/api/v1/roles/{id}` | 刪除角色 | 204 No Content |

### 分頁參數

分頁端點支援以下查詢參數：

| 參數 | 預設值 | 說明 |
|------|--------|------|
| `page` | 0 | 頁碼（從 0 開始） |
| `size` | 20 | 每頁筆數 |
| `sort` | 依端點不同 | 排序欄位與方向，如 `sort=createdAt,desc` |

---

## 請求 / 回應範例

### 建立使用者

**Request:**

```http
POST /api/v1/users
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secureP@ss1",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+886912345678"
}
```

**Response (201):**

```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+886912345678",
  "enabled": false,
  "locked": false,
  "lastLoginAt": null,
  "lastLoginIp": null,
  "roles": [],
  "createdAt": "2026-02-08T10:30:00Z",
  "updatedAt": "2026-02-08T10:30:00Z"
}
```

### 更新使用者

**Request:**

```http
PUT /api/v1/users/1
Content-Type: application/json

{
  "email": "john.new@example.com",
  "firstName": "Johnny",
  "enabled": true
}
```

只傳入需要更新的欄位，`null` 或未傳入的欄位不會被更新。

### 指派角色

**Request:**

```http
PUT /api/v1/users/1/roles
Content-Type: application/json

[1, 2]
```

Request body 為角色 ID 的陣列，會**取代**該使用者現有的所有角色。

### 建立角色

**Request:**

```http
POST /api/v1/roles
Content-Type: application/json

{
  "name": "ROLE_ADMIN",
  "description": "System administrator"
}
```

### 查詢使用者（分頁）

```http
GET /api/v1/users?page=0&size=10&sort=createdAt,desc
```

### 查詢審計日誌

```http
GET /api/v1/users/1/audit-logs?page=0&size=20
```

---

## 錯誤回應格式

所有錯誤統一回傳 `ApiErrorResponse` 格式：

```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '99'",
  "path": "/api/v1/users/99",
  "details": null
}
```

### 錯誤碼對照

| HTTP 狀態碼 | 觸發條件 | 範例 |
|------------|---------|------|
| 400 Bad Request | 請求驗證失敗 | username 為空、email 格式不正確 |
| 404 Not Found | 資源不存在 | 查詢不存在的 User 或 Role |
| 409 Conflict | 資源重複 | username 或 email 已被使用 |
| 500 Internal Server Error | 非預期錯誤 | 伺服器內部異常 |

**400 驗證失敗範例：**

```json
{
  "timestamp": "2026-02-08T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users",
  "details": [
    "username: Username is required",
    "password: Password must be between 8 and 100 characters"
  ]
}
```

---

## 資料庫 Schema

### Table: `users`

| 欄位 | DB 型別 | 約束 | 說明 |
|------|---------|------|------|
| id | BIGSERIAL | PK | 自增主鍵 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 帳號 |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 電子郵件 |
| password | VARCHAR(255) | NOT NULL | 密碼雜湊（bcrypt=60 chars, argon2id~97 chars, 預留 255） |
| first_name | VARCHAR(50) | NULL | 名 |
| last_name | VARCHAR(50) | NULL | 姓 |
| phone | VARCHAR(20) | NULL | 電話（國際格式最長 15 碼 + 前綴） |
| enabled | BOOLEAN | NOT NULL, DEFAULT FALSE | 帳號啟用狀態 |
| locked | BOOLEAN | NOT NULL, DEFAULT FALSE | 帳號鎖定狀態（true = 鎖定） |
| last_login_at | TIMESTAMPTZ | NULL | 反正規化快取：最後登入時間 |
| last_login_ip | VARCHAR(45) | NULL | 反正規化快取：最後登入 IP（IPv6 最長 45 字元） |
| created_at | TIMESTAMPTZ | NOT NULL | 建立時間 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新時間 |

### Table: `roles`

| 欄位 | DB 型別 | 約束 | 說明 |
|------|---------|------|------|
| id | BIGSERIAL | PK | 自增主鍵 |
| name | VARCHAR(50) | UNIQUE, NOT NULL | 角色名，如 ROLE_USER, ROLE_ADMIN |
| description | VARCHAR(255) | NULL | 角色描述 |
| created_at | TIMESTAMPTZ | NOT NULL | 建立時間 |
| updated_at | TIMESTAMPTZ | NOT NULL | 更新時間 |

### Table: `user_roles`（關聯表）

| 欄位 | DB 型別 | 約束 | 說明 |
|------|---------|------|------|
| user_id | BIGINT | FK -> users(id), NOT NULL | 使用者外鍵 |
| role_id | BIGINT | FK -> roles(id), NOT NULL | 角色外鍵 |

### Table: `audit_logs`

| 欄位 | DB 型別 | 約束 | 說明 |
|------|---------|------|------|
| id | BIGSERIAL | PK | 自增主鍵 |
| user_id | BIGINT | FK -> users(id), NOT NULL | 關聯使用者 |
| event_type | VARCHAR(30) | NOT NULL | 事件類型（Enum stored as STRING） |
| ip_address | VARCHAR(45) | NULL | 來源 IP |
| user_agent | VARCHAR(500) | NULL | 瀏覽器/客戶端 UA |
| details | TEXT | NULL | 額外上下文資訊 |
| created_at | TIMESTAMPTZ | NOT NULL | 事件發生時間 |

---

## 索引設計

### users

| 索引名稱 | 欄位 | 類型 | 用途 |
|---------|------|------|------|
| `uk_users_username` | username | UNIQUE | 登入查詢主要路徑 |
| `uk_users_email` | email | UNIQUE | 密碼重設、信箱驗證 |

`enabled` / `locked` 為低基數欄位（boolean），不額外建立索引。若未來需要可加條件索引（partial index）。

### roles

| 索引名稱 | 欄位 | 類型 | 用途 |
|---------|------|------|------|
| `uk_roles_name` | name | UNIQUE | 角色名稱查詢 |

### user_roles

| 索引名稱 | 欄位 | 類型 | 用途 |
|---------|------|------|------|
| PK | (user_id, role_id) | COMPOSITE PK | 唯一約束 + 查某用戶所有角色 |
| `idx_user_roles_role_id` | role_id | INDEX | 反向查詢：查某角色下所有用戶 |

### audit_logs

| 索引名稱 | 欄位 | 類型 | 用途 |
|---------|------|------|------|
| `idx_audit_logs_user_id_created_at` | (user_id, created_at DESC) | COMPOSITE | 查某用戶最近的審計紀錄（最頻繁查詢） |
| `idx_audit_logs_event_type` | event_type | INDEX | 安全監控（如查所有登入失敗事件） |
| `idx_audit_logs_created_at` | created_at | INDEX | 全域時間範圍查詢 |

> audit_logs 是高寫入表，未來資料量大時建議依 `created_at` 做 table partitioning（按月分區）。

---

## 審計事件類型（AuditEventType）

| 事件 | 說明 | 觸發時機 |
|------|------|---------|
| `ACCOUNT_CREATED` | 帳號建立 | POST /api/v1/users |
| `ACCOUNT_UPDATED` | 帳號更新 | PUT /api/v1/users/{id} |
| `ACCOUNT_DISABLED` | 帳號停用 | 預留，未來 enabled 設為 false 時 |
| `ACCOUNT_ENABLED` | 帳號啟用 | 預留，未來 enabled 設為 true 時 |
| `ROLE_CHANGE` | 角色變更 | PUT /api/v1/users/{id}/roles |
| `LOGIN` | 登入成功 | 預留，未來認證模組使用 |
| `LOGOUT` | 登出 | 預留，未來認證模組使用 |
| `LOGIN_FAILED` | 登入失敗 | 預留，未來認證模組使用 |
| `PASSWORD_CHANGE` | 密碼變更 | 預留，未來密碼變更 API 使用 |

---

## DTO 驗證規則

### CreateUserRequest

| 欄位 | 驗證 | 規則 |
|------|------|------|
| username | `@NotBlank` `@Size(3, 50)` | 必填，3-50 字元 |
| email | `@NotBlank` `@Email` | 必填，合法 email 格式 |
| password | `@NotBlank` `@Size(8, 100)` | 必填，8-100 字元 |
| firstName | `@Size(max=50)` | 選填 |
| lastName | `@Size(max=50)` | 選填 |
| phone | `@Size(max=20)` | 選填 |

### UpdateUserRequest

| 欄位 | 驗證 | 規則 |
|------|------|------|
| email | `@Email` | 選填，若提供須為合法 email |
| firstName | `@Size(max=50)` | 選填 |
| lastName | `@Size(max=50)` | 選填 |
| phone | `@Size(max=20)` | 選填 |
| enabled | 無 | 選填，Boolean 型別 |

### CreateRoleRequest

| 欄位 | 驗證 | 規則 |
|------|------|------|
| name | `@NotBlank` `@Size(max=50)` | 必填，最長 50 字元 |
| description | `@Size(max=255)` | 選填 |

---

## 架構設計決策

### 混合式審計架構

採用**反正規化快取 + 獨立審計表**的混合方案：

- `users.last_login_at` / `users.last_login_ip`：反正規化欄位，避免每次查詢使用者時都需 JOIN audit_logs
- `audit_logs` 表：完整的事件歷史紀錄，支援追蹤與分析

### Entity 欄位命名

- 使用**正向命名** `locked`（預設 false）取代 Spring Security 的 `accountNonLocked`
- 未來實作 `UserDetails` 介面時，只需 `return !this.locked`

### enabled 預設 false

配合自助註冊流程，使用者建立後預設未啟用，需經驗證後才將 `enabled` 設為 true。

### 為 Spring Security 預留的欄位

| 欄位 | 對應 UserDetails 方法 | 轉換邏輯 |
|------|---------------------|---------|
| `enabled` | `isEnabled()` | 直接回傳 |
| `locked` | `isAccountNonLocked()` | `return !this.locked` |

---

## 設定檔參考

### application.yml

```yaml
spring:
  application:
    name: user-api
  datasource:
    url: jdbc:postgresql://localhost:5432/user_api
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update        # 正式環境建議改為 validate 搭配 Flyway/Liquibase
    show-sql: true             # 正式環境建議關閉
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 8080

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

### docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: user-api-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: user_api
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

| 項目 | 值 | 說明 |
|------|-----|------|
| Image | `postgres:16-alpine` | 輕量級 Alpine 版本，約 100MB |
| Port | 5432:5432 | 對應 application.yml 預設 |
| POSTGRES_DB | user_api | 容器啟動時自動建立此資料庫 |
| Volume | postgres_data | 資料持久化，`docker-compose down` 不會遺失資料 |

### 正式環境注意事項

| 項目 | 開發環境 | 正式環境建議 |
|------|---------|------------|
| `ddl-auto` | update | validate（搭配 Flyway 或 Liquibase 管理 migration） |
| `show-sql` | true | false |
| `password` | 明文寫在 yml | 使用環境變數 `${DB_PASSWORD}` |
| Swagger UI | 開啟 | 視需求關閉或限制存取 |

---

## Maven 依賴

| 依賴 | 版本 | 用途 |
|------|------|------|
| spring-boot-starter-web | 3.2.5 (inherited) | REST API |
| spring-boot-starter-data-jpa | 3.2.5 (inherited) | JPA / Hibernate |
| spring-boot-starter-validation | 3.2.5 (inherited) | Jakarta Bean Validation |
| postgresql | inherited | PostgreSQL JDBC 驅動 |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI / OpenAPI 3 |
| spring-boot-starter-test | 3.2.5 (inherited) | 測試框架（scope: test） |
