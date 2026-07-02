package dev.vepo.issues.categories;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryEndpoint {
    public static record CategoryResponse(long id, String name, String color) {}

    public static final CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }

    private final CategoryRepository repository;

    @Inject
    public CategoryEndpoint(CategoryRepository repository) {
        this.repository = repository;
    }

    @GET
    public List<CategoryResponse> listAll() {
        return repository.findAll()
                         .map(CategoryEndpoint::toResponse)
                         .toList();
    }
}