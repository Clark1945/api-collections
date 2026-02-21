# User API — 功能架構文件

## 目錄

1. [系統總覽](#系統總覽)
2. [技術棧](#技術棧)
3. [專案結構](#專案結構)
4. [認證與授權](#認證與授權)
5. [會員系統](#會員系統)
6. [錢包系統](#錢包系統)
7. [股票追蹤系統](#股票追蹤系統)
8. [審計日誌系統](#審計日誌系統)
9. [API 端點一覽](#api-端點一覽)
10. [頁面路由一覽](#頁面路由一覽)
11. [資料庫結構](#資料庫結構)
12. [設定參數](#設定參數)

---

## 系統總覽

```
┌─────────────────────────────────────────────────────────────┐
│                     瀏覽器 (Thymeleaf + JS)                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────┐ ┌──────┐ │
│  │ register │ │  login   │ │dashboard │ │wallet │ │stocks│ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └───┬───┘ └──┬───┘ │
│       │ form POST   │ form POST  │            │ AJAX   │ SSE │
└───────┼─────────────┼────────────┼────────────┼────────┼─────┘
        ▼             ▼            ▼            ▼        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot (user-api)                     │
│                                                              │
│  PageController ─── Thymeleaf 頁面路由 (session 驗證)         │
│  AuthController ─── REST /api/v1/auth/** (JWT)               │
│  UserController ─── REST /api/v1/users/** (JWT)              │
│  RoleController ─── REST /api/v1/roles/** (JWT)              │
│  WalletController ─ REST /api/v1/wallet/** (session)         │
│  StockController ── REST /api/v1/stocks/** (session + SSE)   │
│                                                              │
│  Services: Auth / User / Role / Wallet / TwseApi /           │
│            StockWatchlist / StockSse / AuditLog               │
│                                                              │
│  Cross-cutting: AuditLoggingAspect (@Auditable AOP)          │
│                 GlobalExceptionHandler                        │
│                 JwtAuthenticationFilter                       │
└──────────────┬──────────────────────────┬────────────────────┘
               ▼                          ▼
        ┌──────────┐              ┌──────────────┐
        │PostgreSQL│              │  TWSE API    │
        │ appdb    │              │ (外部即時報價) │
        └──────────┘              └──────────────┘
```

---

## 技術棧

| 類別 | 技術 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2.5 |
| 語言 | Java | 17 |
| 資料庫 | PostgreSQL | — |
| ORM | Spring Data JPA + Hibernate | — |
| 安全 | Spring Security + OAuth2 Client | — |
| JWT | jjwt (io.jsonwebtoken) | 0.12.6 |
| 模板引擎 | Thymeleaf | — |
| API 文件 | springdoc-openapi (Swagger) | 2.5.0 |
| 建置工具 | Maven | — |

---

## 專案結構

```
user-api/src/main/java/com/example/userapi/
├── UserApiApplication.java          # 啟動類 (@EnableScheduling, @EnableConfigurationProperties)
│
├── aspect/
│   ├── Auditable.java               # 審計註解
│   └── AuditLoggingAspect.java      # AOP 切面，支援 User / Wallet 回傳型別
│
├── config/
│   ├── JpaAuditingConfig.java       # @EnableJpaAuditing
│   ├── JwtProperties.java           # JWT 設定 (secret, expirationMs)
│   ├── OpenApiConfig.java           # Swagger 設定
│   ├── SecurityConfig.java          # Spring Security 過濾鏈
│   ├── TwseApiProperties.java       # TWSE API 連線設定
│   └── RestTemplateConfig.java      # RestTemplate Bean (含 timeout)
│
├── controller/
│   ├── PageController.java          # Thymeleaf 頁面路由 (MVC)
│   ├── AuthController.java          # REST — JWT 登入
│   ├── UserController.java          # REST — 使用者 CRUD
│   ├── RoleController.java          # REST — 角色管理
│   ├── WalletController.java        # REST — 錢包操作
│   └── StockController.java         # REST + SSE — 股票追蹤
│
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java     # 註冊
│   │   ├── LoginRequest.java        # 登入
│   │   ├── CreateUserRequest.java   # 建立使用者 (API)
│   │   ├── UpdateUserRequest.java   # 更新使用者
│   │   ├── CreateRoleRequest.java   # 建立角色
│   │   ├── TopUpRequest.java        # 儲值
│   │   ├── PaymentRequest.java      # 付款
│   │   └── AddStockRequest.java     # 新增追蹤股票
│   └── response/
│       ├── UserResponse.java
│       ├── AuthResponse.java
│       ├── RoleResponse.java
│       ├── AuditLogResponse.java
│       ├── ApiErrorResponse.java
│       ├── WalletResponse.java
│       ├── WalletTransactionResponse.java
│       ├── StockWatchlistResponse.java
│       └── StockQuoteResponse.java
│
├── entity/
│   ├── BaseEntity.java              # 抽象基底 (id, createdAt, updatedAt)
│   ├── User.java
│   ├── Role.java
│   ├── UserRole.java                # User ↔ Role 多對多關聯表
│   ├── AuditLog.java
│   ├── Wallet.java
│   ├── WalletTransaction.java
│   └── StockWatchlist.java
│
├── enums/
│   ├── UserStatus.java              # ENABLED / DISABLED / LOCKED / PENDING
│   ├── AuthProvider.java            # LOCAL / GOOGLE / GITHUB / LINE
│   ├── AuditEventType.java          # 12 種審計事件
│   └── TransactionType.java         # TOP_UP / PAYMENT
│
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── AuthenticationException.java
│   └── InsufficientBalanceException.java
│
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── UserRoleRepository.java
│   ├── AuditLogRepository.java
│   ├── WalletRepository.java
│   ├── WalletTransactionRepository.java
│   └── StockWatchlistRepository.java
│
├── security/
│   ├── JwtTokenProvider.java        # JWT 產生 / 驗證
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter
│   ├── CustomUserDetails.java       # UserDetails 實作
│   ├── CustomUserDetailsService.java
│   ├── CustomOAuth2User.java        # OAuth2User 包裝
│   ├── OAuth2UserService.java       # 第三方登入使用者處理
│   └── OAuth2LoginSuccessHandler.java
│
└── service/
    ├── AuthService.java             # 登入驗證
    ├── UserService.java             # 使用者 CRUD
    ├── RoleService.java             # 角色管理
    ├── AuditLogService.java         # 審計日誌寫入
    ├── WalletService.java           # 錢包儲值 / 付款
    ├── TwseApiService.java          # TWSE API 呼叫
    ├── StockWatchlistService.java   # 追蹤清單管理
    └── StockSseService.java         # SSE 推送排程
```

---

## 認證與授權

本系統同時支援兩套認證流程：

### 1. Session 認證（Thymeleaf 頁面）

```
瀏覽器 → POST /login (username + password)
       → AuthService.login() 驗證
       → HttpSession.setAttribute("user", user)
       → redirect /dashboard
```

- 適用於：`/login`、`/register`、`/dashboard`、`/wallet`、`/stocks`
- 頁面 Controller 內以 `session.getAttribute("user")` 手動檢查
- 登出時 `session.invalidate()`

### 2. JWT 認證（REST API）

```
客戶端 → POST /api/v1/auth/login (JSON body)
       → AuthenticationManager 驗證
       → JwtTokenProvider.generateToken()
       → 回傳 { token, tokenType: "Bearer", username }

後續請求 → Authorization: Bearer <token>
         → JwtAuthenticationFilter 攔截解析
         → SecurityContextHolder 設定認證
```

- 適用於：`/api/v1/users/**`、`/api/v1/roles/**`
- JWT 有效期：24 小時（可透過 `jwt.expiration-ms` 設定）

### 3. OAuth2 第三方登入

支援三個 Provider：

| Provider | 登入入口 | 取得資訊 |
|----------|---------|---------|
| Google | `/oauth2/authorization/google` | email, name (sub) |
| GitHub | `/oauth2/authorization/github` | email, name/login (id) |
| LINE | `/oauth2/authorization/line` | displayName (userId) |

流程：
1. 瀏覽器導向 Provider 授權頁
2. 回調至 `/login/oauth2/code/{provider}`
3. `OAuth2UserService` 查找或建立本地 User
4. `OAuth2LoginSuccessHandler` 回傳 JWT JSON

### SecurityConfig 路由規則

| 路由 | 存取權限 |
|------|---------|
| `/api/v1/auth/**` | 公開 |
| `POST /api/v1/users` | 公開 |
| `/login`, `/register` | 公開 |
| `/oauth2/**`, `/login/oauth2/**` | 公開 |
| `/swagger-ui/**`, `/v3/api-docs/**` | 公開 |
| `/dashboard`, `/logout`, `/wallet`, `/stocks`, `/account/delete` | 公開（Controller 內做 session 檢查） |
| `/api/v1/wallet/**`, `/api/v1/stocks/**` | 公開（Controller 內做 session 檢查） |
| `/css/**`, `/js/**`, `/images/**`, `/error` | 公開 |
| 其餘所有路由 | 需 JWT 認證 |

---

## 會員系統

### 資料模型

**User** 繼承 `BaseEntity`，欄位：

| 欄位 | 類型 | 說明 |
|------|------|------|
| username | String(50) | 唯一 |
| email | String(255) | 唯一 |
| password | String(255) | BCrypt 加密，OAuth 使用者可為 null |
| firstName | String(50) | 選填 |
| lastName | String(50) | 選填 |
| phone | String(20) | 選填 |
| status | UserStatus | ENABLED / DISABLED / LOCKED / PENDING |
| lastLoginAt | Instant | 最近登入時間 |
| lastLoginIp | String(45) | 最近登入 IP |
| provider | AuthProvider | LOCAL / GOOGLE / GITHUB / LINE |
| providerId | String(255) | 第三方 Provider ID |
| userRoles | Set\<UserRole\> | 角色關聯（EAGER） |

### 狀態機

```
          建立
           │
           ▼
  ┌──── PENDING ────┐
  │                  │
  │    setEnabled    │
  │    (false)       │    OAuth 自動 ENABLED
  ▼                  ▼
DISABLED ◄──── ENABLED
               │
               │ lock
               ▼
             LOCKED
```

- `ENABLED`：可登入
- `DISABLED`：軟刪除，不可登入
- `LOCKED`：鎖定，不可登入
- `PENDING`：待審核，不可登入

### 功能

| 功能 | 入口 | 說明 |
|------|------|------|
| 註冊 | `GET/POST /register` | 建立 User (ENABLED) + 自動建立 Wallet，導向登入頁 |
| 登入 | `GET/POST /login` | 驗證帳密，更新 lastLoginAt/IP，建立 session |
| 登出 | `POST /logout` | 銷毀 session |
| 刪除帳號 | `POST /account/delete` | 軟刪除（status → DISABLED），銷毀 session |
| API CRUD | `/api/v1/users/**` | 建立、列表、查詢、更新、軟刪除（需 JWT） |
| 角色指派 | `PUT /api/v1/users/{id}/roles` | 取代使用者所有角色 |

---

## 錢包系統

### 資料模型

**Wallet**

| 欄位 | 類型 | 說明 |
|------|------|------|
| user | User | `@OneToOne(LAZY)`, unique FK |
| balance | BigDecimal(15,2) | 預設 0.00 |

**WalletTransaction**

| 欄位 | 類型 | 說明 |
|------|------|------|
| wallet | Wallet | `@ManyToOne(LAZY)` |
| type | TransactionType | TOP_UP / PAYMENT |
| amount | BigDecimal(15,2) | 交易金額 |
| balanceAfter | BigDecimal(15,2) | 交易後餘額快照 |
| description | String(255) | 交易描述 |

### 業務邏輯

```
儲值 (TopUp)
  1. 查找使用者的 Wallet
  2. balance += amount
  3. 儲存 Wallet
  4. 建立 WalletTransaction (type=TOP_UP, balanceAfter=新餘額)

付款 (Payment)
  1. 查找使用者的 Wallet
  2. 檢查 balance >= amount，否則拋出 InsufficientBalanceException
  3. balance -= amount
  4. 儲存 Wallet
  5. 建立 WalletTransaction (type=PAYMENT, balanceAfter=新餘額)
```

### API 端點

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/v1/wallet` | 查詢錢包餘額 |
| POST | `/api/v1/wallet/top-up` | 儲值 `{ amount }` |
| POST | `/api/v1/wallet/payment` | 付款 `{ amount, description }` |
| GET | `/api/v1/wallet/transactions?page=0&size=20` | 交易紀錄（分頁） |

### 頁面

`/wallet` — 錢包頁面，包含：
- 餘額即時顯示
- 儲值表單（AJAX）
- 付款表單（AJAX）
- 交易紀錄表格（分頁）

---

## 股票追蹤系統

### 資料模型

**StockWatchlist**

| 欄位 | 類型 | 說明 |
|------|------|------|
| user | User | `@ManyToOne(LAZY)` |
| stockSymbol | String(10) | 股票代號，如 "2330" |
| stockName | String(50) | 股票名稱，如 "台積電" |

- 唯一約束：`(user_id, stock_symbol)`

### TWSE API 串接

呼叫台灣證交所公開 API 取得即時報價：

```
GET https://mis.twse.com.tw/stock/api/getStockInfo.jsp?ex_ch=tse_2330.tw|tse_2317.tw
```

回傳 JSON 解析對應欄位：

| TWSE 欄位 | 對應 DTO 欄位 | 說明 |
|-----------|-------------|------|
| c | stockSymbol | 代號 |
| n | stockName | 名稱 |
| z | currentPrice | 成交價（"-" 表示尚無成交） |
| y | previousClose | 昨收 |
| o | openPrice | 開盤 |
| h | highPrice | 最高 |
| l | lowPrice | 最低 |
| v | volume | 成交量 |
| t | timestamp | 時間 |

漲跌與漲跌幅由 `currentPrice - previousClose` 計算。

### SSE 即時推送

```
StockSseService
  ├── ConcurrentHashMap<Long userId, SseEmitter> — 管理所有 SSE 連線
  │
  └── @Scheduled(fixedRate = 15000ms)
      pushStockUpdates():
        1. 收集所有連線用戶的追蹤代號（去重）
        2. 批次呼叫 TWSE API（用 | 串接多股一次請求）
        3. 依用戶過濾後推送各自的報價資料
        4. 清除失效的 emitter

SSE 事件名稱: "stock-update"
SSE 資料格式: JSON 陣列 [StockQuoteResponse, ...]
Emitter 逾時: 300 秒（5 分鐘）
```

瀏覽器端透過 `EventSource('/api/v1/stocks/sse')` 接收，斷線時瀏覽器自動重連。

### API 端點

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/v1/stocks/watchlist` | 取得追蹤清單 |
| POST | `/api/v1/stocks/watchlist` | 新增追蹤 `{ stockSymbol }` |
| DELETE | `/api/v1/stocks/watchlist/{symbol}` | 移除追蹤 |
| GET | `/api/v1/stocks/quote/{symbol}` | 單股即時報價 |
| GET | `/api/v1/stocks/sse` | SSE 連線（text/event-stream） |

### 頁面

`/stocks` — 股票追蹤頁面，包含：
- 新增追蹤表單（輸入 4-6 位數字股票代號）
- 追蹤清單表格：代號、名稱、現價、漲跌、漲跌幅、成交量、最高、最低、開盤、昨收、移除按鈕
- SSE 即時連線狀態指示燈
- 走勢圖（Canvas 繪製日內價格折線圖，累積 SSE 推送的價格點）

### 走勢圖

- 使用原生 Canvas API 繪製（無第三方圖表庫）
- 最多保留 100 個數據點
- 多股同時顯示，用不同顏色區分
- 含格線、Y 軸價格標籤、圖例

---

## 審計日誌系統

### 機制

使用 Spring AOP `@AfterReturning` 切面，攔截標註 `@Auditable` 的方法：

```java
@Auditable(eventType = AuditEventType.ACCOUNT_CREATED)
public User createUser(CreateUserRequest request) { ... }
```

切面邏輯：
1. 檢查回傳值是否為 `User` 或 `Wallet`（Wallet 透過 `wallet.getUser()` 取得 User）
2. 若 `detail` 屬性非空，以 SpEL 表達式解析（可用 `#args`、`#result`）
3. 呼叫 `AuditLogService.log(user, eventType, detail)`

### 審計事件類型

| 事件 | 觸發時機 |
|------|---------|
| LOGIN | 登入成功 |
| LOGOUT | 登出 |
| LOGIN_FAILED | 登入失敗 |
| PASSWORD_CHANGE | 密碼變更 |
| ROLE_CHANGE | 角色異動 |
| ACCOUNT_CREATED | 建立帳號 |
| ACCOUNT_UPDATED | 更新帳號 |
| ACCOUNT_DISABLED | 停用帳號 |
| ACCOUNT_ENABLED | 啟用帳號 |
| WALLET_TOP_UP | 錢包儲值 |
| WALLET_PAYMENT | 錢包付款 |
| STOCK_WATCHLIST_ADD | 新增追蹤 |
| STOCK_WATCHLIST_REMOVE | 移除追蹤 |

### 資料模型

**AuditLog**（不繼承 BaseEntity）

| 欄位 | 類型 | 說明 |
|------|------|------|
| id | Long | 自增主鍵 |
| user | User | `@ManyToOne(LAZY)` |
| eventType | AuditEventType | 事件類型 |
| ipAddress | String(45) | 來源 IP |
| userAgent | String(500) | User-Agent |
| details | TEXT | 詳細資訊 |
| createdAt | Instant | 建立時間 |

索引：`(user_id, created_at DESC)`、`(event_type)`、`(created_at)`

---

## API 端點一覽

### REST API（JWT 認證）

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/v1/auth/login` | JWT 登入 |
| POST | `/api/v1/users` | 建立使用者（公開） |
| GET | `/api/v1/users` | 使用者列表（分頁） |
| GET | `/api/v1/users/{id}` | 查詢使用者 |
| PUT | `/api/v1/users/{id}` | 更新使用者 |
| DELETE | `/api/v1/users/{id}` | 軟刪除使用者 |
| PUT | `/api/v1/users/{id}/roles` | 指派角色 |
| GET | `/api/v1/users/{id}/audit-logs` | 查詢審計日誌 |
| POST | `/api/v1/roles` | 建立角色 |
| GET | `/api/v1/roles` | 角色列表 |
| GET | `/api/v1/roles/{id}` | 查詢角色 |
| DELETE | `/api/v1/roles/{id}` | 刪除角色 |

### REST API（Session 認證）

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/v1/wallet` | 查詢錢包 |
| POST | `/api/v1/wallet/top-up` | 儲值 |
| POST | `/api/v1/wallet/payment` | 付款 |
| GET | `/api/v1/wallet/transactions` | 交易紀錄 |
| GET | `/api/v1/stocks/watchlist` | 追蹤清單 |
| POST | `/api/v1/stocks/watchlist` | 新增追蹤 |
| DELETE | `/api/v1/stocks/watchlist/{symbol}` | 移除追蹤 |
| GET | `/api/v1/stocks/quote/{symbol}` | 即時報價 |
| GET | `/api/v1/stocks/sse` | SSE 連線 |

---

## 頁面路由一覽

| 方法 | 路徑 | 模板 | 說明 |
|------|------|------|------|
| GET | `/login` | login.html | 登入頁 |
| POST | `/login` | — | 處理登入 → redirect |
| GET | `/register` | register.html | 註冊頁 |
| POST | `/register` | — | 處理註冊 → redirect |
| GET | `/dashboard` | dashboard.html | 儀表板 |
| GET | `/wallet` | wallet.html | 錢包頁 |
| GET | `/stocks` | stocks.html | 股票追蹤頁 |
| POST | `/account/delete` | — | 刪除帳號 → redirect |
| POST | `/logout` | — | 登出 → redirect |

### 頁面導航關係

```
                  ┌─────────┐
           ┌──────│ register│
           │      └────┬────┘
           │           │ 註冊成功
           │           ▼
           │      ┌─────────┐
           └──────│  login   │◄──── 登出 / 刪除帳號
                  └────┬────┘
                       │ 登入成功
                       ▼
                 ┌───────────┐
                 │ dashboard  │
                 │            │
                 │ [錢包] [股票]│
                 │ [登出] [刪除]│
                 └──┬─────┬──┘
                    │     │
              ┌─────┘     └─────┐
              ▼                 ▼
        ┌──────────┐     ┌──────────┐
        │  wallet   │     │  stocks   │
        │           │     │           │
        │ [儀表板]   │     │ [儀表板]   │
        │ [股票追蹤] │     │ [錢包]    │
        └──────────┘     └──────────┘
```

---

## 資料庫結構

Hibernate `ddl-auto: update` 自動建立/更新表結構。

### ER Diagram

```
┌──────────┐     ┌────────────┐     ┌──────────┐
│  users   │     │ user_roles │     │  roles   │
├──────────┤     ├────────────┤     ├──────────┤
│ id (PK)  │◄────│ user_id(FK)│     │ id (PK)  │
│ username │     │ role_id(FK)│────►│ name     │
│ email    │     │ id (PK)   │     │ description│
│ password │     └────────────┘     └──────────┘
│ status   │
│ provider │     ┌──────────────┐
│ ...      │◄────│ audit_logs   │
└────┬─────┘     ├──────────────┤
     │           │ id (PK)      │
     │           │ user_id (FK) │
     │           │ event_type   │
     │           │ details      │
     │           │ created_at   │
     │           └──────────────┘
     │
     │ 1:1       ┌──────────────────────┐
     ├──────────►│ wallets              │
     │           ├──────────────────────┤
     │           │ id (PK)             │
     │           │ user_id (FK, UNIQUE)│
     │           │ balance             │
     │           └────────┬────────────┘
     │                    │ 1:N
     │                    ▼
     │           ┌──────────────────────┐
     │           │ wallet_transactions  │
     │           ├──────────────────────┤
     │           │ id (PK)             │
     │           │ wallet_id (FK)      │
     │           │ type                │
     │           │ amount              │
     │           │ balance_after       │
     │           │ description         │
     │           └──────────────────────┘
     │
     │ 1:N       ┌──────────────────────┐
     └──────────►│ stock_watchlist      │
                 ├──────────────────────┤
                 │ id (PK)             │
                 │ user_id (FK)        │
                 │ stock_symbol        │
                 │ stock_name          │
                 │ UNIQUE(user_id,     │
                 │   stock_symbol)     │
                 └──────────────────────┘
```

### 各表欄位明細

#### users
| 欄位 | 類型 | 約束 |
|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NULLABLE |
| first_name | VARCHAR(50) | |
| last_name | VARCHAR(50) | |
| phone | VARCHAR(20) | |
| status | VARCHAR(30) | NOT NULL |
| last_login_at | TIMESTAMP | |
| last_login_ip | VARCHAR(45) | |
| provider | VARCHAR(20) | NOT NULL |
| provider_id | VARCHAR(255) | |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

#### wallets
| 欄位 | 類型 | 約束 |
|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | FK → users, NOT NULL, UNIQUE |
| balance | DECIMAL(15,2) | NOT NULL, DEFAULT 0.00 |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

#### wallet_transactions
| 欄位 | 類型 | 約束 |
|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT |
| wallet_id | BIGINT | FK → wallets, NOT NULL |
| type | VARCHAR(20) | NOT NULL (TOP_UP / PAYMENT) |
| amount | DECIMAL(15,2) | NOT NULL |
| balance_after | DECIMAL(15,2) | NOT NULL |
| description | VARCHAR(255) | |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

#### stock_watchlist
| 欄位 | 類型 | 約束 |
|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | FK → users, NOT NULL |
| stock_symbol | VARCHAR(10) | NOT NULL |
| stock_name | VARCHAR(50) | |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |
| | | UNIQUE(user_id, stock_symbol) |

---

## 設定參數

### application.yml

```yaml
# 資料庫
spring.datasource.url: jdbc:postgresql://localhost:5432/appdb
spring.datasource.username: admin
spring.datasource.password: admin
spring.jpa.hibernate.ddl-auto: update

# JWT
jwt.secret: ${JWT_SECRET:...}
jwt.expiration-ms: ${JWT_EXPIRATION_MS:86400000}       # 24 小時

# OAuth2 (Google / GitHub / LINE)
spring.security.oauth2.client.registration.*

# TWSE API
twse.api.base-url: https://mis.twse.com.tw/stock/api
twse.api.connect-timeout-ms: 5000                      # 連線逾時
twse.api.read-timeout-ms: 10000                        # 讀取逾時

# SSE 推送
stock.sse.push-interval-ms: 15000                      # 推送間隔（15 秒）
stock.sse.emitter-timeout-ms: 300000                   # SSE 連線逾時（5 分鐘）

# Server
server.port: 8080

# Swagger
springdoc.swagger-ui.path: /swagger-ui.html
springdoc.api-docs.path: /v3/api-docs
```

---

## 異常處理

`GlobalExceptionHandler` 統一處理所有 REST 異常：

| 異常 | HTTP Status | 說明 |
|------|-------------|------|
| ResourceNotFoundException | 404 | 資源不存在 |
| DuplicateResourceException | 409 | 資源重複 |
| InsufficientBalanceException | 400 | 餘額不足 |
| IllegalStateException | 400 | 非法狀態 |
| MethodArgumentNotValidException | 400 | 驗證失敗（含欄位明細） |
| BadCredentialsException | 401 | 帳密錯誤 |
| DisabledException | 401 | 帳號停用 |
| LockedException | 401 | 帳號鎖定 |
| Exception (catch-all) | 500 | 未預期錯誤 |

回傳格式：

```json
{
  "timestamp": "2026-02-18T...",
  "status": 400,
  "error": "Bad Request",
  "message": "餘額不足，目前餘額: 100.00，需要: 500.00",
  "path": "/api/v1/wallet/payment"
}
```
