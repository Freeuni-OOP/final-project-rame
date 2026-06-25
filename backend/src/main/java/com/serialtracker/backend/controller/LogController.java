package com.serialtracker.backend.controller;

import com.serialtracker.backend.dto.LogRequest;
import com.serialtracker.backend.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/log")
@CrossOrigin(origins = "http://localhost:5173")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping
    public ResponseEntity<String> createLog(@RequestBody LogRequest request) {
        logService.saveLog(request);
        return ResponseEntity.ok("Log saved");
    }
}