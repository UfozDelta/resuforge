package com.resumepipeline.api;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Surfaces upstream errors (Gemini, validation, etc.) as structured JSON so the
 * frontend can show a real message instead of a generic 500.
 */
@ControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<Map<String, Object>> handleGeminiClient(ClientException e) {
        log.warn("Gemini client error: {}", e.getMessage());
        HttpStatus status = mapGeminiStatus(e);
        return ResponseEntity.status(status).body(Map.of(
                "source", "gemini",
                "status", status.value(),
                "message", trimGeminiMessage(e.getMessage()),
                "hint", hintForStatus(status)
        ));
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<Map<String, Object>> handleGeminiServer(ServerException e) {
        log.warn("Gemini server error: {}", e.getMessage());
        return ResponseEntity.status(502).body(Map.of(
                "source", "gemini",
                "status", 502,
                "message", trimGeminiMessage(e.getMessage()),
                "hint", "Gemini is having a bad time. Try again in a moment."
        ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleGeminiOther(ApiException e) {
        log.warn("Gemini API error: {}", e.getMessage());
        return ResponseEntity.status(502).body(Map.of(
                "source", "gemini",
                "status", 502,
                "message", trimGeminiMessage(e.getMessage())
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.status(400).body(Map.of(
                "source", "validation",
                "status", 400,
                "message", e.getMessage() == null ? "Bad request" : e.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleState(IllegalStateException e) {
        return ResponseEntity.status(409).body(Map.of(
                "source", "state",
                "status", 409,
                "message", e.getMessage() == null ? "Conflict" : e.getMessage()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        // Unwrap wrapped Gemini errors that escaped via parsing failures.
        Throwable cause = e.getCause();
        if (cause instanceof ClientException ce)  return handleGeminiClient(ce);
        if (cause instanceof ServerException se)  return handleGeminiServer(se);
        if (cause instanceof ApiException ae)     return handleGeminiOther(ae);

        log.error("Unhandled runtime error", e);
        return ResponseEntity.status(500).body(Map.of(
                "source", "server",
                "status", 500,
                "message", e.getMessage() == null ? "Internal error" : e.getMessage()
        ));
    }

    private static HttpStatus mapGeminiStatus(ClientException e) {
        // Gemini SDK exception messages start with the status code, e.g. "429 ..."
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.startsWith("429")) return HttpStatus.TOO_MANY_REQUESTS;
        if (msg.startsWith("401") || msg.startsWith("403")) return HttpStatus.UNAUTHORIZED;
        if (msg.startsWith("400")) return HttpStatus.BAD_REQUEST;
        if (msg.startsWith("404")) return HttpStatus.NOT_FOUND;
        return HttpStatus.BAD_GATEWAY;
    }

    /** Gemini error messages are giant URL-laden walls of text — keep the useful bit. */
    private static String trimGeminiMessage(String raw) {
        if (raw == null) return "Gemini error";
        String s = raw.replaceAll("\\s+", " ").trim();
        int firstLink = s.indexOf("https://");
        if (firstLink > 60) s = s.substring(0, firstLink).trim();
        int firstAsterisk = s.indexOf(" *");
        if (firstAsterisk > 0 && firstAsterisk < s.length() - 1) {
            String head = s.substring(0, firstAsterisk).trim();
            if (head.length() > 30) s = head;
        }
        if (s.length() > 400) s = s.substring(0, 400) + "…";
        return s;
    }

    private static String hintForStatus(HttpStatus s) {
        return switch (s) {
            case TOO_MANY_REQUESTS -> "Gemini free-tier rate limit hit. Wait ~30s and retry, or enable billing in AI Studio.";
            case UNAUTHORIZED -> "Check GEMINI_API_KEY in application-local.yml.";
            case BAD_REQUEST -> "Gemini rejected the request payload.";
            default -> "";
        };
    }
}
