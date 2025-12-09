# YogaDetox Backend – Spring Boot API

REST API for YogaDetox mobile app (Expo + React Native).

Provides secure access to yoga video courses, meditation audios, private sessions, and admin tools.

## Features

- 🎥 **Video courses** – Metadata + S3 storage
- 🎧 **Meditation audios** – Metadata + S3 storage
- 📅 **Private sessions** – Google Calendar integration
- 💬 **Direct chat** – WebSockets with admin
- 🔐 **JWT auth** – Login/register (email + Google), role-based access
- 🌐 **Multi-language** – Content in es/en
- 🧘 **Free vs Premium** – Role-based content access
- 👩‍💻 **Admin panel** – Manage users, content, sessions
- 💳 **Payments** – Stripe & MercadoPago (planned)

## API Modules

### Auth & Users
- Register / Login (email + Google)
- JWT issuance & validation
- Roles: `STUDENT_FREE`, `STUDENT_PREMIUM`, `ADMIN`

### Content
- CRUD for Courses (video)
- CRUD for Meditation Audios
- S3 upload URLs / file handling
- Public vs premium flags

### Private Sessions
- Create / list / cancel bookings
- Google Calendar sync (teacher calendars)

### Chat
- WebSocket endpoint for real-time admin chat

### Admin
- Manage users, roles, content visibility
- View bookings and activity

## Tech Stack

- **Java** + **Spring Boot** (REST, Security, WebSocket)
- **JWT** (stateless auth)
- **Amazon S3** (videos, audios)
- **Google Calendar API**
- **JPA / Hibernate** + **PostgreSQL**

## Local Setup

1. Configure `application.yml` / `.properties`:
   - Database connection
   - S3 credentials
   - JWT secret
   - Google API credentials

2. Run:
```bash
./mvnw spring-boot:run
```

API available at:
```
http://localhost:8080
```

## To Do

- Integrate Stripe & MercadoPago payments
- Add full i18n layer for texts/messages

