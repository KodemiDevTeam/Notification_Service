# Notification Service — API Documentation

Base URL: `http://localhost:8080`

All protected endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

---

## 1. Notification APIs

Base path: `/api/notifications`

---

### 1.1 Create Notification

**POST** `/api/notifications`

Creates and sends a notification through the specified channel.

**Headers:**
| Key | Value |
|---|---|
| Authorization | Bearer `<token>` |
| Content-Type | application/json |

**Request Body:**
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

**Field Reference:**

| Field | Type | Required | Description |
|---|---|---|---|
| title | String | Yes | Notification title |
| description | String | Yes | Notification body |
| type | Enum | Yes | See Notification Types below |
| channel | Enum | Yes | EMAIL / SMS / PUSH / IN_APP |
| email | String | For EMAIL | Recipient email address |
| phoneNumber | String | For SMS | Recipient phone number |
| deviceToken | String | For PUSH | FCM device token |
| isScheduled | Boolean | No | Default: false |
| scheduledTime | Long | No | Epoch milliseconds |
| redirectUrl | String | No | Deep link URL |

**Notification Types:**
- `COURSE_ALERT`
- `TRAINER_LIVE`
- `STREAK_ALERT`
- `FEEDBACK_ALERT`
- `SESSION_REMINDER`
- `BADGE_UPDATE`
- `ADMIN_BROADCAST`
- `REWARD_REMINDER`
- `PAYOUT_UPDATE`
- `NEW_ENROLLMENT`

**Response: 201 Created**
```json
{
  "notificationId": "uuid-here",
  "userId": "user-id-from-jwt",
  "title": "Live Class Starting",
  "description": "Your class starts in 10 minutes",
  "type": "SESSION_REMINDER",
  "channel": "EMAIL",
  "email": "user@example.com",
  "status": "SENT",
  "retryCount": 0,
  "isRead": false,
  "isScheduled": false,
  "createdAt": 1713245600000,
  "redirectUrl": "https://app.example.com/class/123"
}
```

**Error Responses:**
| Status | Reason |
|---|---|
| 400 | Missing required fields (title, description, type, channel) |
| 401 | Invalid or missing Authorization header |

---

### 1.2 Get All Notifications for Logged-in User

**GET** `/api/notifications`

Returns all notifications belonging to the user extracted from JWT.

**Headers:**
| Key | Value |
|---|---|
| Authorization | Bearer `<token>` |

**Response: 200 OK**
```json
[
  {
    "notificationId": "uuid-1",
    "userId": "user1",
    "title": "Live Class Starting",
    "type": "SESSION_REMINDER",
    "channel": "EMAIL",
    "status": "SENT",
    "createdAt": 1713245600000
  }
]
```

---

### 1.3 Get Notification by ID

**GET** `/api/notifications/{id}`

**Path Variable:**
| Variable | Description |
|---|---|
| id | Notification ID (UUID) |

**Response: 200 OK**
```json
{
  "notificationId": "uuid-1",
  "userId": "user1",
  "title": "Live Class Starting",
  "status": "SENT"
}
```

**Error Responses:**
| Status | Reason |
|---|---|
| 404 | Notification not found |

---

### 1.4 Update Notification

**PUT** `/api/notifications/{id}`

Updates an existing notification's fields.

**Path Variable:**
| Variable | Description |
|---|---|
| id | Notification ID (UUID) |

**Headers:**
| Key | Value |
|---|---|
| Content-Type | application/json |

**Request Body:** (only include fields you want to update)
```json
{
  "title": "Updated Title",
  "description": "Updated description",
  "status": "SENT",
  "channel": "SMS",
  "phoneNumber": "+919999999999",
  "deviceToken": "new-token"
}
```

**Response: 200 OK** — returns updated notification object

---

### 1.5 Delete Notification

**DELETE** `/api/notifications/{id}`

**Path Variable:**
| Variable | Description |
|---|---|
| id | Notification ID (UUID) |

**Response: 200 OK**
```
Notification deleted successfully
```

**Error Responses:**
| Status | Reason |
|---|---|
| 404 | Notification not found |

---

## 2. In-App Notification APIs

Base path: `/api/inapps`

---

### 2.1 Get All In-App Notifications

**GET** `/api/inapps`

Returns all in-app notifications for the logged-in user, sorted newest first.

**Headers:**
| Key | Value |
|---|---|
| Authorization | Bearer `<token>` |

**Response: 200 OK**
```json
[
  {
    "id": "uuid-1",
    "userId": "user1",
    "title": "Student Feedback",
    "description": "New 5-star review received",
    "type": "FEEDBACK_ALERT",
    "isRead": false,
    "createdAt": 1713245600000,
    "redirectUrl": "https://app.example.com/feedback"
  }
]
```

---

### 2.2 Get Unread Notifications

**GET** `/api/inapps/unread`

Returns only unread in-app notifications for the logged-in user.

**Headers:**
| Key | Value |
|---|---|
| Authorization | Bearer `<token>` |

**Response: 200 OK** — array of unread `InAppNotification` objects

---

### 2.3 Get Unread Count

**GET** `/api/inapps/unread/count`

Returns the count of unread notifications (used for badge display).

**Headers:**
| Key | Value |
|---|---|
| Authorization | Bearer `<token>` |

**Response: 200 OK**
```
3
```

---

### 2.4 Get Single In-App Notification

**GET** `/api/inapps/one/{id}`

**Path Variable:**
| Variable | Description |
|---|---|
| id | In-App Notification ID (UUID) |

**Response: 200 OK** — single `InAppNotification` object

**Error Responses:**
| Status | Reason |
|---|---|
| 404 | Notification not found |

---

### 2.5 Mark Notification as Read

**PUT** `/api/inapps/read/{id}`

Marks a specific in-app notification as read.

**Path Variable:**
| Variable | Description |
|---|---|
| id | In-App Notification ID (UUID) |

**Response: 200 OK**
```
Marked as read
```

**Error Responses:**
| Status | Reason |
|---|---|
| 404 | Notification not found |

---

### 2.6 Delete In-App Notification

**DELETE** `/api/inapps/{id}`

**Path Variable:**
| Variable | Description |
|---|---|
| id | In-App Notification ID (UUID) |

**Response: 200 OK**
```
Notification deleted
```

---

## 3. User Preference APIs

Base path: `/api/preferences`

---

### 3.1 Save or Update Preferences

**POST** `/api/preferences`

Saves or updates notification preferences for the logged-in user.

**Headers:**
| Key | Value |
|---|---|
| Authorization | Bearer `<token>` |
| Content-Type | application/json |

**Request Body:**
```json
{
  "studentFeedback": true,
  "liveClassReminder": true,
  "payoutUpdate": false,
  "streakUpdate": true,
  "newEnrollment": true
}
```

**Field Reference:**
| Field | Type | Controls |
|---|---|---|
| studentFeedback | Boolean | FEEDBACK_ALERT notifications |
| liveClassReminder | Boolean | SESSION_REMINDER notifications |
| payoutUpdate | Boolean | PAYOUT_UPDATE notifications |
| streakUpdate | Boolean | STREAK_ALERT notifications |
| newEnrollment | Boolean | NEW_ENROLLMENT notifications |

**Response: 200 OK**
```json
{
  "userId": "user1",
  "studentFeedback": true,
  "liveClassReminder": true,
  "payoutUpdate": false,
  "streakUpdate": true,
  "newEnrollment": true
}
```

---

### 3.2 Get User Preferences

**GET** `/api/preferences`

Returns the notification preferences for the logged-in user.

**Headers:**
| Key | Value |
|---|---|
| Authorization | Bearer `<token>` |

**Response: 200 OK**
```json
{
  "userId": "user1",
  "studentFeedback": true,
  "liveClassReminder": true,
  "payoutUpdate": false,
  "streakUpdate": true,
  "newEnrollment": true
}
```

---

## 4. Error Response Format

Validation errors return a map of field names to error messages:

```json
{
  "title": "must not be blank",
  "type": "must not be null",
  "channel": "must not be blank"
}
```

---

## 5. Quick Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/notifications | Yes | Create & send notification |
| GET | /api/notifications | Yes | Get all user notifications |
| GET | /api/notifications/{id} | No | Get notification by ID |
| PUT | /api/notifications/{id} | No | Update notification |
| DELETE | /api/notifications/{id} | No | Delete notification |
| GET | /api/inapps | Yes | Get all in-app notifications |
| GET | /api/inapps/unread | Yes | Get unread notifications |
| GET | /api/inapps/unread/count | Yes | Get unread count |
| GET | /api/inapps/one/{id} | No | Get single in-app notification |
| PUT | /api/inapps/read/{id} | No | Mark as read |
| DELETE | /api/inapps/{id} | No | Delete in-app notification |
| POST | /api/preferences | Yes | Save/update preferences |
| GET | /api/preferences | Yes | Get preferences |
