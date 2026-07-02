package dev.vepo.issues.categories.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.categories.CategoryPaths;
import dev.vepo.issues.categories.CategoryResponse;
import dev.vepo.issues.categories.CategoryService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(CategoryPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Category")
public class ListCategoriesEndpoint {

    private final CategoryService categoryService;

    @Inject
    public ListCategoriesEndpoint(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GET
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listCategories", summary = "List ticket categories")
    public List<CategoryResponse> listAll() {
        return categoryService.listAll();
    }
}
