package ca.dtadmi.gamehubapi.repository;

import ca.dtadmi.gamehubapi.BaseIntegrationTest;
import ca.dtadmi.gamehubapi.projects.Project;
import ca.dtadmi.gamehubapi.projects.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectsRepositoryIT extends BaseIntegrationTest {

    @Autowired
    ProjectRepository repository;

    @Test
    void save_and_findBySlug() {
        Project p = new Project();
        p.setSlug("sample-proj");
        p.setName("Sample Project");
        p.setDescription("A sample");
        repository.save(p);

        Optional<Project> found = repository.findBySlug("sample-proj");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Sample Project");
    }
}
