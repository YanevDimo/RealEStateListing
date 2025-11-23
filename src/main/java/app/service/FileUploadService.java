package app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

      //Upload multiple files and return their URLs

    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();
        
        if (files == null || files.isEmpty()) {
            return uploadedUrls;
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String url = uploadFile(file);
                if (url != null) {
                    uploadedUrls.add(url);
                }
            }
        }

        return uploadedUrls;
    }

     // Upload a single file and return its URL

    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }

        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidFileExtension(originalFilename)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + ALLOWED_EXTENSIONS);
        }

        // Generate unique filename
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

        // Create upload path
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file
        Path targetPath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Return URL path
        String fileUrl = "/uploads/" + uniqueFilename;
        log.info("File uploaded successfully: {}", fileUrl);
        
        return fileUrl;
    }


    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
                return false;
            }

            String filename = fileUrl.substring("/uploads/".length());
            Path filePath = Paths.get(uploadDir, filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: {}", fileUrl);
                return true;
            }
            
            return false;
        } catch (IOException e) {
            log.error("Error deleting file: {}", fileUrl, e);
            return false;
        }
    }


    private boolean isValidFileExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }


    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }


     // Get upload directory path

    public String getUploadDir() {
        return uploadDir;
    }


    public long getMaxFileSize() {
        return maxFileSize;
    }
}











