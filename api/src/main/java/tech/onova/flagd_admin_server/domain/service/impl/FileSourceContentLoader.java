package tech.onova.flagd_admin_server.domain.service.impl;

import org.springframework.stereotype.Component;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.exception.SourceContentAccessException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentNotFoundException;
import tech.onova.flagd_admin_server.domain.service.SourceContentLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

record ValidatedFileUri(String filePath, Path path) {
}

@Component
public class FileSourceContentLoader implements SourceContentLoader {
    
    @Override
    public boolean supports(String uriScheme) {
        return "file".equalsIgnoreCase(uriScheme);
    }
    
    @Override
    public String loadContent(SourceUri sourceUri) {
        ValidatedFileUri validated = validateFileUri(sourceUri);
        
        if (!Files.exists(validated.path())) {
            throw new SourceContentNotFoundException("File not found: " + validated.filePath());
        }
        
        if (!Files.isReadable(validated.path())) {
            throw new SourceContentAccessException("File not readable: " + validated.filePath());
        }
        
        try {
            return Files.readString(validated.path());
        } catch (IOException e) {
            throw new SourceContentAccessException("Error reading file: " + validated.filePath(), e);
        }
    }
    
    @Override
    public void initializeContent(SourceUri sourceUri, String content) {
        ValidatedFileUri validated = validateFileUri(sourceUri);
        
        try {
            // Create parent directories if they don't exist
            Path parentDir = validated.path().getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            if (content == null) {
                content = "{\"$schema\": \"https://flagd.dev/schema/v0/flags.json\",\"flags\": {}}";
            }
            
            // Write the content to the file
            Files.writeString(validated.path(), content);
        } catch (IOException e) {
            throw new SourceContentAccessException("Error writing to file: " + validated.filePath(), e);
        }
    }
    
    private ValidatedFileUri validateFileUri(SourceUri sourceUri) {
        String uri = sourceUri.uri();
        if (!uri.startsWith("file://")) {
            throw new IllegalArgumentException("Invalid file URI: " + uri);
        }
        
        String filePath = uri.substring(7); // Remove "file://" prefix
        Path path = Paths.get(filePath);
        
        return new ValidatedFileUri(filePath, path);
    }
}