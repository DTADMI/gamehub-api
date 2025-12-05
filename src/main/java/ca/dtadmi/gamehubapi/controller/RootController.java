package ca.dtadmi.gamehubapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        return ResponseEntity.ok(
                Map.of(
                        "name", "GameHub",
                        "status", "ok",
                        "message", "Welcome to GameHub API"
                )
        );
    }
}
