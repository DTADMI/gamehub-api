package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.projects.Project;
import ca.dtadmi.gamehubapi.projects.ProjectsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    private final ProjectsService service;

    public ProjectsController(ProjectsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Project>> list(@PageableDefault(size = 20) Pageable pageable) {
        // Enforce an upper bound to avoid abuse
        int maxSize = 100;
        if (pageable.getPageSize() > maxSize) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.page(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> get(@PathVariable UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
