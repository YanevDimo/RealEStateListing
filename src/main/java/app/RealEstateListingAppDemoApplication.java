package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "app.client")
public class RealEstateListingAppDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealEstateListingAppDemoApplication.class, args);
    }

}
