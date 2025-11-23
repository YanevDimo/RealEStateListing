# Setup Checklist for New Machine

Use this checklist when setting up the application on a new machine.

## ✅ Pre-Installation Checklist

- [ ] **Java JDK 17** installed and verified
  - Command: `java -version`
  - Expected: Java version 17.x.x

- [ ] **Maven 3.6+** installed (optional - project includes Maven Wrapper)
  - Command: `mvn -version`
  - Or use: `./mvnw --version`

- [ ] **MySQL 8.0+** installed and running
  - Command: `mysql --version`
  - Service running: Check MySQL service status

- [ ] **Git** installed (for cloning repository)
  - Command: `git --version`

## ✅ Database Setup

- [ ] MySQL server is running
- [ ] Database credentials are known (username/password)
- [ ] Database `real_estate_hub` can be created (or already exists)
- [ ] User has CREATE DATABASE privileges (if using `createDatabaseIfNotExist=true`)

## ✅ Property Service Setup

- [ ] **Property Service Microservice** is available
- [ ] Property service runs on port 8083 (or configure custom URL)
- [ ] Property service is accessible from this machine
- [ ] Property service has data/seeded (optional, for testing)

## ✅ Configuration Steps

- [ ] Repository cloned: `git clone <repository-url>`
- [ ] Navigate to project: `cd RealEstateListingAppDemo`
- [ ] Set environment variables:
  - `DB_USERNAME` (default: root)
  - `DB_PASSWORD` (required)
  - `PROPERTY_SERVICE_URL` (if different from localhost:8083)
- [ ] Or edit `src/main/resources/application.properties`:
  - Update `spring.datasource.username`
  - Update `spring.datasource.password`
  - Update `property.service.url` (if needed)

## ✅ Build and Run

- [ ] Build project: `./mvnw clean install` (or `mvnw.cmd` on Windows)
- [ ] Create uploads directory: `mkdir uploads` (or let app create it)
- [ ] Start property-service (if separate)
- [ ] Run application: `./mvnw spring-boot:run`
- [ ] Verify application starts without errors
- [ ] Access application: http://localhost:8080

## ✅ Verification

- [ ] Application starts successfully
- [ ] No database connection errors
- [ ] No property-service connection errors
- [ ] Home page loads: http://localhost:8080
- [ ] Can access properties page
- [ ] Can access agents page
- [ ] File uploads work (if testing)

## 🔧 Troubleshooting

If issues occur, check:

1. **Port conflicts**: Ensure port 8080 is available
2. **Database connection**: Verify MySQL is running and credentials are correct
3. **Property service**: Ensure it's running and accessible
4. **Java version**: Must be exactly JDK 17
5. **Maven dependencies**: Run `./mvnw clean install` to download dependencies

## 📋 Minimum Requirements Summary

| Requirement | Version | Notes |
|------------|---------|-------|
| Java JDK | 17 | Required |
| Maven | 3.6+ | Optional (wrapper included) |
| MySQL | 8.0+ | Required |
| Property Service | - | Required (port 8083) |
| OS | Any | Windows, Linux, macOS |

## 🚨 Critical Dependencies

1. **MySQL Database** - Application will not start without database connection
2. **Property Service** - Application will start but property features won't work
3. **Java 17** - Application will not compile/run with other Java versions

---

**Quick Start Command Sequence:**

```bash
# 1. Clone repository
git clone <repository-url>
cd RealEstateListingAppDemo

# 2. Set environment variables (Linux/Mac)
export DB_USERNAME=root
export DB_PASSWORD=your_password

# 3. Build
./mvnw clean install

# 4. Run
./mvnw spring-boot:run
```


