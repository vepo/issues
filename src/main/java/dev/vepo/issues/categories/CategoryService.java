package dev.vepo.issues.categories;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

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

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (repository.findByName(request.name()).isPresent()) {
            throw new BadRequestException("Category already exists! name=%s".formatted(request.name()));
        }
        return CategoryResponse.load(repository.save(new Category(request.name(), request.color())));
    }

    @Transactional
    public CategoryResponse update(long id, UpdateCategoryRequest request) {
        var category = repository.findById(id)
                                 .orElseThrow(() -> new NotFoundException("Category not found! id=%d".formatted(id)));
        repository.findByName(request.name())
                  .filter(existing -> !existing.getId().equals(id))
                  .ifPresent(existing -> {
                      throw new BadRequestException("Category already exists! name=%s".formatted(request.name()));
                  });
        category.setName(request.name());
        category.setColor(request.color());
        return CategoryResponse.load(category);
    }

    @Transactional
    public void delete(long id) {
        var category = repository.findById(id)
                                 .orElseThrow(() -> new NotFoundException("Category not found! id=%d".formatted(id)));
        if (repository.countTicketsByCategoryId(id) > 0) {
            throw new BadRequestException("Category cannot be deleted while tickets reference it");
        }
        if (repository.countProjectsByTemplateCategoryId(id) > 0) {
            throw new BadRequestException("Category cannot be deleted while project ticket templates reference it");
        }
        repository.delete(category);
    }
}
