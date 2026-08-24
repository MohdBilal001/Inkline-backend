# Inkline Backend

Spring Boot + MySQL backend for the Inkline Medium-style blogging platform.

## Requirements

- Java 17+
- Maven 3.9+ (or use the Maven wrapper if added later)
- MySQL 8+

## 1. Create the database

In MySQL:

```sql
CREATE DATABASE inkline CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 2. Configure credentials

You can use environment variables:

Windows PowerShell:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="replace-with-a-long-random-secret-at-least-32-characters"
```

The default database URL is:

```text
jdbc:mysql://localhost:3306/inkline
```

## 3. Run

From this directory:

```powershell
mvn spring-boot:run
```

Backend:

```text
http://localhost:8080
```

## Initial API

### Signup

POST `/api/auth/signup`

```json
{
  "name": "Ravi Kumar",
  "username": "ravi.codes",
  "email": "ravi@example.com",
  "password": "password123"
}
```

### Login

POST `/api/auth/login`

```json
{
  "email": "ravi@example.com",
  "password": "password123"
}
```

The response contains a JWT and user object.

### Feed

GET `/api/articles`

### Article

GET `/api/articles/{slug}`

### User

GET `/api/users/{id}`

### Create article

POST `/api/articles`

Header:

```text
Authorization: Bearer YOUR_JWT
```

Body:

```json
{
  "title": "My first Inkline article",
  "excerpt": "A short introduction.",
  "content": "Article content goes here.",
  "status": "PUBLISHED",
  "readingTime": 3
}
```

## Important

`spring.jpa.hibernate.ddl-auto=update` is convenient for development. Before production, use Flyway or Liquibase migrations and set the schema strategy appropriately.


## Contact Us email

The backend now exposes a public contact endpoint:

`POST /api/contact`

Request body:

```json
{
  "name": "Visitor Name",
  "email": "visitor@example.com",
  "subject": "Question about Inkline",
  "message": "Hello, I have a question..."
}
```

The backend sends the message through Gmail SMTP to `CONTACT_TO`. The visitor's email is set as `Reply-To`.

### Gmail configuration

Use a Google App Password rather than your normal Gmail password.

Windows PowerShell:

```powershell
$env:MAIL_USERNAME="yourgmail@gmail.com"
$env:MAIL_PASSWORD="your-16-character-app-password"
$env:CONTACT_TO="yourgmail@gmail.com"
mvn spring-boot:run
```

The Contact Us endpoint is public, so visitors do not need to log in.
