package dev.vepo.issues.infra;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
@IfBuildProfile("dev")
public class DatabaseDevSetup {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseDevSetup.class);
    private EntityManager entityManager;

    @Inject
    public DatabaseDevSetup(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void onStart(@Observes StartupEvent ev) throws IOException {
        logger.info("Execuing /dev-import.sql script...");
        this.entityManager.createNativeQuery(new String(DatabaseDevSetup.class.getResourceAsStream("/dev-import.sql").readAllBytes()))
                          .executeUpdate();
        logger.info("/dev-import.sql script executed!!");
    }

}
