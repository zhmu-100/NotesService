# NotesService

## About

Default configuration for this microservice in env:

```
PORT=8001

DB_MODE=LOCAL        # LOCAL or gateway
DB_HOST=localhost
DB_PORT=5432

```

Default port for this service is 8081. [App.kt](app/src/main/kotlin/org/example/App.kt)/

### Routes:

Notes Routes:

- GET notebook/notes/{id} - Get a note by ID
- GET notebook/notes?user_id={userId}&page={page No}&page_size={page size} - List all notes for a specific user
- POST notebook/notes - Create a new note
- PUT notebook/notes/{id} - Update a note
- DELETE notebook/notes/{id}?user_id={userId} - Delete a note

Notifications Routes:

- GET notebook/notifications/{id} - Get a notification by ID
- GET notebook/notifications?user_id={userId}&page={page No}&page_size={page size} - List all notifications for a
  specific user
- POST notebook/notifications - Create a new notification
- POST notebook/notifications/{id}/action - Perform an action on a notification

Types of actions for Notifications should be referenced in
the [NotificationActionEnum](app/src/main/kotlin/org/example/model/NotificationActionEnum.kt)
and [NotificationSnoozeEnum](app/src/main/kotlin/org/example/model/NotificationSnoozeEnum.kt) files.

### Notes query examples

**Get by ID**

URL (body is empty):

```
GET http://localhost:8001/notebook/notes/3c236bf1-0a1c-457c-97eb-bf4a9c8ce346
```

Response:

```json
{
  "id": "3c236bf1-0a1c-457c-97eb-bf4a9c8ce346",
  "userId": "user123",
  "title": "Первая заметка",
  "content": "Содержимое первой заметки",
  "date": "2025-04-06T14:20:06.417571800"
}
```

**Get list**

URL (body is empty):

```
GET http://localhost:8001/notebook/notes?user_id=user123&page=1&page_size=10
```

Response:

```json
[
  {
    "id": "970f57c0-9156-4c48-8324-4811cba4fe8d",
    "userId": "user123",
    "title": "Xasd заметка",
    "content": "Содержимое первой заметки",
    "date": "2025-04-06T14:42:27.169306700"
  },
  {
    "id": "3c236bf1-0a1c-457c-97eb-bf4a9c8ce346",
    "userId": "user123",
    "title": "Первая заметка",
    "content": "Содержимое первой заметки",
    "date": "2025-04-06T14:20:06.417571800"
  }
]
```

**Create a note**

URL:

```
POST http://localhost:8001/notebook/notes
```

Body:

```json
{
  "userId": "user123",
  "title": "Первая заметка",
  "content": "Содержимое первой заметки"
}
```

Response:

```json
{
  "id": "14fd594c-835c-4d8e-b1fd-a9876064a130",
  "userId": "user123",
  "title": "Первая заметка",
  "content": "Содержимое первой заметки",
  "date": "2025-04-12T11:13:53.295328700"
}
```

**Update a note**

URL:

```
PUT http://localhost:8001/notebook/notes/3c236bf1-0a1c-457c-97eb-bf4a9c8ce346
```

Body:

```json
{
  "userId": "user123",
  "title": "Вторая заметка",
  "content": "Новое содержимое заметки"
}
```

Response:

```json
{
  "id": "3c236bf1-0a1c-457c-97eb-bf4a9c8ce346",
  "userId": "user123",
  "title": "Вторая заметка",
  "content": "Новое содержимое заметки",
  "date": "2025-04-12T11:15:16.400968400"
}
```

**Delete a note**

URL:

```
DELETE http://localhost:8001/notebook/notes/3c236bf1-0a1c-457c-97eb-bf4a9c8ce346?user_id=user123
```

Response:

```
Note deleted
```

### Notifications query examples

**Get by ID**

URL (body is empty):

```
GET http://localhost:8001/notebook/notifications/bf08f435-253b-4a6f-93ec-d87104a36cf5
```

Response:

```json
{
  "id": "bf08f435-253b-4a6f-93ec-d87104a36cf5",
  "userId": "user123",
  "title": "Второе напоминание",
  "description": "Содержимое второго напоминания",
  "createDate": "2025-04-12T10:22:27.206634600",
  "notificationDate": "2025-04-16T14:20:06.417571800"
}
```

**Get list**

URL (body is empty):

```
GET http://localhost:8001/notebook/notifications?user_id=user123&page=1&page_size=10
```

Response:

```json
[
  {
    "id": "bf08f435-253b-4a6f-93ec-d87104a36cf5",
    "userId": "user123",
    "title": "Второе напоминание",
    "description": "Содержимое второго напоминания",
    "createDate": "2025-04-12T10:22:27.206634600",
    "notificationDate": "2025-04-16T14:20:06.417571800"
  },
  {
    "id": "ab049053-32c3-403a-8710-2f7fb1e65895",
    "userId": "user123",
    "title": "[DONE] Первое напоминание",
    "description": "Содержимое первго напоминания",
    "createDate": "2025-04-12T10:21:19.571632700",
    "notificationDate": "2025-04-14T14:35:06.417571800"
  }
]
```

**Create a notification**
URL:

```
POST http://localhost:8001/notebook/notifications
```

Body:

```json
{
  "userId": "user123",
  "title": "Первое напоминание",
  "description": "Содержимое первго напоминания",
  "createDate": "2025-04-12T14:20:06.417571800",
  "notificationDate": "2025-04-14T14:20:06.417571800"
}
```

Response:

```json
{
  "id": "5a3bfd91-f02e-4f20-99d5-c63eed8463b1",
  "userId": "user123",
  "title": "Первое напоминание",
  "description": "Содержимое первго напоминания",
  "createDate": "2025-04-12T11:19:03.207240200",
  "notificationDate": "2025-04-14T14:20:06.417571800"
}
```

**Preform action**
URL:

```
POST http://localhost:8001/notebook/notifications/bf08f435-253b-4a6f-93ec-d87104a36cf5/action
```

Body:

```json
{
  "id": "bf08f435-253b-4a6f-93ec-d87104a36cf5",
  "userId": "user123",
  "title": "Второе напоминание",
  "description": "Содержимое второго напоминания",
  "createDate": "2025-04-12T10:22:27.206634600",
  "notificationDate": "2025-04-16T14:35:06.417571800"
}
```

Response:

```json
{
  "id": "5a3bfd91-f02e-4f20-99d5-c63eed8463b1",
  "userId": "user123",
  "title": "Первое напоминание",
  "description": "Содержимое первго напоминания",
  "createDate": "2025-04-12T11:19:03.207240200",
  "notificationDate": "2025-04-14T14:20:06.417571800"
}
```

## Notes

Notes sections of this service has the following actions:

- Create Note (note format)
- Get Note (by providing the note ID)
- List Notes (list all notes for a specific user by providing user_id, page, and page_size)
- Update Note (if the user wants to update a note, they should provide the updated note to this endpoint)
- Delete Note (delete a specific note by providing the note ID and user_id)

Note format:

```proto
message Note {
  string id = 1;
  string user_id = 2;
  string title = 3;
  string content = 4;
  google.protobuf.Timestamp date = 5;
}
```

Actions for notes:

```proto
service NoteService {
  rpc CreateNote(CreateNoteRequest) returns (Note);
  rpc GetNote(GetNoteRequest) returns (Note);
  rpc ListNotes(ListNotesRequest) returns (ListNotesResponse);
  rpc UpdateNote(UpdateNoteRequest) returns (Note);
  rpc DeleteNote(DeleteNoteRequest) returns (google.protobuf.Empty);
}

message CreateNoteRequest {
  Note note = 1;
}

message GetNoteRequest {
  string id = 1;
}

message ListNotesRequest {
  string user_id = 1;
  int32 page = 2;
  int32 page_size = 3;
}

message ListNotesResponse {
  repeated Note notes = 1;
}

message UpdateNoteRequest {
  Note note = 1;
}

message DeleteNoteRequest {
  string id = 1;
  string user_id = 2;
}
```

## Notifications

```proto
enum NotificationAction {
  NOTIFICATION_ACTION_UNSPECIFIED = 0;
  NOTIFICATION_ACTION_COMPLETE = 1;
  NOTIFICATION_ACTION_DISMISS = 2;
  NOTIFICATION_ACTION_SNOOZE = 3;
}

enum NotificationSnooze {
  NOTIFICATION_SNOOZE_UNSPECIFIED = 0;
  NOTIFICATION_SNOOZE_5_MINUTES = 1;
  NOTIFICATION_SNOOZE_15_MINUTES = 2;
  NOTIFICATION_SNOOZE_30_MINUTES = 3;
  NOTIFICATION_SNOOZE_1_HOUR = 4;
  NOTIFICATION_SNOOZE_5_HOURS = 5;
  NOTIFICATION_SNOOZE_1_DAY = 6;
}

message Notification {
  string id = 1;
  string user_id = 2;
  string title = 3;
  string description = 4;
  google.protobuf.Timestamp create_date = 5;
  google.protobuf.Timestamp notification_date = 6;
}
```

Actions for notifications:

```proto
service NoteService {
  rpc CreateNotification(CreateNotificationRequest) returns (Notification);
  rpc GetNotification(GetNotificationRequest) returns (Notification);
  rpc ListNotifications(ListNotificationsRequest) returns (ListNotificationsResponse);
  rpc PerformNotificationAction(NotificationActionRequest) returns (Notification);
}

message CreateNotificationRequest {
  Notification notification = 1;
}

message GetNotificationRequest {
  string id = 1;
}

message ListNotificationsRequest {
  string user_id = 1;
  int32 page = 2;
  int32 page_size = 3;
}

message ListNotificationsResponse {
  repeated Notification notifications = 1;
}

message NotificationActionRequest {
  string id = 1;
  string user_id = 2;
  NotificationAction action = 3;
  optional NotificationSnooze snooze_duration = 4;
}
```

## SQL

Notes table

```sql
create table notes (
    id varchar(36) PRIMary key,
    userid varchar(255) not null,
    title text not null,
    content text not null,
    date varchar(255) not null
);
```

Notifications table

```sql
create table notifications (
    id varchar(36) PRIMary key,
    userid varchar(255) not null,
    title text not null,
    description text not null,
    create_date varchar(255) not null,
    notification_date varchar(255) not null
);
```