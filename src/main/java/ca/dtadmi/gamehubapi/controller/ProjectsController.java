package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.projects.Project;
import ca.dtadmi.gamehubapi.projects.ProjectsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    private final ProjectsService service;

    public ProjectsController(ProjectsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Project>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> get(@PathVariable UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
