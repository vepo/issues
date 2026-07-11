package dev.vepo.issues.notifications;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class NotificationRepository {

    @PersistenceContext
    private EntityManager em;

    public Notification save(Notification notification) {
        this.em.persist(notification);
        return notification;
    }

    public Stream<Notification> findAll(String username) {
        return this.em.createQuery("FROM Notification n WHERE n.receive.username = :username", Notification.class)
                      .setParameter("username", username)
                      .getResultStream();
    }

    public List<Notification> findPage(String username, int page, int size) {
        return this.em.createQuery("FROM Notification n WHERE n.receive.username = :username ORDER BY n.createdAt DESC, n.id DESC",
                                   Notification.class)
                      .setParameter("username", username)
                      .setFirstResult(page * size)
                      .setMaxResults(size)
                      .getResultList();
    }

    public long countByUsername(String username) {
        return this.em.createQuery("SELECT COUNT(n) FROM Notification n WHERE n.receive.username = :username", Long.class)
                      .setParameter("username", username)
                      .getSingleResult();
    }

    public long countUnreadByUsername(String username) {
        return this.em.createQuery("SELECT COUNT(n) FROM Notification n WHERE n.receive.username = :username AND n.read = false",
                                   Long.class)
                      .setParameter("username", username)
                      .getSingleResult();
    }

    public int markAllReadByUsername(String username) {
        return this.em.createQuery("UPDATE Notification n SET n.read = true WHERE n.receive.username = :username AND n.read = false")
                      .setParameter("username", username)
                      .executeUpdate();
    }

    public Optional<Notification> findById(long id) {
        return this.em.createQuery("FROM Notification n WHERE n.id = :id", Notification.class)
                      .setParameter("id", id)
                      .getResultStream()
                      .findFirst();
    }
}
