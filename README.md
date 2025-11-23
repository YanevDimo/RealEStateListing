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

### 7. Access the Application

- **Application URL:** http://localhost:8080
- **Default port:** 8080 (can be changed in `application.properties`)

## ⚙️ Configuration

### Application Properties

Key configuration in `src/main/resources/application.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | Application port | 8080 |
| `spring.datasource.url` | MySQL connection URL | `jdbc:mysql://localhost:3306/real_estate_hub` |
| `spring.datasource.username` | Database username | `root` (or `DB_USERNAME` env var) |
| `spring.datasource.password` | Database password | `770329` (or `DB_PASSWORD` env var) |
| `property.service.url` | Property service URL | `http://localhost:8083` |
| `app.upload.dir` | File upload directory | `uploads` |
| `spring.jpa.hibernate.ddl-auto` | Database schema strategy | `update` |

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

## 🔌 External Services

### Property Service Microservice

This application requires a property-service microservice to be running:

- **Default URL:** http://localhost:8083
- **Required Endpoints:**
  - `GET /api/v1/properties` - Get all properties
  - `GET /api/v1/properties/{id}` - Get property by ID
  - `GET /api/v1/properties/featured` - Get featured properties
  - `GET /api/v1/properties/search` - Search properties
  - `GET /api/v1/properties/agent/{agentId}` - Get properties by agent
  - `GET /api/v1/properties/city/{cityId}` - Get properties by city

**⚠️ Important:** The application will fail to load properties if the property-service is not running.

## 🧪 Testing

Run tests with:

```bash
./mvnw test
```

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

## 📝 Development Notes

- **Thymeleaf Cache:** Disabled in development (`spring.thymeleaf.cache=false`)
- **SQL Logging:** Enabled (`spring.jpa.show-sql=true`)
- **Hibernate DDL:** Set to `update` (auto-creates/updates schema)

## 🔒 Security Considerations

- Default database password is hardcoded - **change for production**
- Use environment variables for sensitive data
- Configure proper security settings for production
- Review Spring Security configuration in `SecurityConfig.java`

## 📄 License

[Add your license information here]

## 👥 Contributors

[Add contributor information here]

## 📞 Support

For issues and questions, please open an issue in the repository.

---




