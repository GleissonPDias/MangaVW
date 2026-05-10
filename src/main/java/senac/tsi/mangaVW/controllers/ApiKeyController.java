package senac.tsi.mangaVW.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.mangaVW.entities.ApiKey;
import senac.tsi.mangaVW.repositories.ApiKeyRepository;
import senac.tsi.mangaVW.infrastructure.RequireApiKey;

@Tag(name = "API Keys", description = "Endpoints for managing API Keys")
@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;

    @Autowired
    public ApiKeyController(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Operation(summary = "Create a new API Key", description = "Generates a new API key for the specified client.")
    @PostMapping
    public ResponseEntity<ApiKey> generateKey(@RequestParam String clientName) {
        if (clientName == null || clientName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ApiKey newKey = new ApiKey(clientName);
        ApiKey savedKey = apiKeyRepository.save(newKey);
        java.net.URI location = java.net.URI.create("/api-keys/" + savedKey.getId());
        return ResponseEntity.created(location).body(savedKey);
    }

    @Operation(summary = "Delete an API Key", description = "Permanently deletes an existing API Key from the system.")
    @RequireApiKey
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteKey(@PathVariable String key) {
        return apiKeyRepository.findByKeyAndActiveTrue(key)
                .map(apiKey -> {
                    apiKeyRepository.delete(apiKey);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all API Keys", description = "Retrieves a list of all API keys generated in the system. Note: In real production systems, keys are usually masked, but we expose them here for academic demonstration.")
    @RequireApiKey
    @GetMapping
    public ResponseEntity<java.util.List<ApiKey>> getAllKeys() {
        return ResponseEntity.ok(apiKeyRepository.findAll());
    }
}
