# JForum Spring Boot

This is a modernized version of the JForum2 forum application, rebuilt with Spring Boot, JPA, and modern Java practices.

## Features

1. **User Management**
   - User registration and authentication with JWT
   - Profile management and preferences
   - User groups and permissions
   - Password reset and email verification

2. **Forum Structure**
   - Category and forum management
   - Topic creation and management
   - Hierarchical permissions and access control

3. **Post Management**
   - Post creation, editing, and deletion
   - Reply threading and post history
   - Rich text content support
   - Post moderation features

4. **Private Messaging**
   - Send and receive private messages
   - Message threads and organization
   - Notification system

5. **Search and Navigation**
   - Full-text search across posts and topics
   - Advanced search filters
   - Pagination and sorting

6. **Administration Features**
   - Admin dashboard and controls
   - User and content moderation
   - System configuration and monitoring

## Technology Stack

- **Backend**: Spring Boot 3.x, Java 17
- **Security**: Spring Security, JWT
- **Database**: JPA/Hibernate with support for H2, MySQL, and PostgreSQL
- **Database Migration**: Flyway
- **API Documentation**: Springdoc OpenAPI (Swagger)
- **Testing**: JUnit 5, Spring Test, Testcontainers

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- MySQL or PostgreSQL (optional, can use H2 for development)

### Running the Application

1. Clone the repository
2. Configure the database in `application.yml` or use environment variables
3. Run the application:

```bash
mvn spring-boot:run
```

The application will be available at `http://localhost:8080/api`

### API Documentation

Swagger UI is available at `http://localhost:8080/api/swagger-ui.html`

### Default Credentials

- Username: `admin`
- Password: `admin`

## Configuration

The application can be configured using the following files:

- `application.yml` - Main configuration file
- `application-dev.yml` - Development environment configuration
- `application-prod.yml` - Production environment configuration

## Database Migration

The application uses Flyway for database migration. Migration scripts are located in `src/main/resources/db/migration`.

## Security

The application uses JWT for authentication. JWT tokens are issued upon successful login and must be included in the `Authorization` header for protected API endpoints.

## Docker Support

A Dockerfile and docker-compose.yml file are provided for containerized deployment.

To build and run with Docker:

```bash
docker-compose up -d
```

## License

This project is licensed under the Apache License 2.0.