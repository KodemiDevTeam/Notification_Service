# Notification Service

A Spring Boot microservice that sends notifications through multiple channels — Email, SMS, Push, and In-App. It uses AWS DynamoDB for storage, JWT for authentication, and Twilio for SMS delivery.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [How It Works — Architecture](#4-how-it-works--architecture)
5. [Configuration](#5-configuration)
6. [Authentication — JWT](#6-authentication--jwt)
7. [Notification Channels](#7-notification-channels)
8. [REST API Endpoints](#8-rest-api-endpoints)
9. [Data Models](#9-data-models)
10. [Scheduler — Auto Retry](#10-scheduler--auto-retry)
11. [Exception Handling](#11-exception-handling)
12. [Running Locally](#12-running-locally)

---

## 1. Project Overview

This service handles the full lifecycle of a notification:

- A client sends a request with a JWT token
- The service validates the token, extracts the user identity
- It checks the user's notification preferences
- It dispatches the notification through the correct channel (Email / SMS / Push / In-App)
- It saves the notification to DynamoDB with a status (PENDING → SENT / FAILED)
- A background scheduler retries any PENDING notifications automatically

---

## 2. Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Language |
| Spring Boot 2.7.18 | Framework |
| AWS DynamoDB (v1 SDK) | Database |
| spring-data-dynamodb | DynamoDB repository support |
| Twilio SDK | SMS sending |
| Spring Mail (JavaMailSender) | Email sending |
| JJWT (io.jsonwebtoken) | JWT token parsing |
| Lombok | Boilerplate reduction |
| Spring Validation | Request body validation |

---

## 3. Project Structure

```
src/main/java/org/notification/
│
├── Main.java                        # Spring Boot entry point
│
├── channel/                         # Channel implementations
│   ├── NotificationChannel.java     # Interface all channels implement
│   ├── EmailChannel.java            # Sends via JavaMailSender
│   ├── SmsChannel.java              # Sends via Twilio
│   ├── PushChannel.java             # Logs push (stub)
│   └── InAppChannel.java            # Saves to DynamoDB inapp table
│
├── config/
│   ├── DynamoDBConfig.java          # AmazonDynamoDB + DynamoDBMapper beans
│   └── TwilioConfig.java            # Twilio credentials config
│
├── controller/
│   ├── NotificationController.java  # CRUD for notifications
│   ├── InAppController.java         # In-app notification endpoints
│   ├── UserPreferenceController.java# User preference endpoints
│   └── GlobalExceptionHandler.java  # Centralized error handling
│
├── exception/                       # Custom exception classes
│
├── model/
│   ├── Notification.java            # Main notification entity
│   ├── InAppNotification.java       # In-app specific entity
│   ├── UserPreference.java          # User preference entity
│   └── enums/
│       ├── ChannelType.java         # EMAIL, SMS, PUSH, IN_APP
│       ├── NotificationType.java    # COURSE_ALERT, STREAK_ALERT, etc.
│       └── RoleType.java            # LEARNER, TRAINER, ADMIN
│
├── repository/
│   ├── NotificationRepository.java  # DynamoDB ops for Notification
│   ├── InAppRepository.java         # DynamoDB ops for InAppNotification
│   └── UserPreferenceRepository.java# DynamoDB ops for UserPreference
│
├── scheduler/
│   └── NotificationScheduler.java   # Retries PENDING notifications
│
├── security/
│   └── JwtUtil.java                 # JWT generate / parse / validate
│
└── service/
    ├── NotificationService.java     # Core business logic
    ├── ChannelDispatcherService.java# Routes notification to correct channel
    ├── EmailService.java            # Email sending logic
    ├── SmsService.java              # SMS sending logic
    ├── InAppService.java            # In-app creation logic
    └── UserPreferenceService.java   # Preference save/get logic
```

---

## 4. How It Works — Architecture

```
Client Request (with JWT)
        │
        ▼
NotificationController
        │  extracts userId + email from JWT
        ▼
NotificationService
        │  validates input
        │  checks UserPreference (is this notification type allowed?)
        │  saves to DynamoDB with status = PENDING
        │
        ▼
processChannel()
        │
        ├── channel = EMAIL  →  EmailService → JavaMailSender
        ├── channel = SMS    →  SmsService   → Twilio API
        └── channel = IN_APP →  InAppService → DynamoDB (inapp_notifications table)
        │
        ▼
status updated to SENT or FAILED → saved to DynamoDB

        (background)
NotificationScheduler (every 10 seconds)
        │  scans all PENDING notifications
        │  dispatches via ChannelDispatcherService
        │  retries up to 3 times on failure
        └── marks SENT or FAILED permanently
```

---

## 5. Configuration

All config lives in `src/main/resources/application.yml`.

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USER:dummy@gmail.com}   # set via env var
    password: ${EMAIL_PASS:dummy}             # set via env var

aws:
  region: us-east-1
  accessKeyId: your-key
  secretKey: your-secret
  dynamodb:
    endpoint: http://localhost:8000           # local DynamoDB

twilio:
  account-sid: ${TWILIO_ACCOUNT_SID:dummy}
  auth-token: ${TWILIO_AUTH_TOKEN:dummy}
  phone-number: ${TWILIO_PHONE_NUMBER:+10000000000}

notification:
  scheduler:
    interval: 10000    # runs every 10 seconds
    max-retry: 3       # max retry attempts before marking FAILED
```

### Environment Variables

Set these before running (or they fall back to dummy values for local dev):

| Variable | Description |
|---|---|
| `EMAIL_USER` | Gmail address used to send emails |
| `EMAIL_PASS` | Gmail app password |
| `TWILIO_ACCOUNT_SID` | Twilio account SID |
| `TWILIO_AUTH_TOKEN` | Twilio auth token |
| `TWILIO_PHONE_NUMBER` | Twilio sender phone number |

---

## 6. Authentication — JWT

Every API request must include a JWT in the `Authorization` header:

```
Authorization: Bearer <token>
```

`JwtUtil` handles token operations:

| Method | Description |
|---|---|
| `generateToken(userId, email, role)` | Creates a signed JWT valid for 10 hours |
| `getUserId(token)` | Extracts the subject (userId) from the token |
| `getEmail(token)` | Extracts the email claim |
| `getRole(token)` | Extracts the role claim |
| `validateToken(token)` | Returns true if token is not expired |

The secret key is HMAC-SHA based. In production, move it to an environment variable.

---

## 7. Notification Channels

All channels implement the `NotificationChannel` interface:

```java
public interface NotificationChannel {
    String getChannelName();
    void send(Notification notification);
}
```

### EmailChannel
- Uses Spring's `JavaMailSender`
- Requires `notification.email` to be set
- Throws `InvalidEmailException` if email is missing

### SmsChannel
- Uses Twilio SDK
- Requires `notification.phoneNumber` to be set
- Twilio credentials injected via `@Value`
- Throws `InvalidPhoneNumberException` or `SmsSendingException` on failure

### PushChannel
- Stub implementation — logs the device token and message
- Requires `notification.deviceToken` to be set
- Throws `InvalidDeviceTokenException` if token is missing
- Ready to integrate with FCM or APNs

### InAppChannel
- Saves an `InAppNotification` record to DynamoDB (`inapp_notifications` table)
- Requires `notification.userId` to be set
- Throws `InvalidUserIdException` if userId is missing

### ChannelDispatcherService
Holds a `Map<String, NotificationChannel>` built from all channel beans at startup. When `dispatch(notification)` is called, it looks up the channel by name and calls `send()`. Handles retry count on failure.

---

## 8. REST API Endpoints

### Notification Endpoints — `/api/notifications`

| Method | URL | Auth | Description |
|---|---|---|---|
| POST | `/api/notifications` | JWT | Create and send a notification |
| GET | `/api/notifications` | JWT | Get all notifications for the logged-in user |
| GET | `/api/notifications/{id}` | None | Get a notification by ID |
| PUT | `/api/notifications/{id}` | None | Update a notification |
| DELETE | `/api/notifications/{id}` | None | Delete a notification |

#### POST /api/notifications — Request Body

```json
{
  "title": "Live Class Starting",
  "description": "Your class starts in 10 minutes",
  "type": "SESSION_REMINDER",
  "channel": "EMAIL",
  "email": "user@example.com",
  "phoneNumber": "+919999999999",
  "deviceToken": "fcm-token-here",
  "isScheduled": false,
  "scheduledTime": null,
  "redirectUrl": "https://app.example.com/class/123"
}
```

`channel` must be one of: `EMAIL`, `SMS`, `PUSH`, `IN_APP`

`type` must be one of: `COURSE_ALERT`, `TRAINER_LIVE`, `STREAK_ALERT`, `FEEDBACK_ALERT`, `SESSION_REMINDER`, `BADGE_UPDATE`, `ADMIN_BROADCAST`, `REWARD_REMINDER`, `PAYOUT_UPDATE`, `NEW_ENROLLMENT`

---

### In-App Endpoints — `/api/inapps`

| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/api/inapps` | JWT | Get all in-app notifications (sorted newest first) |
| GET | `/api/inapps/unread` | JWT | Get unread notifications |
| GET | `/api/inapps/unread/count` | JWT | Get count of unread notifications |
| GET | `/api/inapps/one/{id}` | None | Get a single in-app notification by ID |
| PUT | `/api/inapps/read/{id}` | None | Mark a notification as read |
| DELETE | `/api/inapps/{id}` | None | Delete an in-app notification |

---

### User Preference Endpoints — `/api/preferences`

| Method | URL | Auth | Description |
|---|---|---|---|
| POST | `/api/preferences` | JWT | Save or update notification preferences |
| GET | `/api/preferences` | JWT | Get preferences for the logged-in user |

#### POST /api/preferences — Request Body

```json
{
  "studentFeedback": true,
  "liveClassReminder": true,
  "payoutUpdate": false,
  "streakUpdate": true,
  "newEnrollment": true
}
```

If a preference is `false`, notifications of that type will be blocked for that user.

---

## 9. Data Models

### Notification (DynamoDB table: `notifications`)

| Field | Type | Description |
|---|---|---|
| `notificationId` | String (PK) | UUID, auto-generated |
| `userId` | String (GSI) | Extracted from JWT |
| `title` | String | Notification title |
| `description` | String | Notification body |
| `type` | NotificationType | Enum — type of notification |
| `channel` | String | EMAIL / SMS / PUSH / IN_APP |
| `email` | String | Target email (for EMAIL channel) |
| `phoneNumber` | String | Target phone (for SMS channel) |
| `deviceToken` | String | Target device (for PUSH channel) |
| `isRead` | Boolean | Default: false |
| `isScheduled` | Boolean | Default: false |
| `scheduledTime` | Long | Epoch ms for scheduled delivery |
| `createdAt` | Long | Epoch ms, auto-set |
| `status` | String | PENDING / SENT / FAILED |
| `retryCount` | Integer | Default: 0, max: 3 |
| `redirectUrl` | String | Deep link URL |

### InAppNotification (DynamoDB table: `inapp_notifications`)

| Field | Type | Description |
|---|---|---|
| `id` | String (PK) | UUID, auto-generated |
| `userId` | String (GSI) | Owner of the notification |
| `title` | String | Notification title |
| `description` | String | Notification body |
| `type` | NotificationType | Enum |
| `isRead` | Boolean | Default: false |
| `createdAt` | Long | Epoch ms |
| `redirectUrl` | String | Deep link URL |

### UserPreference (DynamoDB table: `user_preferences`)

| Field | Type | Description |
|---|---|---|
| `userId` | String (PK) | User identifier |
| `studentFeedback` | Boolean | Allow FEEDBACK_ALERT |
| `liveClassReminder` | Boolean | Allow SESSION_REMINDER |
| `payoutUpdate` | Boolean | Allow PAYOUT_UPDATE |
| `streakUpdate` | Boolean | Allow STREAK_ALERT |
| `newEnrollment` | Boolean | Allow NEW_ENROLLMENT |

---

## 10. Scheduler — Auto Retry

`NotificationScheduler` runs every 10 seconds (configurable via `notification.scheduler.interval`).

Flow:
1. Scans all notifications from DynamoDB
2. Filters those with `status = PENDING`
3. Dispatches each via `ChannelDispatcherService`
4. On success → sets `status = SENT`
5. On failure → increments `retryCount`
   - If `retryCount < maxRetry (3)` → keeps status as `PENDING` for next run
   - If `retryCount >= 3` → sets `status = FAILED` permanently

---

## 11. Exception Handling

### Custom Exceptions

| Exception | When thrown |
|---|---|
| `InvalidAuthorizationException` | Missing or malformed JWT header |
| `NotificationNotFoundException` | Notification ID not found in DB |
| `NotificationValidationException` | Missing required fields (userId, type, channel) |
| `InvalidEmailException` | Email missing for EMAIL channel |
| `InvalidPhoneNumberException` | Phone missing for SMS channel |
| `InvalidDeviceTokenException` | Device token missing for PUSH channel |
| `InvalidUserIdException` | UserId missing for IN_APP channel |
| `InvalidMessageException` | Message body is empty |
| `EmailSendingException` | JavaMailSender threw an error |
| `SmsSendingException` | Twilio API threw an error |
| `InAppNotificationException` | DynamoDB save failed for in-app |

### GlobalExceptionHandler

`@RestControllerAdvice` catches `MethodArgumentNotValidException` (from `@Valid` on request bodies) and returns a structured JSON error response:

```json
{
  "title": "must not be blank",
  "type": "must not be null"
}
```

---

## 12. Running Locally

### Prerequisites

- Java 17
- Maven
- Local DynamoDB running on port 8000

Start local DynamoDB (using Docker):
```bash
docker run -p 8000:8000 amazon/dynamodb-local
```

### Run the app

```bash
mvn clean install
mvn spring-boot:run
```

Or set env vars first if you want real email/SMS:
```bash
export EMAIL_USER=your@gmail.com
export EMAIL_PASS=your-app-password
export TWILIO_ACCOUNT_SID=ACxxxxxxx
export TWILIO_AUTH_TOKEN=xxxxxxx
export TWILIO_PHONE_NUMBER=+1xxxxxxxxxx
mvn spring-boot:run
```

### DynamoDB Tables Required

Create these tables in your local DynamoDB before running:

**notifications**
- Partition key: `notificationId` (String)
- GSI: `userId-index` on `userId` (String)

**inapp_notifications**
- Partition key: `id` (String)
- GSI: `userId-index` on `userId` (String)

**user_preferences**
- Partition key: `userId` (String)
