package senac.tsi.mangaVW.controllers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.mangaVW.entities.ApiKey;
import senac.tsi.mangaVW.repositories.ApiKeyRepository;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import senac.tsi.mangaVW.exceptions.ApiErrorResponse;
import senac.tsi.mangaVW.infrastructure.RateLimit;

@Tag(name = "API Keys", description = "Endpoints for managing API Keys")
@RestController
@RequestMapping("/api-keys")
@ApiResponse(responseCode = "400", description = "Invalid request: Bad parameters or syntax error",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "429", description = "Too Many Requests: Rate limit exceeded", content = @Content)
@ApiResponse(responseCode = "401", description = "Unauthorized: API Key is missing or invalid")
public class ApiKeyController {
    private final ApiKeyRepository apiKeyRepository;
    private final java.util.Map<String, IdempotentCreateResponse> createResponses = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object createIdempotencyLock = new Object();
    
    private record CreateApiKeyFingerprint(String clientName) {}
    private record IdempotentCreateResponse(CreateApiKeyFingerprint requestFingerprint, ApiKey apiKey, java.net.URI location) {}

    @Autowired
    public ApiKeyController(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }
    
    @Operation(summary = "Create a new API Key", description = "Generates a new API key for the specified client.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "API Key created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or missing Idempotency-Key"),
            @ApiResponse(responseCode = "409", description = "Idempotency key already used with a different payload")
    })
    @RateLimit(capacity = 5, minutes = 5) // Evita spam de geração de chaves
    @PostMapping
    public ResponseEntity<ApiKey> generateKey(@RequestParam String clientName,
                                              @io.swagger.v3.oas.annotations.Parameter(description = "Required key used to make repeated create requests idempotent", required = true)
                                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (clientName == null || clientName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        var requestFingerprint = new CreateApiKeyFingerprint(clientName);

        synchronized (createIdempotencyLock) {
            var storedResponse = createResponses.get(idempotencyKey);
            
            if (storedResponse != null) {
                if (!storedResponse.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
                }
                // Retorna a cópia do objeto salvo
                ApiKey copy = new ApiKey(storedResponse.apiKey().getClientName());
                copy.setId(storedResponse.apiKey().getId());
                copy.setKey(storedResponse.apiKey().getKey());
                copy.setActive(storedResponse.apiKey().isActive());
                copy.setCreatedAt(storedResponse.apiKey().getCreatedAt());
                
                return ResponseEntity.created(storedResponse.location()).body(copy);
            }
            
            ApiKey newKey = new ApiKey(clientName);
            ApiKey savedKey = apiKeyRepository.save(newKey);
            java.net.URI location = java.net.URI.create("/api-keys/" + savedKey.getId());
            
            createResponses.put(idempotencyKey, new IdempotentCreateResponse(
                    requestFingerprint,
                    savedKey,
                    location
            ));
            
            return ResponseEntity.created(location).body(savedKey);
        }
    }
    
    @Operation(summary = "Delete an API Key", description = "Permanently deletes an existing API Key from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "API Key deleted successfully"),
            @ApiResponse(responseCode = "404", description = "API Key not found")
    })
    @RateLimit(capacity = 5, minutes = 10)
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List returned successfully")
    })
    @RateLimit()
    @GetMapping
    public ResponseEntity<java.util.List<ApiKey>> getAllKeys() {
        return ResponseEntity.ok(apiKeyRepository.findAll());
    }
}
