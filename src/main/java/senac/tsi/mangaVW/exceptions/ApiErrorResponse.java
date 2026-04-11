package senac.tsi.mangaVW.exceptions;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        @Schema(description = "Time of the error", example = "2026-04-11T18:19:17Z", type = "string", format = "date-time")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}