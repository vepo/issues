package dev.vepo.issues.ticket.search.saved;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SavedQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void persist(SavedQuery savedQuery) {
        em.persist(savedQuery);
    }

    @Transactional
    public SavedQuery merge(SavedQuery savedQuery) {
        return em.merge(savedQuery);
    }

    @Transactional
    public void delete(SavedQuery savedQuery) {
        em.remove(em.contains(savedQuery) ? savedQuery : em.merge(savedQuery));
    }

    public Optional<SavedQuery> findById(long id) {
        return em.createQuery("FROM SavedQuery WHERE id = :id", SavedQuery.class)
                 .setParameter("id", id)
                 .getResultStream()
                 .findFirst();
    }

    public Optional<SavedQuery> findBySlug(String slug) {
        return em.createQuery("FROM SavedQuery WHERE slug = :slug", SavedQuery.class)
                 .setParameter("slug", slug)
                 .getResultStream()
                 .findFirst();
    }

    public List<SavedQuery> findByOwnerId(long ownerId) {
        return em.createQuery("FROM SavedQuery WHERE owner.id = :ownerId ORDER BY updatedAt DESC", SavedQuery.class)
                 .setParameter("ownerId", ownerId)
                 .getResultList();
    }

    public List<SavedQuery> findShowAtHomeByOwnerId(long ownerId) {
        return em.createQuery("""
                              FROM SavedQuery
                              WHERE owner.id = :ownerId AND showAtHome = true
                              ORDER BY name ASC
                              """, SavedQuery.class)
                 .setParameter("ownerId", ownerId)
                 .getResultList();
    }
}
