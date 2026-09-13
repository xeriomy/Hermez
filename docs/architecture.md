# Hermes Android Architecture

This document describes the architecture of the Hermes Android client application.

## Overview

The Hermes Android client is a native Android application built with modern Android development best practices. It communicates with the Hermes WebUI backend via REST APIs and Server-Sent Events (SSE) for real-time streaming.

## Architecture Diagram

```
+------------------+     +-------------------+     +------------------+
|     UI Layer     | --> |   ViewModel Layer | --> |  Repository Layer |
+------------------+     +-------------------+     +------------------+
        |                        |                        |
        v                        v                        v
+------------------+     +-------------------+     +------------------+
|   Compose UI    |     |  State Management |     |   Network Layer  |
|  (Screens)      |     |  (StateFlow)      |     |  (Retrofit/OkHttp)|
+------------------+     +-------------------+     +------------------+
        ^                        ^                        |
        |                        |                        v
+------------------+     +-------------------+     +------------------+
|   Components    |     |    Business       |     |    HermesApi     |
|  (Reusable)     |     |    Logic          |     |    Interface    |
+------------------+     +-------------------+     +------------------+
                                                 |
                                                 v
                                          +------------------+
                                          |   SSE Client     |
                                          |  (OkHttp SSE)    |
                                          +------------------+
```

## Layers

### 1. UI Layer

**Purpose**: Display data and handle user interactions

**Components**:
- **Screens**: Full-screen composables (ServerConnectionScreen, SessionsScreen, ChatScreen)
- **Components**: Reusable UI components (SessionCard, MessageBubble, StreamingMessage)
- **Theme**: Material 3 theming (Theme.kt, Type.kt)
- **Navigation**: AppNavigation with Jetpack Navigation Compose

**Technology**:
- Jetpack Compose
- Material 3
- Jetpack Navigation Compose

### 2. ViewModel Layer

**Purpose**: Manage UI-related state and business logic

**ViewModels**:
- `ServerViewModel`: Manages server connection state
- `SessionViewModel`: Manages session listing and creation
- `ChatViewModel`: Manages chat operations and streaming
- `AuthViewModel`: Manages authentication (future)

**Technology**:
- AndroidX ViewModel
- Kotlin StateFlow
- Coroutines

### 3. Repository Layer

**Purpose**: Manage data operations and business logic

**Repositories**:
- `AuthRepository`: Handles authentication and server connection
- `SessionRepository`: Handles session data operations
- `ChatRepository`: Handles chat operations and SSE streaming

**Responsibilities**:
- API calls via HermesApi
- Data transformation
- State management
- Caching
- Error handling

**Technology**:
- Kotlin Coroutines
- StateFlow
- Retrofit

### 4. Network Layer

**Purpose**: Handle all network communication with Hermes WebUI

**Components**:
- `HermesApi`: Retrofit interface defining all API endpoints
- `ApiClient`: Singleton for managing API client instances and cookies
- `SseClient`: Handles Server-Sent Events for real-time streaming

**Features**:
- REST API calls
- SSE streaming
- Cookie management
- Authentication
- Error handling
- Logging (with sensitive data redaction)

**Technology**:
- Retrofit 2
- OkHttp 3
- OkHttp SSE
- Gson (JSON serialization)

## Data Flow

### REST API Flow

1. **UI Layer** -> ViewModel: User triggers action (e.g., load sessions)
2. **ViewModel** -> Repository: Calls repository method
3. **Repository** -> Network Layer: Calls HermesApi method
4. **Network Layer** -> Server: Makes HTTP request via Retrofit/OkHttp
5. **Server** -> Network Layer: Returns HTTP response
6. **Network Layer** -> Repository: Parses response, handles errors
7. **Repository** -> ViewModel: Updates StateFlow with result
8. **ViewModel** -> UI Layer: UI observes state changes and updates

### SSE Streaming Flow

1. **UI Layer** -> ViewModel: User starts chat
2. **ViewModel** -> Repository: Calls startChat
3. **Repository** -> Network Layer: Calls HermesApi.startChat
4. **Repository** -> SseClient: Starts SSE connection with streamId
5. **SseClient** -> Server: Opens SSE connection
6. **Server** -> SseClient: Sends stream events
7. **SseClient** -> Repository: Emits events via Flow
8. **Repository** -> ViewModel: Processes events, updates state
9. **ViewModel** -> UI Layer: UI observes and displays streaming content

## State Management

The app uses **StateFlow** for state management throughout all layers:

### UI State
- Screen-specific state (loading, error, success)
- Form input state
- Navigation state

### ViewModel State
- UI-related state exposed to Compose
- Transforms repository state for UI consumption
- Handles UI-specific logic

### Repository State
- Data operation state (loading, error, success)
- Cached data
- Business logic state

## Dependency Injection

The app uses **ViewModelProvider.Factory** for dependency injection:

- ViewModels are created with factories that inject dependencies
- Repositories are created within ViewModels
- API clients are created in ServerViewModel and passed down

## Error Handling

### Network Errors
- HTTP errors (4xx, 5xx) are caught and converted to meaningful error messages
- Connection errors are handled gracefully with retry options
- Timeouts are configured appropriately

### SSE Errors
- Connection drops trigger automatic reconnection with exponential backoff
- Parse errors are logged and handled gracefully
- Stream errors are surfaced to the UI

### UI Errors
- Empty state handling
- Error state display
- Loading state indicators
- Retry mechanisms

## Authentication

### Current Implementation
- Cookie-based authentication via `hermes_session` cookie
- Automatic cookie handling via OkHttp CookieJar
- Authentication status checking via `/api/auth/status`
- Password login via `/api/auth/login`

### Future Enhancements
- Persistent session storage
- Biometric authentication
- OAuth support

## Navigation

The app uses **Jetpack Navigation Compose** with a simple hierarchy:

```
ServerConnectionScreen
    -> SessionsScreen
        -> ChatScreen/{sessionId}
```

Navigation is handled in `AppNavigation.kt` with:
- NavController for managing back stack
- Composable destinations for each screen
- Argument passing via route parameters

## Project Structure

```
hermes-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/hermes/android/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/          # Compose screens
│   │   │   │   │   ├── components/       # Reusable UI components
│   │   │   │   │   ├── theme/            # Theming
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   └── HermesApp.kt
│   │   │   │   ├── viewmodel/           # ViewModels
│   │   │   │   ├── repository/         # Repositories
│   │   │   │   ├── network/            # Network layer
│   │   │   │   │   ├── HermesApi.kt
│   │   │   │   │   ├── ApiClient.kt
│   │   │   │   │   └── SseClient.kt
│   │   │   │   └── model/               # Data models
│   │   │   └── res/                    # Resources
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── docs/
    ├── architecture.md
    ├── hermes-webui-api.md
    └── build.md
```

## Design Principles

1. **Separation of Concerns**: Clear separation between UI, business logic, and data
2. **Unidirectional Data Flow**: State flows from Repository -> ViewModel -> UI
3. **Reactive Programming**: StateFlow for observable state changes
4. **Immutability**: Data classes are immutable where possible
5. **Testability**: Dependencies are injectable, logic is testable
6. **Error Handling**: Graceful error handling at all levels
7. **Performance**: Efficient data loading, caching, and pagination

## Key Features

### Implemented
- ✅ Server connection management
- ✅ Session listing and creation
- ✅ Basic chat with message sending
- ✅ SSE streaming for real-time responses
- ✅ Session selection and navigation
- ✅ Error handling and retry mechanisms
- ✅ Loading states
- ✅ Material 3 theming
- ✅ Responsive UI with Jetpack Compose

### Not Yet Implemented (Phase 1)
- ❌ Authentication UI (login dialog)
- ❌ Session-level SSE (separate from chat stream)
- ❌ Stop/cancel button functionality
- ❌ Workspace API integration
- ❌ Projects API integration

### Future Phases
- VPS/SSH/Docker integration (explicitly excluded from Phase 1)
- Push notifications
- Workspace file editor
- Skills UI
- Memory UI
- Tasks UI
- Attachments
- Model management UI
