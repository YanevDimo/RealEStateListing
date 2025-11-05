package app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded files from the uploads directory
        Path uploadPath = Paths.get(uploadDir);
        String uploadPathStr = uploadPath.toFile().getAbsolutePath();
        
        // Ensure path ends with separator for proper resource location
        String resourceLocation = uploadPathStr.endsWith("/") || uploadPathStr.endsWith("\\") 
                ? "file:" + uploadPathStr 
                : "file:" + uploadPathStr + "/";
        
        log.info("Configuring static resource handler for /uploads/** -> {}", resourceLocation);
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}











