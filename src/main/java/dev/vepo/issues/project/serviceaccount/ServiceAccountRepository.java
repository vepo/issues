package dev.vepo.issues.project.serviceaccount;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ServiceAccountRepository {

    private final EntityManager em;

    @Inject
    public ServiceAccountRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<ServiceAccount> findById(long id) {
        return Optional.ofNullable(em.find(ServiceAccount.class, id));
    }

    public List<ServiceAccount> listByProjectId(long projectId) {
        return em.createQuery("""
                              FROM ServiceAccount sa
                              WHERE sa.project.id = :projectId
                              ORDER BY sa.createdAt DESC
                              """, ServiceAccount.class)
                 .setParameter("projectId", projectId)
                 .getResultList();
    }

    @Transactional
    public void persist(ServiceAccount serviceAccount) {
        em.persist(serviceAccount);
    }

    public Optional<ServiceAccountToken> findTokenById(long tokenId) {
        return Optional.ofNullable(em.find(ServiceAccountToken.class, tokenId));
    }

    public Optional<ServiceAccountToken> findActiveTokenByHash(String tokenHash) {
        return em.createQuery("""
                              FROM ServiceAccountToken t
                              JOIN FETCH t.serviceAccount sa
                              JOIN FETCH sa.user
                              WHERE t.tokenHash = :tokenHash
                                AND t.revokedAt IS NULL
                                AND sa.active = true
                              """, ServiceAccountToken.class)
                 .setParameter("tokenHash", tokenHash)
                 .getResultStream()
                 .findFirst();
    }

    @Transactional
    public void persistToken(ServiceAccountToken token) {
        em.persist(token);
    }
}
