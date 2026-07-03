package dev.vepo.issues.categories;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CategoryRepository {
    @PersistenceContext
    private EntityManager em;

    public Optional<Category> findById(Long id) {
        return em.createQuery("FROM Category WHERE id = :id", Category.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Stream<Category> findAll() {
        return em.createQuery("FROM Category", Category.class)
                 .getResultStream();
    }

    public Optional<Category> findByName(String name) {
        return em.createQuery("FROM Category WHERE lower(name) = lower(:name)", Category.class)
                 .setParameter("name", name)
                 .getResultStream()
                 .findFirst();
    }

    public Category save(Category category) {
        em.persist(category);
        return category;
    }
}