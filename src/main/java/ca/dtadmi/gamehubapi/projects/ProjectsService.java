package ca.dtadmi.gamehubapi.projects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectsService {

    private final ProjectRepository repository;

    public ProjectsService(ProjectRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Project> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Project> page(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Project> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public Project save(Project p) {
        return repository.save(p);
    }
}
