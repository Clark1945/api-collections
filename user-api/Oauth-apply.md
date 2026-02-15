
● OAuth2 設定指南

每個 Provider 都需要申請 Client ID 和 Client Secret，並設定 Redirect URI。

Redirect URI（三個 Provider 都一樣的格式）

http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/github
http://localhost:8080/login/oauth2/code/line

  ---
1. Google

- 前往 https://console.cloud.google.com/ → APIs & Services → Credentials
- 建立 OAuth 2.0 Client ID（類型選 Web Application）
- 在 "Authorized redirect URIs" 加上 http://localhost:8080/login/oauth2/code/google
- 取得 Client ID 和 Client Secret

2. GitHub

- 前往 GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
- Homepage URL: http://localhost:8080
- Authorization callback URL: http://localhost:8080/login/oauth2/code/github
- 取得 Client ID 和 Client Secret

3. LINE

- 前往 https://developers.line.biz/ → 建立 Provider → 建立 LINE Login Channel
- Callback URL: http://localhost:8080/login/oauth2/code/line
- 取得 Channel ID（= Client ID）和 Channel Secret（= Client Secret）