package ca.dtadmi.gamehubapi.repository;

import ca.dtadmi.gamehubapi.BaseIntegrationTest;
import ca.dtadmi.gamehubapi.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void canCreateAndFindByEmail() {
        User u = new User();
        u.setUsername("tester1");
        u.setEmail("tester1@example.com");
        u.setPassword("hashed");
        userRepository.save(u);

        Optional<User> byEmail = userRepository.findByEmail("tester1@example.com");
        assertTrue(byEmail.isPresent());
        assertEquals("tester1", byEmail.get().getUsername());
    }

    @Test
    void uniqueEmailConstraintEnforced() {
        User u1 = new User();
        u1.setUsername("uniqueA");
        u1.setEmail("dupe@example.com");
        u1.setPassword("x");
        userRepository.saveAndFlush(u1);

        User u2 = new User();
        u2.setUsername("uniqueB");
        u2.setEmail("dupe@example.com");
        u2.setPassword("y");
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(u2));
    }
}
