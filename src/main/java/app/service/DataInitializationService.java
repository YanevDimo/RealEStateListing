package app.service;


import app.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    private final UserService userService;
    private final AgentService agentService;
    private final PropertyService propertyService;
    private final CityService cityService;
    private final PropertyTypeService propertyTypeService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");
        
        try {
            // Always initialize cities and property types
            initializeCitiesAndPropertyTypes();
            
            // Only create agents and properties if they don't exist
            if (agentService.countAllAgents() == 0) {
                log.info("No agents found, creating sample agents and properties");
                initializeSampleAgentsAndProperties();
            } else {
                log.info("Agents already exist, skipping agent creation");
            }
            
            log.info("Data initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during data initialization", e);
        }
    }

    private void initializeCitiesAndPropertyTypes() {
        log.info("Initializing cities and property types...");
        
        // Create Bulgarian cities
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

    private void initializeSampleAgentsAndProperties() {
        // Get existing cities and property types
        City sofia = cityService.findCityByName("Sofia").orElse(createCity("Sofia"));
        City plovdiv = cityService.findCityByName("Plovdiv").orElse(createCity("Plovdiv"));
        City varna = cityService.findCityByName("Varna").orElse(createCity("Varna"));

        PropertyType apartment = propertyTypeService.findPropertyTypeByName("Apartment").orElse(createPropertyType("Apartment"));
        PropertyType house = propertyTypeService.findPropertyTypeByName("House").orElse(createPropertyType("House"));
        PropertyType villa = propertyTypeService.findPropertyTypeByName("Villa").orElse(createPropertyType("Villa"));

        // Create users and agents
        User user1 = createUser("john.smith@example.com", "John Smith", "+359888123456");
        Agent agent1 = createAgent(user1, "LIC001", "Experienced real estate agent specializing in Sofia properties", 5, "[\"Residential\", \"Commercial\"]");

        User user2 = createUser("sarah.johnson@example.com", "Sarah Johnson", "+359888123457");
        Agent agent2 = createAgent(user2, "LIC002", "Luxury property specialist with 10+ years experience", 10, "[\"Luxury Homes\", \"Villas\"]");

        User user3 = createUser("michael.brown@example.com", "Michael Brown", "+359888123458");
        Agent agent3 = createAgent(user3, "LIC003", "Expert in residential properties and first-time buyers", 7, "[\"Residential\", \"New Construction\"]");

        // Create sample properties
        createProperty("Beautiful 3-bedroom apartment in Sofia center", 
                     "Modern apartment with stunning city views, fully furnished", 
                     apartment, sofia, "Vitosha Blvd 15", 
                     new BigDecimal("150000"), 3, 2, new BigDecimal("120.50"), 2020, agent1);

        createProperty("Luxury villa with garden in Plovdiv", 
                     "Spacious villa with private garden, perfect for families", 
                     villa, plovdiv, "Tsar Boris III 25", 
                     new BigDecimal("250000"), 4, 3, new BigDecimal("200.00"), 2018, agent2);

        createProperty("Cozy house near Varna beach", 
                     "Charming house just 5 minutes from the beach", 
                     house, varna, "Primorski Blvd 10", 
                     new BigDecimal("180000"), 3, 2, new BigDecimal("150.75"), 2019, agent3);

        createProperty("Modern apartment with sea view", 
                     "Brand new apartment with panoramic sea views", 
                     apartment, varna, "Morska Gradina 5", 
                     new BigDecimal("200000"), 2, 2, new BigDecimal("95.00"), 2021, agent1);

        createProperty("Family house with large yard", 
                     "Perfect family home with spacious yard for children", 
                     house, sofia, "Boyana District 12", 
                     new BigDecimal("220000"), 4, 3, new BigDecimal("180.00"), 2017, agent2);
    }

    private City createCity(String name) {


        City city = City.builder()
                .name(name)
                .build();
        return cityService.saveCity(city);
    }

    private PropertyType createPropertyType(String name) {
        PropertyType type = PropertyType.builder()
                .name(name)
                .build();
        return propertyTypeService.savePropertyType(type);
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

    private Property createProperty(String title, String description, PropertyType type, City city, 
                                  String address, BigDecimal price, int beds, int baths, 
                                  BigDecimal areaSqm, int yearBuilt, Agent agent) {
        Property property = Property.builder()
                .title(title)
                .description(description)
                .propertyType(type)
                .city(city)
                .address(address)
                .price(price)
                .beds(beds)
                .baths(baths)
                .areaSqm(areaSqm)
                .yearBuilt(yearBuilt)
                .agent(agent)
                .status(PropertyStatus.ACTIVE)
                .featured(false)
                .build();
        
        Property savedProperty = propertyService.saveProperty(property);
        
        // Update agent's listing count
        agentService.incrementAgentListings(agent.getId());
        
        return savedProperty;
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







