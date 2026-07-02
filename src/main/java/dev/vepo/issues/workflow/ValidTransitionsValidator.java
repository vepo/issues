package dev.vepo.issues.workflow;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidTransitionsValidator implements ConstraintValidator<ValidTransitions, CreateWorkflowRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ValidTransitionsValidator.class);

    @Override
    public boolean isValid(CreateWorkflowRequest request, ConstraintValidatorContext context) {
        logger.debug("Validating workflow create request! request={} context={}", request, context);
        if (Objects.isNull(request.statuses())
                || Objects.isNull(request.transitions())
                || request.transitions().stream().anyMatch(t -> Objects.isNull(t.from()) || Objects.isNull(t.to()))) {
            // not validated by this validator
            logger.warn("Invalid request! But it should be handle by @NonNull! request={}", request);
            return true;
        }
        // Check start
        if (!request.statuses().contains(request.start())) {
            logger.warn("Start status is unknown! request={}", request);
            return false;
        }

        // Check transitions
        for (var transition : request.transitions()) {
            if (!request.statuses().contains(transition.from()) || !request.statuses().contains(transition.to())) {
                logger.warn("Transition defining an unknown status! request={}", request);
                return false;
            }
        }

        logger.debug("Valid request! request={}", request);
        return true;
    }
}
