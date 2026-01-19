package tech.onova.flagd_admin_server.domain.service.impl;

import dev.openfeature.contrib.providers.flagd.Config;
import dev.openfeature.contrib.providers.flagd.FlagdOptions;
import dev.openfeature.contrib.providers.flagd.FlagdProvider;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ImmutableContext;
import org.springframework.stereotype.Service;
import tech.onova.flagd_admin_server.domain.exception.ContentValidationException;
import tech.onova.flagd_admin_server.domain.service.ContentValidator;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FlagdContentValidator implements ContentValidator {
    
    @Log
    public void validateContent(String content) throws ContentValidationException {
        Path tempFile = null;
        try {
            // Create temporary file
            tempFile = Files.createTempFile("flagd-validation-", ".json");
            Files.writeString(tempFile, content);
            
            // Initialize FlagdProvider with temp file path
            FlagdProvider flagdProvider = new FlagdProvider(
                FlagdOptions.builder()
                    .resolverType(Config.Resolver.FILE)
                    .offlineFlagSourcePath(tempFile.toString())
                    .build()
            );
            
            // Try to initialize the provider to validate content
            EvaluationContext context = new ImmutableContext();
            flagdProvider.initialize(context);
            
        } catch (IOException e) {
            throw new ContentValidationException("Failed to create temporary file for validation", e);
        } catch (Exception | Error e) {
            throw new ContentValidationException("Content validation failed: " + e.getMessage(), e);
        } finally {
            // Always clean up temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    // Log cleanup failure but don't throw
                    System.err.println("Failed to delete temporary file: " + e.getMessage());
                }
            }
        }
    }
}