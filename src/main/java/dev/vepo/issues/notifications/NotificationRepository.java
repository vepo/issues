package dev.vepo.issues.notifications;

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

    public Optional<Notification> findById(long id) {
        return this.em.createQuery("FROM Notification n WHERE n.id = :id", Notification.class)
                      .setParameter("id", id)
                      .getResultStream()
                      .findFirst();
    }
}
