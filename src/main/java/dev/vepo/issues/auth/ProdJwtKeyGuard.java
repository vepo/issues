package dev.vepo.issues.auth;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * SEC2: refuse production startup when JWT still uses the committed development
 * keypair.
 */
@ApplicationScoped
public class ProdJwtKeyGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProdJwtKeyGuard.class);

    private static final String DEV_SIGN_KEY = "privateKey.pem";
    private static final String DEV_VERIFY_KEY = "META-INF/resources/publicKey.pem";

    private final String profile;
    private final String signKeyLocation;
    private final String verifyKeyLocation;

    @Inject
    public ProdJwtKeyGuard(@ConfigProperty(name = "quarkus.profile", defaultValue = "prod") String profile,
                           @ConfigProperty(name = "smallrye.jwt.sign.key.location") String signKeyLocation,
                           @ConfigProperty(name = "mp.jwt.verify.publickey.location") String verifyKeyLocation) {
        this.profile = profile;
        this.signKeyLocation = signKeyLocation;
        this.verifyKeyLocation = verifyKeyLocation;
    }

    void onStart(@Observes StartupEvent event) {
        if (!isProdProfile()) {
            return;
        }
        if (usesDevSignKey() || usesDevVerifyKey()) {
            var message = """
                          Production profile must not use the repository development JWT keys. \
                          Set SMALLRYE_JWT_SIGN_KEY_LOCATION and MP_JWT_VERIFY_PUBLICKEY_LOCATION \
                          (or JWKS) to environment-specific material. Bundled privateKey.pem / publicKey.pem are %dev/%test only.
                          """.strip();
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }
    }

    private boolean isProdProfile() {
        return profile != null && profile.contains("prod");
    }

    private boolean usesDevSignKey() {
        return signKeyLocation != null && (signKeyLocation.equals(DEV_SIGN_KEY) || signKeyLocation.endsWith("/" + DEV_SIGN_KEY));
    }

    private boolean usesDevVerifyKey() {
        return verifyKeyLocation != null && verifyKeyLocation.contains("publicKey.pem");
    }
}
