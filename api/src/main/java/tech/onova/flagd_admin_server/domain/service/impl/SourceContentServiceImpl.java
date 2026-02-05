package tech.onova.flagd_admin_server.domain.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.exception.ContentValidationException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentAccessException;
import tech.onova.flagd_admin_server.domain.exception.SourceContentNotFoundException;
import tech.onova.flagd_admin_server.domain.exception.UnsupportedSourceUriException;
import tech.onova.flagd_admin_server.domain.service.ContentValidator;
import tech.onova.flagd_admin_server.domain.service.SourceContentLoader;
import tech.onova.flagd_admin_server.domain.service.SourceContentService;

import java.util.List;

@Service
public class SourceContentServiceImpl implements SourceContentService {
    
    private final List<SourceContentLoader> contentLoaders;
    private final ContentValidator contentValidator;
    
    @Autowired
    public SourceContentServiceImpl(List<SourceContentLoader> contentLoaders, ContentValidator contentValidator) {
        this.contentLoaders = contentLoaders;
        this.contentValidator = contentValidator;
    }
    
    public String loadContent(SourceUri sourceUri) throws SourceContentNotFoundException, SourceContentAccessException {
        String uri = sourceUri.uri();
        String scheme = extractScheme(uri);

        return contentLoaders.stream()
                .filter(loader -> loader.supports(scheme))
                .findFirst()
                .orElseThrow(() -> new UnsupportedSourceUriException("Unsupported URI scheme: " + scheme))
                .loadContent(sourceUri);
    }
    
    public void initializeContent(SourceUri sourceUri, String content) throws SourceContentAccessException, ContentValidationException {
        contentValidator.validateContent(content);
        
        String uri = sourceUri.uri();
        String scheme = extractScheme(uri);

        contentLoaders.stream()
                .filter(loader -> loader.supports(scheme))
                .findFirst()
                .orElseThrow(() -> new UnsupportedSourceUriException("Unsupported URI scheme: " + scheme))
                .initializeContent(sourceUri, content);
    }
    
    public boolean contentExists(SourceUri sourceUri) throws SourceContentAccessException {
        String uri = sourceUri.uri();
        String scheme = extractScheme(uri);

        return contentLoaders.stream()
                .filter(loader -> loader.supports(scheme))
                .findFirst()
                .map(loader -> {
                    try {
                        loader.loadContent(sourceUri);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .orElse(false);
    }
    
    public void initializeContentWithConfig(SourceUri sourceUri) throws SourceContentAccessException, ContentValidationException {
        String uri = sourceUri.uri();
        String scheme = extractScheme(uri);

        String currentContent = null;
        var contentLoader = contentLoaders.stream()
                .filter(loader -> loader.supports(scheme))
                .findFirst()
                .orElseThrow(() -> new UnsupportedSourceUriException("Unsupported URI scheme: " + scheme));

        try {
            currentContent = contentLoader.loadContent(sourceUri);
        } catch (SourceContentNotFoundException ignored) { }

        if (currentContent != null) {
            // Validate existing content to ensure it's a valid flagd configuration file
            contentValidator.validateContent(currentContent);
        } else {
            // File doesn't exist, create with default valid content
            contentLoader.initializeContent(sourceUri, null);
        }
    }
    
    private String extractScheme(String uri) {
        int colonIndex = uri.indexOf(':');
        if (colonIndex <= 0) {
            throw new IllegalArgumentException("Invalid URI format: " + uri);
        }
        return uri.substring(0, colonIndex);
    }
}