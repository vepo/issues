package dev.vepo.issues.categories.delete;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.categories.CategoryPaths;
import dev.vepo.issues.categories.CategoryService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(CategoryPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Category")
public class DeleteCategoryEndpoint {

    private final CategoryService categoryService;

    @Inject
    public DeleteCategoryEndpoint(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({ Role.ADMIN_ROLE })
    @Operation(operationId = "deleteCategory", summary = "Delete a ticket category")
    public void delete(@PathParam("id") long id) {
        categoryService.delete(id);
    }
}
