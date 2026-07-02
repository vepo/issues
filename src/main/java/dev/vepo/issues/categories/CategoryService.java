package dev.vepo.issues.categories;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CategoryService {

    private final CategoryRepository repository;

    @Inject
    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<CategoryResponse> listAll() {
        return repository.findAll()
                         .map(CategoryResponse::load)
                         .toList();
    }
}
