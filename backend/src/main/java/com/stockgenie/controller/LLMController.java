package com.stockgenie.controller;

import com.stockgenie.dto.LLMRequestDto;
import com.stockgenie.dto.LLMResponseDto;
import com.stockgenie.service.LocalLLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/llm")
public class LLMController {

    @Autowired
    private LocalLLMService localLLMService;

    @PostMapping("/analyze")
    public ResponseEntity<LLMResponseDto> analyzeStock(@RequestBody LLMRequestDto request) {
        try {
            LLMResponseDto response = localLLMService.analyzeStockData(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getLLMStatus() {
        try {
            boolean isAvailable = localLLMService.isLLMAvailable();
            Map<String, Object> status = Map.of(
                "available", isAvailable,
                "model", "mistral:7b",
                "endpoint", "http://localhost:11434",
                "message", isAvailable ? "LLM service is available and ready" : "LLM service is not available - please ensure Ollama is running"
            );
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/test")
    public ResponseEntity<String> testLLM(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.getOrDefault("prompt", "Hello, how are you?");
            String response = localLLMService.testLLM(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("LLM test failed: " + e.getMessage());
        }
    }
}
