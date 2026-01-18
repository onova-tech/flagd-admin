package tech.onova.flagd_admin_server.domain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.onova.flagd_admin_server.controller.DTOs.FlagConfigRequestDTO;
import tech.onova.flagd_admin_server.controller.DTOs.FlagDTO;
import tech.onova.flagd_admin_server.controller.DTOs.TargetingDTO;
import tech.onova.flagd_admin_server.domain.entity.Source;
import tech.onova.flagd_admin_server.domain.entity.SourceId;
import tech.onova.flagd_admin_server.domain.entity.SourceUri;
import tech.onova.flagd_admin_server.domain.exception.SourceContentNotFoundException;
import tech.onova.flagd_admin_server.domain.repository.SourceRepository;
import tech.onova.flagd_admin_server.domain.service.FlagService;
import tech.onova.flagd_admin_server.domain.service.SourceContentService;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FlagServiceImpl implements FlagService {
    
    private final SourceRepository sourceRepository;
    private final SourceContentService sourceContentService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    public FlagServiceImpl(SourceRepository sourceRepository, SourceContentService sourceContentService) {
        this.sourceRepository = sourceRepository;
        this.sourceContentService = sourceContentService;
    }
    
    @Override
    @Log
    public List<FlagDTO> getFlags(SourceId sourceId) {
        Optional<Source> sourceOpt = sourceRepository.findById(sourceId);
        if (sourceOpt.isEmpty()) {
            throw new SourceContentNotFoundException("Source not found");
        }
        
        Source source = sourceOpt.get();
        String content = sourceContentService.loadContent(source.getUri());
        
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            List<FlagDTO> flags = new ArrayList<>();
            
            JsonNode flagsNode = rootNode.get("flags");
            if (flagsNode != null && flagsNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = flagsNode.fields();
                while (fields.hasNext()) {
                    JsonNode flagNode = fields.next().getValue();
                    FlagDTO flag = parseFlagNode(flagNode);
                    if (flag != null) {
                        flags.add(flag);
                    }
                }
            }
            
            return flags;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse flag configuration", e);
        }
    }
    
    @Override
    @Log
    public FlagDTO getFlag(SourceId sourceId, String flagId) {
        Optional<Source> sourceOpt = sourceRepository.findById(sourceId);
        if (sourceOpt.isEmpty()) {
            throw new SourceContentNotFoundException("Source not found");
        }
        
        Source source = sourceOpt.get();
        String content = sourceContentService.loadContent(source.getUri());
        
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            JsonNode flagsNode = rootNode.get("flags");
            
            if (flagsNode != null && flagsNode.isObject() && flagsNode.has(flagId)) {
                JsonNode flagNode = flagsNode.get(flagId);
                return parseFlagNode(flagNode);
            }
            
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse flag configuration", e);
        }
    }
    
    @Override
    @Log
    public void addOrUpdateFlag(SourceId sourceId, String flagId, FlagConfigRequestDTO request) {
        Optional<Source> sourceOpt = sourceRepository.findById(sourceId);
        if (sourceOpt.isEmpty()) {
            throw new SourceContentNotFoundException("Source not found");
        }
        
        Source source = sourceOpt.get();
        String content = sourceContentService.loadContent(source.getUri());
        
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            ObjectNode flagsNode;
            JsonNode existingFlagsNode = rootNode.get("flags");
            
            if (existingFlagsNode == null || !existingFlagsNode.isObject()) {
                flagsNode = objectMapper.createObjectNode();
                rootNode = objectMapper.createObjectNode();
                ((ObjectNode) rootNode).put("$schema", "https://flagd.dev/schema/v0/flags.json");
                ((ObjectNode) rootNode).put("flags", flagsNode);
            } else {
                flagsNode = (ObjectNode) existingFlagsNode;
            }
            
            ObjectNode flagNode = objectMapper.createObjectNode();
            flagNode.put("key", flagId);
            flagNode.put("state", request.state());
            if (request.name() != null && !request.name().isBlank()) {
                flagNode.put("name", request.name());
            }
            if (request.description() != null && !request.description().isBlank()) {
                flagNode.put("description", request.description());
            }
            if (request.defaultVariant() != null && !request.defaultVariant().isBlank()) {
                flagNode.put("defaultVariant", request.defaultVariant());
            }
            if (request.variants() != null && !request.variants().isEmpty()) {
                flagNode.putPOJO("variants", request.variants());
            }
            if (request.targeting() != null) {
                ObjectNode targetingNode = objectMapper.createObjectNode();
                if (request.targeting().targetingKey() != null && !request.targeting().targetingKey().isEmpty()) {
                    targetingNode.putPOJO("targetingKey", request.targeting().targetingKey());
                }
                if (request.targeting().rule() != null && !request.targeting().rule().isBlank()) {
                    targetingNode.put("rule", request.targeting().rule());
                }
                flagNode.set("targeting", targetingNode);
            }
            
            flagsNode.set(flagId, flagNode);
            
            String updatedContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            sourceContentService.initializeContent(source.getUri(), updatedContent);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to update flag configuration", e);
        }
    }
    
    @Override
    @Log
    public void deleteFlag(SourceId sourceId, String flagId) {
        Optional<Source> sourceOpt = sourceRepository.findById(sourceId);
        if (sourceOpt.isEmpty()) {
            throw new SourceContentNotFoundException("Source not found");
        }
        
        Source source = sourceOpt.get();
        String content = sourceContentService.loadContent(source.getUri());
        
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            JsonNode flagsNode = rootNode.get("flags");
            
            if (flagsNode != null && flagsNode.isObject() && flagsNode.has(flagId)) {
                ((ObjectNode) flagsNode).remove(flagId);
                
                String updatedContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                sourceContentService.initializeContent(source.getUri(), updatedContent);
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete flag", e);
        }
    }
    
    private FlagDTO parseFlagNode(JsonNode flagNode) {
        try {
            String key = flagNode.has("key") ? flagNode.get("key").asText() : null;
            String name = flagNode.has("name") ? flagNode.get("name").asText() : null;
            String description = flagNode.has("description") ? flagNode.get("description").asText() : null;
            String state = flagNode.has("state") ? flagNode.get("state").asText() : null;
            String defaultVariant = flagNode.has("defaultVariant") ? flagNode.get("defaultVariant").asText() : null;
            
            Map<String, Object> variants = null;
            if (flagNode.has("variants") && flagNode.get("variants").isObject()) {
                variants = objectMapper.convertValue(flagNode.get("variants"), Map.class);
            }
            
            TargetingDTO targeting = null;
            if (flagNode.has("targeting") && flagNode.get("targeting").isObject()) {
                JsonNode targetingNode = flagNode.get("targeting");
                Map<String, String> targetingKey = null;
                String rule = null;
                if (targetingNode.has("targetingKey") && targetingNode.get("targetingKey").isObject()) {
                    targetingKey = objectMapper.convertValue(targetingNode.get("targetingKey"), Map.class);
                }
                if (targetingNode.has("rule")) {
                    rule = targetingNode.get("rule").asText();
                }
                targeting = new TargetingDTO(targetingKey, rule);
            }
            
            if (key == null) {
                return null;
            }
            
            return new FlagDTO(key, name, description, state, defaultVariant, variants, targeting);
        } catch (Exception e) {
            return null;
        }
    }
}
