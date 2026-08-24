# Google OAuth2 setup

The backend is now wired for Google OAuth2.

## 1. Configure Google Cloud

Use this redirect URI:

`http://localhost:8080/login/oauth2/code/google`

The browser origin is:

`http://localhost:5173`

## 2. Set credentials as environment variables

Do not commit the Google client secret to Git.

PowerShell for the current terminal:

```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
```

Then start Spring Boot from the same terminal/STS launch configuration.

Because the client secret was previously displayed in chat/screenshots, rotate/regenerate that Google secret before using it in the project.

## 3. Login URL

Open:

`http://localhost:8080/oauth2/authorization/google`

After successful login, Spring redirects to:

`http://localhost:5173/oauth-success?token=...`

The React app should store the token and call:

`GET /api/users/me`

with the bearer token to obtain the Inkline user.

## What was changed

- Added `spring-boot-starter-oauth2-client`.
- Enabled OAuth2 login in `SecurityConfig`.
- Changed session policy to `IF_REQUIRED` so Spring can maintain the OAuth2 authorization request during the Google redirect.
- Added `CustomOAuth2UserService` integration.
- Added `OAuth2SuccessHandler` to issue an Inkline JWT.
- Added `GET /api/users/me`.
- Restricted profile editing (`PUT /api/users/me`) to authenticated users instead of permitting all `/api/users/**`.
