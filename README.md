# Inkline — Backend

Spring Boot REST API for the Inkline blogging platform.

The backend provides authentication, user profiles, article management, image uploads, contact messages, and database integration for the Inkline frontend.

## Features

- JWT-based authentication
- User registration and login
- User profiles
- Profile avatar uploads
- Article creation and publishing
- Article cover image uploads
- Article feed and article details
- User/author information
- Contact Us API
- MySQL database integration
- Cloudinary image storage
- CORS configuration for the frontend
- OAuth2/social authentication support
- Production deployment with Railway

## Tech Stack

- Java 17+
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- MySQL 8+
- Maven
- Cloudinary
- OAuth2
- Gmail SMTP

---

# Requirements

Install the following before running the backend locally:

- Java 17 or newer
- Maven 3.9+
- MySQL 8+
- Git

Verify:

```powershell
java --version
mvn --version
mysql --version