package dev.vepo.issues.categories.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.categories.CategoryPaths;
import dev.vepo.issues.categories.CategoryResponse;
import dev.vepo.issues.categories.CategoryService;
import dev.vepo.issues.categories.CreateCategoryRequest;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(CategoryPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Category")
public class CreateCategoryEndpoint {

    private final CategoryService categoryService;

    @Inject
    public CreateCategoryEndpoint(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @POST
    @ResponseStatus(201)
    @RolesAllowed({ Role.ADMIN_ROLE })
    @Operation(operationId = "createCategory", summary = "Create a ticket category")
    public CategoryResponse create(@Valid CreateCategoryRequest request) {
        return categoryService.create(request);
    }
}
