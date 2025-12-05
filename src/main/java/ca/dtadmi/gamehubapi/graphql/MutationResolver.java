package ca.dtadmi.gamehubapi.graphql;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

/**
 * GraphQL mutations. Methods are added as the schema requires.
 * Keeping this class minimal ensures the project compiles while we
 * modernize the backend. Implementations can be added incrementally.
 */
@Controller
@RequiredArgsConstructor
public class MutationResolver {
    // TODO: Wire required services (e.g., GameService, UserRepository) and add methods matching the schema
    // Example:
    // @MutationMapping
    // public SomeType someMutation(@Argument SomeInput input) {
    //     // Implementation here
    // }
}
