package com.brahim.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
   int status,
   String message,
   String path,

   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
           timezone = "UTC")
   Instant timestamp,
   List<FieldError> errors
) {
    public ApiErrorResponse(int status, String message, String path, Instant timestamp) {
        this(status, message, path, timestamp, null);
    }

    /** Represents a single Bean Validation failure. */
    public record FieldError(String message, String field){}
}
