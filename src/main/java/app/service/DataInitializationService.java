package app.service;

import app.entity.Agent;
import app.entity.City;
import app.entity.PropertyType;
import app.entity.User;
import app.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    private final UserService userService;
    private final AgentService agentService;
    private final CityService cityService;
    private final PropertyTypeService propertyTypeService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");
        try {
            // Initialize cities and property types (master data for main app)
            initializeCitiesAndPropertyTypes();
            log.info("Data initialization completed successfully.");
        } catch (Exception e) {
            log.error("Error during data initialization", e);
            // Don't fail application startup if seeding fails
        }
        // Note: Property seeding is handled by Property Service microservice
    }

    private void initializeCitiesAndPropertyTypes() {
        log.info("Initializing cities and property types...");

        createCityIfNotExists("Sofia");
        createCityIfNotExists("Plovdiv");
        createCityIfNotExists("Varna");
        createCityIfNotExists("Burgas");
        createCityIfNotExists("Ruse");
        createCityIfNotExists("Stara Zagora");
        createCityIfNotExists("Pleven");
        createCityIfNotExists("Sliven");
        createCityIfNotExists("Dobrich");
        createCityIfNotExists("Shumen");
        createCityIfNotExists("Pernik");
        createCityIfNotExists("Yambol");
        createCityIfNotExists("Khaskovo");
        createCityIfNotExists("Pazardzhik");
        createCityIfNotExists("Blagoevgrad");
        createCityIfNotExists("Veliko Tarnovo");
        createCityIfNotExists("Vratsa");
        createCityIfNotExists("Gabrovo");
        createCityIfNotExists("Asenovgrad");
        createCityIfNotExists("Vidin");
        createCityIfNotExists("Kazanlak");
        createCityIfNotExists("Kyustendil");
        createCityIfNotExists("Montana");
        createCityIfNotExists("Targovishte");
        createCityIfNotExists("Razgrad");
        createCityIfNotExists("Silistra");
        createCityIfNotExists("Lovech");
        createCityIfNotExists("Smolyan");
        createCityIfNotExists("Kardzhali");
        createCityIfNotExists("Tarnovo");
        createCityIfNotExists("Sandanski");
        createCityIfNotExists("Gotse Delchev");
        createCityIfNotExists("Petrich");
        createCityIfNotExists("Samokov");
        createCityIfNotExists("Svishchev");
        createCityIfNotExists("Lom");
        createCityIfNotExists("Sevlievo");
        createCityIfNotExists("Nova Zagora");
        createCityIfNotExists("Troyan");
        createCityIfNotExists("Aytos");
        createCityIfNotExists("Botevgrad");
        createCityIfNotExists("Gorna Oryahovitsa");
        createCityIfNotExists("Karnobat");
        createCityIfNotExists("Panagyurishte");
        createCityIfNotExists("Chirpan");
        createCityIfNotExists("Popovo");
        createCityIfNotExists("Radomir");
        createCityIfNotExists("Harmanli");
        createCityIfNotExists("Ikhtiman");
        createCityIfNotExists("Elena");
        createCityIfNotExists("Chepelare");
        createCityIfNotExists("Koprivshtitsa");
        createCityIfNotExists("Krichim");
        createCityIfNotExists("Lukovit");
        createCityIfNotExists("Nesebar");
        createCityIfNotExists("Omurtag");
        createCityIfNotExists("Pavlikeni");
        createCityIfNotExists("Perushtitsa");
        createCityIfNotExists("Provadiya");
        createCityIfNotExists("Radnevo");
        createCityIfNotExists("Rudozem");
        createCityIfNotExists("Saedinenie");
        createCityIfNotExists("Sozopol");
        createCityIfNotExists("Svetovrachane");
        createCityIfNotExists("Tervel");
        createCityIfNotExists("Teteven");
        createCityIfNotExists("Trun");
        createCityIfNotExists("Tsarevo");
        createCityIfNotExists("Valchedram");
        createCityIfNotExists("Velingrad");
        createCityIfNotExists("Vetovo");
        createCityIfNotExists("Zlatograd");

        // Create property types
        createPropertyTypeIfNotExists("Apartment");
        createPropertyTypeIfNotExists("House");
        createPropertyTypeIfNotExists("Villa");
        createPropertyTypeIfNotExists("Studio");
        createPropertyTypeIfNotExists("Duplex");
        createPropertyTypeIfNotExists("Penthouse");
        createPropertyTypeIfNotExists("Townhouse");
        createPropertyTypeIfNotExists("Commercial");
        createPropertyTypeIfNotExists("Office");
        createPropertyTypeIfNotExists("Warehouse");
        createPropertyTypeIfNotExists("Land");
        createPropertyTypeIfNotExists("Farm");

        log.info("Cities and property types initialization completed");
    }

    // Note: Property creation removed - properties are now managed by Property Service microservice
    // This method is kept for reference but should not be used
    private void initializeSampleAgentsAndProperties() {
        // Properties are now created via Property Service microservice
        // This method is disabled as properties are managed externally
        log.debug("Property initialization disabled - properties are managed by Property Service");
    }

    private City createCity(String name) {
        City city = City.builder()
                .name(name)
                .build();
        try {
            return cityService.saveCity(city);
        } catch (DataIntegrityViolationException e) {
            // City already exists, return existing
            return cityService.findCityByName(name).orElseThrow(() -> e);
        }
    }

    private PropertyType createPropertyType(String name) {
        PropertyType type = PropertyType.builder()
                .name(name)
                .build();
        try {
            return propertyTypeService.savePropertyType(type);
        } catch (DataIntegrityViolationException e) {
            // Type already exists, return existing
            return propertyTypeService.findPropertyTypeByName(name).orElseThrow(() -> e);
        }
    }

    private User createUser(String email, String name, String phone) {
        User user = User.builder()
                .email(email)
                .name(name)
                .phone(phone)
                .passwordHash("$2a$10$dummy.hash.for.testing.purposes")
                .role(UserRole.AGENT)
                .isActive(true)
                .build();
        return userService.saveUser(user);
    }

    private Agent createAgent(User user, String licenseNumber, String bio, int experienceYears, String specializations) {
        Agent agent = Agent.builder()
                .user(user)
                .licenseNumber(licenseNumber)
                .bio(bio)
                .experienceYears(experienceYears)
                .specializations(specializations)
                .rating(new BigDecimal("4.5"))
                .totalListings(0)
                .build();
        return agentService.saveAgent(agent);
    }

    // Note: Property creation removed - properties are now managed by Property Service microservice
    // This method is disabled as Property entity no longer exists in main app
    private void createProperty(String title, String description, PropertyType type, City city,
                               String address, BigDecimal price, int beds, int baths,
                               BigDecimal areaSqm, int yearBuilt, Agent agent) {
        // Properties are now created via Property Service REST API
        // Use PropertyServiceClient.createProperty() instead
        log.debug("Property creation disabled - properties are managed by Property Service microservice");
    }

    private City createCityIfNotExists(String name) {
        return cityService.findCityByName(name)
                .orElseGet(() -> createCity(name));
    }

    private PropertyType createPropertyTypeIfNotExists(String name) {
        return propertyTypeService.findPropertyTypeByName(name)
                .orElseGet(() -> createPropertyType(name));
    }
}