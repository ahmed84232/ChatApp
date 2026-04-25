# ChatApp Backend

A modern, scalable real-time chat application backend built with Spring Boot 3.5.4. ChatApp enables users to engage in one-to-one private conversations and group discussions with real-time message synchronization, participant management, and robust authentication.

## 🚀 Features

- **Real-time Messaging**: Instant message delivery using WebSocket and RabbitMQ
- **Private & Group Chats**: Support for both one-to-one and multi-user conversations
- **User Management**: Search users and manage user profiles integrated with Keycloak SSO
- **Message Status Tracking**: Track message delivery status (SENT, DELIVERED, READ)
- **Participant Management**: Add/remove users from group conversations
- **Conversation Management**: Create, delete, and manage conversations with ease
- **OAuth2 Security**: Secured with OAuth2 and JWT tokens via Keycloak
- **Caching**: Redis integration for performance optimization
- **Asynchronous Communication**: RabbitMQ message broker for scalable real-time updates
- **Data Persistence**: PostgreSQL database with JPA/Hibernate ORM
- **Pagination**: Efficient message pagination for large conversation histories

## 🛠️ Technology Stack

### Backend Framework
- **Spring Boot 3.5.4** - Modern Java web framework
- **Spring WebSocket** - Real-time bidirectional communication
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Object-relational mapping
- **Spring AMQP** - Message broker integration
- **Spring Data Redis** - Caching layer

### Infrastructure & Services
- **PostgreSQL** - Primary relational database
- **Redis** - Caching and session store
- **RabbitMQ** - Message broker for real-time updates
- **Keycloak** - Identity and Access Management (OAuth2/OIDC)

### Development Tools
- **Java 24** - Latest Java version
- **Maven** - Build automation tool
- **Lombok** - Reduce boilerplate code
- **Docker** - Containerization

## 📋 Project Structure

```
src/main/java/com/ahmedyasser/
├── ChatApplication.java              # Main Spring Boot entry point
├── config/
│   ├── RabbitMQConfig.java          # RabbitMQ queue and exchange configuration
│   └── TrustAllSSL.java             # SSL configuration
├── controller/
│   ├── ConversationController.java  # Conversation CRUD operations
│   ├── MessageController.java       # Message retrieval
│   ├── UserController.java          # User search and bulk operations
│   ├── ParticipantController.java   # Participant management
│   └── RealtimeController.java      # WebSocket message handling
├── service/
│   ├── ConversationService.java    # Conversation business logic
│   ├── MessageService.java         # Message persistence and caching
│   ├── UserService.java            # User operations with Keycloak
│   └── ParticipantService.java     # Participant management logic
├── client/
│   └── RabbitMQProducer.java       # RabbitMQ message publishing
├── dao/
│   ├── ConversationDao.java        # Conversation data access
│   ├── MessageDao.java             # Message data access
│   ├── UserDao.java                # User data access
│   └── ConversationParticipantDao.java # Participant data access
├── entity/
│   ├── Conversation.java           # Conversation JPA entity
│   ├── Message.java                # Message JPA entity
│   ├── User.java                   # User JPA entity
│   └── ConversationParticipant.java # Participant JPA entity
├── dto/
│   ├── ConversationDto.java        # Conversation data transfer object
│   ├── MessageDto.java             # Message data transfer object
│   ├── UserDto.java                # User data transfer object
│   └── ActionDto.java              # Real-time action wrapper
├── util/
│   └── AuthUtil.java               # Authentication and authorization utilities
└── enums/
    └── MessageStatus.java          # Message status enumeration
```

## 🔌 API Endpoints

### Conversations
```
GET    /conversation/user/{userId}       - Get all conversations for a user
POST   /conversation                     - Create a new conversation
DELETE /conversation/{conversationId}    - Delete a conversation
```

### Messages
```
GET /message/conversation/{conversationId}?page=0  - Get messages with pagination
```

### Users
```
GET    /user/search?query={searchTerm}        - Search users by username
POST   /user/bulk                             - Get multiple users by IDs
```

### Real-time (WebSocket)
```
Message Mapping: /action
- sendMessage              - Send a new message
- updateMessage            - Edit existing message
- deleteMessage            - Delete a message
- typingIndicator          - Send typing status
- MessageStatus            - Update message status
- deleteConversation       - Delete conversation in real-time
- deleteParticipant        - Remove participant from group
- addParticipant           - Add user to group chat
```

## 🏗️ Architecture Overview

### Real-time Communication Flow

```
Client (WebSocket)
    ↓
RealtimeController (/action)
    ↓
MessageService / ConversationService / ParticipantService
    ↓
RabbitMQProducer
    ↓
RabbitMQ Exchange (exchage.main)
    ↓
User-specific Queues (queue.{pod-name})
    ↓
Client (WebSocket) - receives update
```

### Data Flow

1. **Message Sending**: 
   - Client sends action via WebSocket
   - `RealtimeController` receives and processes action
   - `MessageService` persists message to PostgreSQL
   - Cache is invalidated in Redis
   - Message is published to RabbitMQ for all conversation participants
   - WebSocket sends real-time update to connected clients

2. **Message Retrieval**:
   - Client requests messages with pagination
   - `MessageController` queries `MessageService`
   - Results are cached in Redis for performance
   - `UserService` fetches sender information from Keycloak

## 🔐 Security & Authentication

- **OAuth2 Resource Server**: Secured with Keycloak OIDC
- **JWT Token Validation**: Extracts user ID and username from JWT claims
- **Authorization**: Method-level security with `@PreAuthorize` annotations
- **Role-based Access Control**: Via `AuthUtil` utility class
- **Conversation Participant Validation**: Ensures users can only access conversations they're part of

### Authentication Utilities (`AuthUtil`)
- `currentUserId()` - Extract authenticated user ID from JWT
- `currentUser()` - Get full user details from security context
- `isConversationParticipant()` - Verify user participation
- `isConversationOwner()` - Check conversation ownership
- `isMessageOwner()` - Verify message authorship

## 🗄️ Database Schema

### Conversations Table
- `id` (UUID, Primary Key)
- `name` (String) - Conversation name
- `is_group_chat` (Boolean) - Whether it's a group or private chat
- `owner` (UUID) - Group owner's user ID
- `created_at` (Timestamp) - Creation time
- `updated_at` (Timestamp) - Last update time

### Messages Table
- `id` (UUID, Primary Key)
- `conversation_id` (Foreign Key to Conversations)
- `sender_id` (UUID) - Who sent the message
- `username` (String) - Sender's username
- `message_text` (String) - Message content
- `status` (Enum: SENT, DELIVERED, READ) - Message status
- `sent_at` (Timestamp) - Message creation time
- `updated_at` (Timestamp) - Last update time

### Conversation Participants Table
- `id` (UUID, Primary Key)
- `conversation_id` (Foreign Key to Conversations)
- `user_id` (UUID) - Participant user ID
- `joined_at` (Timestamp) - When user joined
- `last_read_time` (Timestamp) - Last message read time (optional)

### Users Table
- `id` (UUID, Primary Key)
- `username` (String) - Username
- `first_name` (String) - User's first name
- `last_name` (String) - User's last name
- `email` (String) - Email address

## 📦 Dependencies

Key dependencies in `pom.xml`:
- `spring-boot-starter-web` - Web framework
- `spring-boot-starter-websocket` - WebSocket support
- `spring-boot-starter-security` - Security framework
- `spring-boot-starter-oauth2-resource-server` - OAuth2 authentication
- `spring-boot-starter-data-jpa` - Database ORM
- `spring-boot-starter-data-redis` - Caching
- `spring-boot-starter-amqp` - Message broker
- `postgresql` - Database driver
- `lombok` - Code generation

## 🚀 Getting Started

### Prerequisites
- Java 24
- PostgreSQL 12+
- Redis
- RabbitMQ
- Keycloak (for OAuth2)
- Maven 3.6+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/ahmed84232/ChatApp.git
   cd ChatApp
   ```

2. **Configure environment variables**
   
   Create/update `src/main/resources/application.properties`:
   ```properties
   # Database Configuration
   DB_URL=jdbc:postgresql://localhost:5432/chatapp
   DB_USER=postgres
   DB_PASS=your_password
   
   # Redis Configuration
   REDIS_HOST=localhost
   REDIS_PORT=6379
   
   # RabbitMQ Configuration
   RABBITMQ_HOST=localhost
   RABBITMQ_PORT=5672
   RABBITMQ_USERNAME=guest
   RABBITMQ_PASSWORD=guest
   
   # Keycloak Configuration
   KC_HOST=https://your-keycloak-host
   KC_REALM_NAME=your-realm
   KC_CLIENT_ID=ChatApp
   KC_CLIENT_SECRET=your-client-secret
   KC_INTERNAL_CLIENT_ID=ChatAppInternal
   KC_INTERNAL_CLIENT_SECRET=your-internal-secret
   
   # Frontend Configuration
   FRONTEND_HOST=http://localhost:3000
   ```

3. **Build the project**
   ```bash
   mvn clean package
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Or using Docker:
   ```bash
   docker build -t chatapp-backend .
   docker run -p 8080:8080 chatapp-backend
   ```

### Running with Docker

The included `Dockerfile` packages the application with Java 26 Oracle Linux runtime and enables debug mode on port 5005:

```bash
docker build -t chatapp .
docker run -p 8080:8080 -p 5005:5005 chatapp
```

## 📡 WebSocket Usage Example

### Connect to WebSocket
```javascript
const stompClient = new StompClient('ws://localhost:8080/ws');
stompClient.connect({}, function() {
  // Subscribe to personal message queue
  stompClient.subscribe('/user/queue/messages', function(message) {
    console.log('Received:', message);
  });
});
```

### Send a Message (Real-time)
```javascript
const messageAction = {
  action: 'sendMessage',
  object: {
    conversationId: '550e8400-e29b-41d4-a716-446655440000',
    messageText: 'Hello, World!',
    senderId: '550e8400-e29b-41d4-a716-446655440001'
  }
};

stompClient.send('/app/action', {}, JSON.stringify(messageAction));
```

## 🔄 Message Lifecycle

1. **Client sends message** → WebSocket `/app/action` endpoint
2. **RealtimeController** processes the action
3. **MessageService** validates and persists message
4. **Cache invalidation** - Redis cache for conversation is cleared
5. **RabbitMQ publishing** - Message published to all participants' queues
6. **Real-time delivery** - Connected clients receive updates via WebSocket

## 🎯 Caching Strategy

- **Cache name**: `mainCache`
- **Cache keys**: `{conversationId}:{pageNumber}`
- **Cache eviction**: Triggered on message send, update, and delete
- **Provider**: Redis with Spring Cache abstractions

## 🔧 Configuration Details

### RabbitMQ Configuration
- **Main Exchange**: `exchage.main` (FanoutExchange)
- **Queue pattern**: `queue.{pod-name}` (per instance queues)
- **Message converter**: Jackson2JsonMessageConverter
- **Binding**: All participant queues bound to main exchange

### Database Configuration
- **Schema**: `chatapp`
- **Connection pooling**: Automatic via Spring Data JPA
- **Transactions**: `@Transactional` for data consistency

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Ahmed Yasser** - [GitHub Profile](https://github.com/ahmed84232)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Support

For issues, questions, or suggestions, please open an issue on GitHub or contact the maintainer.

---

**Last Updated**: April 25, 2026  
**Repository**: [ahmed84232/ChatApp](https://github.com/ahmed84232/ChatApp)
