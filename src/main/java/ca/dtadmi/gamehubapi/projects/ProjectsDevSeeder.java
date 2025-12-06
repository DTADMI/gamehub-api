package ca.dtadmi.gamehubapi.projects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class ProjectsDevSeeder implements CommandLineRunner {

    private final ProjectsService service;
    private final String github;

    public ProjectsDevSeeder(ProjectsService service,
                             @Value("${APP_GITHUB_URL:}") String github) {
        this.service = service;
        this.github = github;
    }

    @Override
    public void run(String... args) {
        if (service.listAll().isEmpty()) {
            Project p1 = new Project();
            p1.setSlug("portfolio-website");
            p1.setName("Portfolio Website");
            p1.setDescription("My personal portfolio built with Next.js");
            p1.setGithubUrl(github);
            p1.setDemoUrl("");
            service.save(p1);

            Project p2 = new Project();
            p2.setSlug("gamehub-frontend");
            p2.setName("GameHub Frontend");
            p2.setDescription("Web hub for games and projects (Next.js)");
            p2.setGithubUrl(github);
            p2.setDemoUrl("");
            service.save(p2);
        }
    }
}
