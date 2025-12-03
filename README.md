# Real Estate Listing App Demo

A Spring Boot application for managing real estate listings, agents, and properties. This application communicates with a property-service microservice for property management.

## 📋 Prerequisites

Before running this application, ensure you have the following installed:

### Required Software

1. **Java Development Kit (JDK) 17**

   - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/#java17
   - Verify installation: `java -version` (should show version 17.x.x)

2. **Apache Maven 3.6+**

   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn -version`
   - Note: The project includes Maven Wrapper (`mvnw`/`mvnw.cmd`), so Maven installation is optional

3. **MySQL Database 8.0+**

   - Download from: https://dev.mysql.com/downloads/mysql/
   - Create a database named `real_estate_hub` (or configure in `application.properties`)
   - The application will auto-create the database if `createDatabaseIfNotExist=true` is set

4. **Property Service Microservice** (Required)
   - This application depends on a property-service microservice running on port 8083
   - The property-service must be running and accessible at `http://localhost:8083`
   - Ensure the property-service is started before running this application

5. **Docker & Docker Compose** (Optional - for Docker setup)
   - Download from: https://www.docker.com/products/docker-desktop
   - Verify installation: `docker --version` and `docker-compose --version`
   - Required only if using Docker deployment option

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd RealEstateListingAppDemo
```

### 2. Configure Database

#### Option A: Using Environment Variables (Recommended)

Set the following environment variables:

```bash
# Windows (PowerShell)
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"

# Windows (CMD)
set DB_USERNAME=root
set DB_PASSWORD=your_password

# Linux/Mac
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

#### Option B: Edit application.properties

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

**⚠️ Security Warning:** Never commit database credentials to version control. Use environment variables for production.

### 3. Configure Property Service URL

If the property-service is running on a different host/port, update:

```properties
property.service.url=http://localhost:8083
```

Or set as environment variable:

```bash
export PROPERTY_SERVICE_URL=http://localhost:8083
```

### 4. Create Uploads Directory

Create the uploads directory for file storage:

```bash
# Windows
mkdir uploads

# Linux/Mac
mkdir -p uploads
```

Or the application will create it automatically on first file upload.

### 5. Build the Application

```bash
# Using Maven Wrapper (recommended)
./mvnw clean install

# Windows
mvnw.cmd clean install

# Or using installed Maven
mvn clean install
```

### 6. Run the Application

#### Option A: Using Maven (Traditional)

```bash
# Using Maven Wrapper
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run

# Or using installed Maven
mvn spring-boot:run

# Or run the JAR file
java -jar target/RealEstateListingAppDemo-0.0.1-SNAPSHOT.jar
```

#### Option B: Using Docker (Recommended)

**Prerequisites:** Docker and Docker Compose must be installed

**Note:** The property-service must be running separately (either on host machine or in another container) and accessible at `http://localhost:8083` or configure `PROPERTY_SERVICE_URL` environment variable.

1. **Build the JAR file:**
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. **Build Docker image:**
   ```bash
   docker build -t realestate-app .
   ```

3. **Start all services (app + MySQL):**
   ```bash
   docker-compose up -d
   ```

4. **View logs:**
   ```bash
   docker-compose logs -f app
   ```

5. **Stop services:**
   ```bash
   docker-compose down
   ```

6. **Stop and remove volumes (clean slate):**
   ```bash
   docker-compose down -v
   ```

**Custom Property Service URL:**
If property-service runs on a different host/port, set environment variable:
```bash
PROPERTY_SERVICE_URL=http://your-host:8083 docker-compose up -d
```

**⚠️ Important:** 
- **Properties** are fetched from the `property-service` microservice. If you don't see properties, ensure the property-service is running on port 8083.
- **Agents** are stored in the main database. Register agents through the registration form at `/agent/register`.

### 7. Access the Application

- **Application URL:** http://localhost:8080
- **Default port:** 8080 (can be changed in `application.properties`)
- **MySQL:** Available on port 3307 (when using Docker, mapped from container port 3306)

## ✨ Features

### Public Features
- Browse and search properties with advanced filters (city, type, price range)
- View property details with images and descriptions
- Submit property inquiries
- View agent profiles and listings
- Contact form for general inquiries
- About page with application information

### User Features
- User registration and authentication
- View personal inquiry history
- Edit user profile information
- Secure password-based login

### Agent Features
- Agent registration with license number verification
- Create, edit, and delete properties
- Upload multiple property images
- Manage and respond to property inquiries
- View agent dashboard with statistics
- Edit agent profile (bio, experience, specializations)
- View inquiry details and update status

### Admin Features
- User management (view all users, activate/deactivate)
- Role management (change user roles: USER, AGENT, ADMIN)
- View all inquiries across the system
- System administration dashboard with statistics
- Monitor user activity and system health

### System Features
- **Spring Events:** Asynchronous event-driven notifications
- **Caching:** Performance optimization with Spring Cache
- **Scheduled Jobs:** Automated data synchronization
- **Microservice Communication:** Feign Client integration
- **Comprehensive Error Handling:** Global exception handler
- **File Upload:** Secure image upload functionality
- **Data Validation:** DTO validation with Jakarta Validation
- **Security:** Role-based access control with Spring Security

## 🎯 Valid Domain Functionalities

The application implements the following valid domain functionalities that cause state changes:

### 1. Create Inquiry
- **Endpoint:** `POST /properties/{id}/inquiry`
- **Description:** Users can submit inquiries about properties
- **State Changes:** Creates new `Inquiry` entity in database
- **Visible Result:** Success message displayed to user
- **Additional:** Triggers Spring Events for notifications

### 2. Agent Registration
- **Endpoint:** `POST /agent/register`
- **Description:** New agents can register with license verification
- **State Changes:** Creates `User` and `Agent` entities
- **Visible Result:** Redirects to login page with success message
- **Validation:** Ensures unique email and license number

### 3. Create Property
- **Endpoint:** `POST /agent/properties/add`
- **Description:** Agents can create new property listings
- **State Changes:** Creates property via property-service, updates agent listing count
- **Visible Result:** Success message and redirect to dashboard
- **Features:** Image upload, property details validation

### 4. Edit Property
- **Endpoint:** `POST /agent/properties/edit/{id}`
- **Description:** Agents can update their property listings
- **State Changes:** Updates property in property-service, invalidates cache
- **Visible Result:** Success message displayed
- **Security:** Only property owner can edit

### 5. Delete Property
- **Endpoint:** `POST /agent/properties/delete/{id}`
- **Description:** Agents can delete their property listings
- **State Changes:** Removes property from property-service, updates agent statistics
- **Visible Result:** Success confirmation message
- **Security:** Only property owner can delete

### 6. Update Inquiry Status
- **Endpoint:** `POST /agent/inquiries/{id}/update`
- **Description:** Agents can respond to inquiries and update status
- **State Changes:** Updates `Inquiry` entity status and response
- **Visible Result:** Success message displayed
- **Status Flow:** NEW → CONTACTED → CLOSED

### 7. User Registration
- **Endpoint:** `POST /auth/register`
- **Description:** New users can create accounts
- **State Changes:** Creates new `User` entity with hashed password
- **Visible Result:** Redirects to login page
- **Validation:** Email uniqueness, password strength

### 8. Admin Role Management
- **Endpoint:** `POST /admin/users/{id}/role`
- **Description:** Admin can change user roles
- **State Changes:** Updates `User` entity role field
- **Visible Result:** Success confirmation message
- **Roles:** USER, AGENT, ADMIN

### 9. User Activation/Deactivation
- **Endpoint:** `POST /admin/users/{id}/activate` or `/deactivate`
- **Description:** Admin can activate or deactivate user accounts
- **State Changes:** Updates `User.isActive` field
- **Visible Result:** Success message displayed
- **Effect:** Deactivated users cannot login

## ⚙️ Configuration

### Application Properties

Key configuration in `src/main/resources/application.properties`:

| Property                        | Description              | Default                                       |
| ------------------------------- | ------------------------ | --------------------------------------------- |
| `server.port`                   | Application port         | 8080                                          |
| `spring.datasource.url`         | MySQL connection URL     | `jdbc:mysql://localhost:3306/real_estate_hub` |
| `spring.datasource.username`    | Database username        | `root` (or `DB_USERNAME` env var)             |
| `spring.datasource.password`    | Database password        | `770329` (or `DB_PASSWORD` env var)           |
| `property.service.url`          | Property service URL     | `http://localhost:8083`                       |
| `app.upload.dir`                | File upload directory    | `uploads`                                     |
| `spring.jpa.hibernate.ddl-auto` | Database schema strategy | `update`                                      |

### Environment Variables

The following environment variables can be used:

- `DB_USERNAME` - MySQL username (default: `root`)
- `DB_PASSWORD` - MySQL password (default: `770329`)
- `PROPERTY_SERVICE_URL` - Property service URL (default: `http://localhost:8083`)

## 🏗️ Project Structure

```
RealEstateListingAppDemo/
├── src/
│   ├── main/
│   │   ├── java/app/
│   │   │   ├── client/          # Feign clients for microservice communication
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # MVC controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Exception handlers
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── service/          # Business logic services
│   │   │   └── util/             # Utility classes
│   │   └── resources/
│   │       ├── templates/       # Thymeleaf templates
│   │       ├── static/          # Static resources (CSS, JS)
│   │       └── application.properties
│   └── test/                     # Test files
├── uploads/                      # File upload directory (created at runtime)
├── pom.xml                       # Maven configuration
└── README.md                     # This file
```

## 🔧 Technology Stack

- **Framework:** Spring Boot 3.4.0
- **Java Version:** 17
- **Build Tool:** Maven
- **Database:** MySQL 8.0+
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security
- **Templating:** Thymeleaf
- **Microservice Communication:** Spring Cloud OpenFeign
- **Lombok:** For reducing boilerplate code

## 📦 Dependencies

Key dependencies (see `pom.xml` for complete list):

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Thymeleaf
- Spring Cloud OpenFeign
- MySQL Connector
- Lombok
- Spring Boot DevTools (development only)

## 🗄️ Database Setup

### Automatic Setup

The application uses `spring.jpa.hibernate.ddl-auto=update`, which automatically creates/updates database tables on startup.

### Manual Setup (Optional)

1. Create database:

```sql
CREATE DATABASE real_estate_hub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Grant privileges:

```sql
GRANT ALL PRIVILEGES ON real_estate_hub.* TO 'your_username'@'localhost';
FLUSH PRIVILEGES;
```

## 🔗 Integrations with Other Systems/Applications

### Property Service Microservice
- **Type:** REST API Microservice
- **Communication Method:** Spring Cloud OpenFeign
- **Default URL:** http://localhost:8083
- **Purpose:** Manages all property data (CRUD operations)
- **Database:** Separate database from main application
- **Required Endpoints:**
  - `GET /api/v1/properties` - Retrieve all properties
  - `GET /api/v1/properties/{id}` - Get property by ID
  - `GET /api/v1/properties/featured` - Get featured properties
  - `GET /api/v1/properties/search` - Search properties with filters
  - `GET /api/v1/properties/agent/{agentId}` - Get properties by agent
  - `GET /api/v1/properties/city/{cityId}` - Get properties by city
  - `POST /api/v1/properties` - Create new property
  - `PUT /api/v1/properties/{id}` - Update property
  - `DELETE /api/v1/properties/{id}` - Delete property

**⚠️ Important:** The application will fail to load properties if the property-service is not running.

### Database Integration
- **Type:** MySQL 8.0+ Database
- **Purpose:** Stores users, agents, cities, property types, and inquiries
- **Connection:** Spring Data JPA / Hibernate ORM
- **Auto-schema:** Tables created automatically on startup (`ddl-auto=update`)
- **Primary Keys:** UUID for all entities
- **Relationships:** 
  - User ↔ Agent (OneToOne)
  - User ↔ Inquiry (OneToMany)
  - City, PropertyType (standalone entities)

## 🧪 Testing

### Running Tests

Run all tests with:

```bash
./mvnw test
```

### Test Coverage

The application includes comprehensive test coverage:

- **Total Tests:** 387 tests
- **Unit Tests:** 163 tests (services, utilities)
- **Integration Tests:** 62 tests (services with real H2 database)
- **API Tests:** 27 tests (REST controllers with MockMvc)
- **MVC Tests:** 33 tests (web controllers)
- **DTO Validation Tests:** 37 tests
- **Exception Handler Tests:** 8 tests
- **Utility Tests:** 6 tests

### Test Coverage Report

Generate coverage report with JaCoCo:

```bash
./mvnw clean test jacoco:report
```

View report at: `target/site/jacoco/index.html`

**Coverage Target:** 80%+ (configured in `pom.xml`)

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Error**

   - Verify MySQL is running: `mysql -u root -p`
   - Check database credentials in `application.properties`
   - Ensure database `real_estate_hub` exists or can be created

2. **Property Service Connection Error**

   - Verify property-service is running on port 8083
   - Check `property.service.url` configuration
   - Check network connectivity to property-service

3. **Port Already in Use**

   - Change port in `application.properties`: `server.port=8081`
   - Or stop the process using port 8080

4. **File Upload Errors**

   - Ensure `uploads/` directory exists and is writable
   - Check file size limits in `application.properties`

5. **Lombok Not Working**
   - Ensure your IDE has Lombok plugin installed
   - Enable annotation processing in IDE settings

6. **Docker Issues**
   - Ensure Docker is running: `docker --version`
   - Check if port 3307 or 8080 is already in use (MySQL uses 3307 to avoid conflicts)
   - View container logs: `docker-compose logs app`
   - Rebuild image if code changes: `docker-compose up -d --build`

7. **No Properties or Agents Showing**
   - **Properties:** Ensure the property-service microservice is running on port 8083
     - Properties are fetched from the property-service, not stored in the main database
     - Check logs: `docker-compose logs app | grep -i "property-service"`
   - **Agents:** Register agents through the registration form
     - Visit http://localhost:8080/agent/register to create an agent account
     - Agents are stored in the main database

## 📝 Development Notes

- **Thymeleaf Cache:** Disabled in development (`spring.thymeleaf.cache=false`)
- **SQL Logging:** Enabled (`spring.jpa.show-sql=true`)
- **Hibernate DDL:** Set to `update` (auto-creates/updates schema)
- **Scheduled Jobs:** 
  - Daily sync at 3:00 AM (cron: `0 0 3 * * ?`)
  - Rating updates every 15 minutes (fixed delay)
- **Caching:** Enabled for cities, property types, featured properties, and all properties
- **Event Processing:** Async event listeners for better performance

## 📚 Additional Information

### Project Statistics
- **Total Java Classes:** 60+
- **Total Test Classes:** 28
- **Total Tests:** 387
- **Web Pages:** 22 dynamic Thymeleaf templates
- **REST Endpoints:** 4 REST controllers for API access
- **Entities:** 5 domain entities
- **Services:** 11 service classes
- **Repositories:** 5 JPA repositories

### Code Quality
- **No Dead Code:** All code is actively used
- **Clean Code:** No comments, well-structured
- **Naming Conventions:** Follows Java standards
- **Error Handling:** Comprehensive exception handling
- **Validation:** DTO validation throughout
- **Logging:** Comprehensive logging with SLF4J

## 🔒 Security

### Authentication & Authorization
- **Authentication:** Form-based login with Spring Security
- **Password Hashing:** BCryptPasswordEncoder (one-way hashing)
- **Session Management:** HTTP session-based authentication
- **CSRF Protection:** Enabled by default (not disabled)

### Role-Based Access Control
- **Roles:** USER, AGENT, ADMIN
- **Public Endpoints:** Home, properties, agents list, about, contact, login, register
- **Authenticated Endpoints:** Agent dashboard, property management
- **Admin-Only Endpoints:** `/admin/**` (user management, role changes)

### Security Features
- Password validation and strength requirements
- Email uniqueness validation
- License number uniqueness for agents
- Property ownership verification (agents can only edit/delete their own properties)
- Inquiry access control (agents can only view inquiries for their properties)

### Security Configuration
- **File:** `src/main/java/app/config/SecurityConfig.java`
- **Custom Success Handler:** Role-based redirect after login
- **Logout:** Proper session invalidation and cookie deletion

### Security Considerations
- Default database password is hardcoded - **change for production**
- Use environment variables for sensitive data
- Configure proper security settings for production
- Review Spring Security configuration in `SecurityConfig.java`

## 📊 Architecture

### System Architecture
- **Main Application:** Port 8080 (Spring Boot MVC application)
- **Property Service:** Port 8083 (Separate microservice)
- **Database:** MySQL (separate from property-service database)
- **Communication:** Feign Client for inter-service REST calls

### Application Layers
1. **Controller Layer:** Handles HTTP requests, view rendering
2. **Service Layer:** Business logic, validation, orchestration
3. **Repository Layer:** Data access via Spring Data JPA
4. **Entity Layer:** Domain models with JPA annotations
5. **DTO Layer:** Data transfer objects for API communication
6. **Exception Layer:** Global exception handling

### Design Patterns
- **Layered Architecture:** Clear separation of concerns
- **Repository Pattern:** Data access abstraction
- **Service Pattern:** Business logic encapsulation
- **DTO Pattern:** Data transfer between layers
- **Event-Driven:** Spring Events for decoupled notifications

### Key Components
- **Feign Client:** Microservice communication
- **Spring Events:** Asynchronous event processing
- **Spring Cache:** Performance optimization
- **Scheduled Tasks:** Automated background jobs
- **Global Exception Handler:** Centralized error handling

## 🎁 Bonus Features

### Docker Setup (1 bonus point)
- **Dockerfile:** Containerizes the Spring Boot application (multi-stage build)
- **docker-compose.yml:** Orchestrates both application and MySQL database
- **Features:**
  - MySQL 8.0 container with persistent data volume
  - Application container with health checks
  - Automatic database initialization
  - Network isolation between services
  - Easy deployment with single command
  - Port mapping: MySQL on 3307 (host) → 3306 (container) to avoid conflicts

**How to use:**
1. Start everything: `docker-compose up -d` (builds JAR automatically)
2. Access: http://localhost:8080
3. Register agents through the registration form at `/agent/register`

**Note:** Properties require the property-service microservice to be running separately on port 8083.

**Files:**
- `Dockerfile` - Application container definition (multi-stage build)
- `docker-compose.yml` - Multi-container orchestration
- `.dockerignore` - Excludes unnecessary files from build

### Spring Events Implementation (1 bonus point)
- **Event Class:** `InquiryCreatedEvent` - Custom application event
- **Event Listeners:**
  - `InquiryNotificationListener` - Sends notifications (async)
  - `InquiryStatisticsListener` - Updates statistics (sync)
- **Event Publishing:** Automatic event publishing when inquiries are created
- **Async Processing:** `@Async` annotation for non-blocking event handling
- **Benefits:** Decoupled architecture, easy to extend with more listeners

**How it works:**
1. User submits inquiry → `InquiryService.createInquiry()` is called
2. Inquiry saved to database
3. `InquiryCreatedEvent` is published
4. Listeners automatically react:
   - Notification listener sends agent notification
   - Statistics listener updates inquiry counts

#





