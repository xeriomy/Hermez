# Hermes WebUI API Documentation

This document describes the API contracts discovered from the Hermes WebUI backend for Android client integration.

## Base URL
The Hermes WebUI server typically runs on port 8787. The base URL is configurable.

Example: `http://192.168.1.100:8788`

## Authentication

### Authentication Status
- **Endpoint**: `GET /api/auth/status`
- **Description**: Check if authentication is enabled and current session status
- **Response**:
  ```json
  {
    "auth_enabled": true/false,
    "logged_in": true/false,
    "oidc_enabled": true/false,
    "password_auth_enabled": true/false,
    "passwordless_enabled": true/false,
    "passkeys_enabled": true/false,
    "passkeys_count": 0,
    "passkey_feature_flag": true/false,
    "auth_type": "trusted" | null,
    "user": "username" | null,
    "bound_profile": "profile_name" | null
  }
  ```

### Login
- **Endpoint**: `POST /api/auth/login`
- **Request Body**:
  ```json
  {
    "password": "user_password"
  }
  ```
- **Response**: Sets `hermes_session` cookie on success
- **Status Codes**:
  - 200: Success
  - 401: Invalid password
  - 404: Auth not configured

### Logout
- **Endpoint**: `POST /api/auth/logout`
- **Description**: Invalidates current session
- **Response**: Clears session cookie

## Sessions API

### List Sessions
- **Endpoint**: `GET /api/sessions`
- **Query Parameters**:
  - `include_archived`: boolean (default: false)
  - `exclude_hidden`: boolean (default: false)
  - `archived_limit`: number (max 2000)
  - `archived_offset`: number (max 200000)
  - `sidebar_source`: "webui" | "cli" | null
- **Response**:
  ```json
  {
    "sessions": [
      {
        "session_id": "uuid",
        "title": "Session title",
        "model": "model_name",
        "profile": "profile_name",
        "created_at": timestamp,
        "updated_at": timestamp,
        "message_count": 0,
        "input_tokens": 0,
        "output_tokens": 0,
        "estimated_cost": 0.0,
        "source": "webui" | "cli" | "messaging" | null,
        "source_tag": "tag" | null,
        "source_label": "label" | null,
        "pinned": true/false,
        "archived": true/false,
        "hidden": true/false,
        "active_stream_id": "stream_id" | null,
        "pending_user_message": "message" | null,
        "llm_title_generated": true/false,
        "manual_title": true/false
      }
    ],
    "projects": [],
    "active_profile": "profile_name",
    "all_profiles": true/false,
    "other_profile_count": 0
  }
  ```

### Get Session
- **Endpoint**: `GET /api/session`
- **Query Parameters**:
  - `session_id`: string (required)
  - `messages`: "0" | "1" (default: "1", whether to load messages)
  - `msg_limit`: number (limit messages returned)
  - `msg_before`: number (0-based index for pagination)
  - `resolve_model`: "0" | "1" (default: "1" if messages=1)
- **Response**: Full session object with messages array
  ```json
  {
    "session_id": "uuid",
    "title": "Session title",
    "model": "model_name",
    "profile": "profile_name",
    "messages": [
      {
        "role": "user" | "assistant" | "system",
        "content": "message content",
        "timestamp": timestamp,
        "token_count": 0,
        "cost": 0.0,
        "message_id": "uuid",
        "stream_id": "stream_id" | null,
        "seq": 0,
        "event_id": "event_id" | null,
        "hidden": true/false
      }
    ],
    "context_messages": [],
    "active_stream_id": "stream_id" | null,
    "pending_user_message": "message" | null,
    "created_at": timestamp,
    "updated_at": timestamp,
    "input_tokens": 0,
    "output_tokens": 0,
    "estimated_cost": 0.0,
    "cache_read_tokens": 0,
    "cache_write_tokens": 0
  }
  ```

### Create Session
- **Endpoint**: `POST /api/session/new`
- **Request Body**:
  ```json
  {
    "title": "New session",
    "profile": "profile_name" | null
  }
  ```
- **Response**:
  ```json
  {
    "session_id": "uuid",
    "title": "New session",
    "profile": "profile_name",
    "created_at": timestamp
  }
  ```

### Session Status
- **Endpoint**: `GET /api/session/status`
- **Query Parameters**:
  - `session_id`: string (required)
- **Response**:
  ```json
  {
    "session_id": "uuid",
    "active_stream_id": "stream_id" | null,
    "pending_user_message": "message" | null,
    "state": "idle" | "processing" | "waiting"
  }
  ```

### Session Usage
- **Endpoint**: `GET /api/session/usage`
- **Query Parameters**:
  - `session_id`: string (required)
- **Response**: Token usage statistics

## Chat API

### Start Chat
- **Endpoint**: `POST /api/chat/start`
- **Request Body**:
  ```json
  {
    "session_id": "uuid" (required),
    "message": "user message" (required),
    "profile": "profile_name" | null,
    "model": "model_name" | null,
    "attachments": [] | null,
    "regenerate": true/false (default: false),
    "regeneration_revision": "revision_id" | null
  }
  ```
- **Response**:
  ```json
  {
    "status": "ok",
    "stream_id": "stream_id",
    "session_id": "uuid",
    "message_id": "uuid",
    "seq": 0
  }
  ```
- **Status Codes**:
  - 200: Success
  - 400: Invalid request (missing fields)
  - 404: Session not found
  - 409: Conflict (regeneration not supported, etc.)

### Chat Stream Status
- **Endpoint**: `GET /api/chat/stream/status`
- **Query Parameters**:
  - `stream_id`: string (required)
- **Response**:
  ```json
  {
    "status": "running" | "cancelled" | "completed" | "error",
    "stream_id": "stream_id",
    "session_id": "uuid",
    "cancelled": true/false
  }
  ```

### Cancel Stream
- **Endpoint**: `POST /api/chat/stream/cancel`
- **Request Body**:
  ```json
  {
    "stream_id": "stream_id" (required)
  }
  ```
- **Response**:
  ```json
  {
    "ok": true,
    "cancelled": true/false,
    "stream_id": "stream_id"
  }
  ```

## Streaming API (SSE)

### Chat Stream (Per-Turn)
- **Endpoint**: `GET /api/chat/stream?stream_id=<id>`
- **SSE Events**:
  - `message` - Token/content streaming
    ```json
    {
      "type": "message",
      "text": "token text",
      "stream_id": "stream_id",
      "session_id": "uuid",
      "message_id": "uuid",
      "seq": 0,
      "event_id": "event_id",
      "done": false
    }
    ```
  - `tool_call` - Tool call event
    ```json
    {
      "type": "tool_call",
      "tool_name": "tool_name",
      "arguments": {...},
      "call_id": "call_id",
      "stream_id": "stream_id"
    }
    ```
  - `tool_result` - Tool result
    ```json
    {
      "type": "tool_result",
      "result": "tool output",
      "call_id": "call_id",
      "stream_id": "stream_id"
    }
    ```
  - `approval` - Approval request
    ```json
    {
      "type": "approval",
      "prompt": "approval message",
      "call_id": "call_id",
      "action": "allow" | "deny" | "always" | "session"
    }
    ```
  - `done` - Stream completion
    ```json
    {
      "type": "done",
      "stream_id": "stream_id",
      "session_id": "uuid",
      "message_id": "uuid"
    }
    ```
  - `stream_end` - Stream ended
    ```json
    {
      "type": "stream_end",
      "stream_id": "stream_id",
      "session_id": "uuid"
    }
    ```
  - `error` - Stream error
    ```json
    {
      "type": "error",
      "error": "error message",
      "stream_id": "stream_id"
    }
    ```
- **Resume Parameters**:
  - `after_event_id`: string - Resume after this event ID
  - `after_seq`: number - Resume after this sequence number
  - `Last-Event-ID`: HTTP header - Fallback resume cursor

### Session Stream (Per-Session)
- **Endpoint**: `GET /api/session/stream?session_id=<id>`
- **SSE Events**:
  - `initial` - Connection established
    ```json
    {
      "session_id": "uuid"
    }
    ```
  - `server_turn_started` - Server-initiated turn started
    ```json
    {
      "type": "server_turn_started",
      "stream_id": "stream_id",
      "session_id": "uuid",
      "recovered": true/false
    }
    ```
  - `bg_task_complete` - Background task completed
    ```json
    {
      "type": "bg_task_complete",
      "session_id": "uuid",
      "task_id": "task_id",
      "status": "completed" | "failed",
      "result": {...}
    }
    ```
  - `session_snapshot` - Session state snapshot
    ```json
    {
      "type": "session_snapshot",
      "session": {...},
      "active_stream_id": "stream_id" | null
    }
    ```
  - `session_updated` - Session metadata updated
    ```json
    {
      "type": "session_updated",
      "session_id": "uuid",
      "title": "new title",
      "updated_at": timestamp
    }
    ```
- **Query Parameters**:
  - `known_count`: number - Last known message count for self-healing

## Workspace API

### List Workspaces
- **Endpoint**: `GET /api/workspaces`
- **Response**:
  ```json
  {
    "workspaces": [
      {
        "path": "/path/to/workspace",
        "name": "workspace_name",
        "last": true/false
      }
    ],
    "last": "/path/to/last/workspace",
    "terminal_remote_backend": true/false
  }
  ```

### Workspace Suggestions
- **Endpoint**: `GET /api/workspaces/suggest`
- **Query Parameters**:
  - `prefix`: string - Path prefix for suggestions
- **Response**:
  ```json
  {
    "suggestions": ["path1", "path2"],
    "prefix": "prefix"
  }
  ```

## Projects API

### List Projects
- **Endpoint**: `GET /api/projects`
- **Query Parameters**:
  - `all_profiles`: boolean (default: false, show all profiles)
- **Response**:
  ```json
  {
    "projects": [
      {
        "name": "project_name",
        "color": "#RRGGBB",
        "profile": "profile_name",
        "session_count": 0
      }
    ],
    "all_profiles": true/false,
    "active_profile": "profile_name",
    "other_profile_count": 0
  }
  ```

## Health Check

### Health Endpoint
- **Endpoint**: `GET /health`
- **Response**:
  ```json
  {
    "status": "ok",
    "version": "x.x.x",
    "hermes_version": "x.x.x",
    "uptime": 1234.56,
    "timestamp": timestamp
  }
  ```

## Error Handling

All API endpoints may return:
- 400: Bad request (invalid parameters)
- 401: Unauthorized (authentication required)
- 403: Forbidden (insufficient permissions)
- 404: Not found (resource doesn't exist)
- 409: Conflict (state conflict)
- 500: Internal server error

## SSE Connection Handling

- SSE streams use `text/event-stream` content type
- Keep-alive: Server sends `:\n\n` (heartbeat) every 30 seconds
- Reconnect: Client should automatically reconnect on disconnect
- Resume: Use `Last-Event-ID` header or query parameters for resumption
- Headers:
  - `Cache-Control: no-cache`
  - `X-Accel-Buffering: no`
  - `Connection: keep-alive`

## Public Paths (No Auth Required)

- `/health`
- `/login`
- `/api/auth/login`
- `/api/auth/status`
- `/api/auth/oidc/start`
- `/api/auth/oidc/callback`
- `/api/auth/passkey/options`
- `/api/auth/passkey/login`
- `/share`
- `/share/*`
- `/manifest.json`
- `/manifest.webmanifest`
- `/session/manifest.json`
- `/session/manifest.webmanifest`
