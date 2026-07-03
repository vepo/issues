package dev.vepo.issues.categories.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.categories.CategoryPaths;
import dev.vepo.issues.categories.CategoryResponse;
import dev.vepo.issues.categories.CategoryService;
import dev.vepo.issues.categories.UpdateCategoryRequest;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(CategoryPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Category")
public class UpdateCategoryEndpoint {

    private final CategoryService categoryService;

    @Inject
    public UpdateCategoryEndpoint(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({ Role.ADMIN_ROLE })
    @Operation(operationId = "updateCategory", summary = "Update a ticket category")
    public CategoryResponse update(@PathParam("id") long id, @Valid UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }
}
