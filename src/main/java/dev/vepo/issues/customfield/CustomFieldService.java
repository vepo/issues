package dev.vepo.issues.customfield;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.infra.HtmlSanitizer;
import dev.vepo.issues.infra.PlainTextLength;
import dev.vepo.issues.workflow.WorkflowRepository;

@ApplicationScoped
public class CustomFieldService {

    private static final Logger logger = LoggerFactory.getLogger(CustomFieldService.class);
    public static final int STRING_PLATFORM_MAX = 255;
    public static final int TEXT_MAX = 1200;

    private final CustomFieldRepository fieldRepository;
    private final CustomFieldValueRepository valueRepository;
    private final WorkflowRepository workflowRepository;
    private final HtmlSanitizer htmlSanitizer;

    @Inject
    public CustomFieldService(CustomFieldRepository fieldRepository,
                              CustomFieldValueRepository valueRepository,
                              WorkflowRepository workflowRepository,
                              HtmlSanitizer htmlSanitizer) {
        this.fieldRepository = fieldRepository;
        this.valueRepository = valueRepository;
        this.workflowRepository = workflowRepository;
        this.htmlSanitizer = htmlSanitizer;
    }

    @Transactional
    public List<CustomFieldResponse> listByProject(long projectId) {
        return fieldRepository.listByProjectId(projectId)
                              .stream()
                              .map(CustomFieldResponse::load)
                              .toList();
    }

    @Transactional
    public List<CustomFieldResponse> listByWorkflow(long workflowId) {
        return fieldRepository.listByWorkflowId(workflowId)
                              .stream()
                              .map(field -> toResponse(field, workflowId))
                              .toList();
    }

    @Transactional
    public List<CustomFieldResponse> listInScope(long projectId, long workflowId) {
        return fieldRepository.listEnabledInScope(projectId, workflowId)
                              .stream()
                              .map(field -> toResponse(field, workflowId))
                              .toList();
    }

    @Transactional
    public CustomFieldResponse createForProject(long projectId, long workflowId, CustomFieldRequest request) {
        validateCreateRequest(request);
        assertNoKeyOnProject(projectId, request.key());
        if (fieldRepository.existsKeyCollisionForProject(projectId, workflowId, request.key())) {
            throw new BadRequestException("Custom field key '%s' already exists in project or workflow scope".formatted(request.key()));
        }
        var field = newField(request);
        field.setProjectId(projectId);
        applyTypeConfig(field, request);
        applyEnumOptions(field, request.enumOptions(), true);
        var saved = fieldRepository.save(field);
        logger.info("Created project custom field id={} key={} projectId={}", saved.getId(), saved.getKey(), projectId);
        return CustomFieldResponse.load(saved);
    }

    @Transactional
    public CustomFieldResponse createForWorkflow(long workflowId, CustomFieldRequest request) {
        validateCreateRequest(request);
        assertNoKeyOnWorkflow(workflowId, request.key());
        if (fieldRepository.existsKeyOnProjectsUsingWorkflow(workflowId, request.key())) {
            throw new BadRequestException("Custom field key '%s' already exists on a project using this workflow".formatted(request.key()));
        }
        var field = newField(request);
        field.setWorkflowId(workflowId);
        applyTypeConfig(field, request);
        applyEnumOptions(field, request.enumOptions(), true);
        var saved = fieldRepository.save(field);
        applyStatusRequired(saved, workflowId, request.statusRequired());
        saved = fieldRepository.save(saved);
        logger.info("Created workflow custom field id={} key={} workflowId={}", saved.getId(), saved.getKey(), workflowId);
        return toResponse(saved, workflowId);
    }

    @Transactional
    public CustomFieldResponse updateForProject(long projectId, long fieldId, CustomFieldRequest request) {
        var field = fieldRepository.findByIdAndProjectId(fieldId, projectId)
                                   .orElseThrow(() -> fieldNotFound(fieldId));
        applyMutableFields(field, request);
        applyTypeConfig(field, request);
        applyEnumOptions(field, request.enumOptions(), false);
        return CustomFieldResponse.load(fieldRepository.save(field));
    }

    @Transactional
    public CustomFieldResponse updateForWorkflow(long workflowId, long fieldId, CustomFieldRequest request) {
        var field = fieldRepository.findByIdAndWorkflowId(fieldId, workflowId)
                                   .orElseThrow(() -> fieldNotFound(fieldId));
        applyMutableFields(field, request);
        applyTypeConfig(field, request);
        applyEnumOptions(field, request.enumOptions(), false);
        applyStatusRequired(field, workflowId, request.statusRequired());
        return toResponse(fieldRepository.save(field), workflowId);
    }

    @Transactional
    public void deleteForProject(long projectId, long fieldId) {
        var field = fieldRepository.findByIdAndProjectId(fieldId, projectId)
                                   .orElseThrow(() -> fieldNotFound(fieldId));
        assertDeletable(field);
        fieldRepository.delete(field);
    }

    @Transactional
    public void deleteForWorkflow(long workflowId, long fieldId) {
        var field = fieldRepository.findByIdAndWorkflowId(fieldId, workflowId)
                                   .orElseThrow(() -> fieldNotFound(fieldId));
        assertDeletable(field);
        fieldRepository.delete(field);
    }

    public void assertNoKeyCollisionOnWorkflowChange(long projectId, long newWorkflowId) {
        var collisions = fieldRepository.findCollidingKeys(projectId, newWorkflowId).toList();
        if (!collisions.isEmpty()) {
            throw new BadRequestException("Cannot change workflow: custom field key collision(s): %s".formatted(String.join(", ",
                                                                                                                            collisions)));
        }
    }

    @Transactional
    public void dropStaleTemplateDefaults(long projectId, long workflowId) {
        var inScopeIds = fieldRepository.listEnabledInScope(projectId, workflowId)
                                        .stream()
                                        .map(CustomField::getId)
                                        .toList();
        valueRepository.deleteTemplateValuesNotInScope(projectId, inScopeIds);
    }

    @Transactional
    public List<CustomFieldValueResponse> getTemplateDefaults(long projectId, long workflowId) {
        var inScope = fieldRepository.listEnabledInScope(projectId, workflowId)
                                     .stream()
                                     .collect(Collectors.toMap(CustomField::getId, Function.identity()));
        return valueRepository.listTemplateValuesByProjectId(projectId)
                              .stream()
                              .filter(v -> inScope.containsKey(v.getCustomField().getId()))
                              .map(v -> CustomFieldValueResponse.load(v.getCustomField(),
                                                                      extractValue(v.getCustomField(),
                                                                                   v.getStringValue(),
                                                                                   v.getTextValue(),
                                                                                   v.getIntegerValue(),
                                                                                   v.getBooleanValue(),
                                                                                   v.getEnumOption()),
                                                                      true))
                              .toList();
    }

    @Transactional
    public void setTemplateDefaults(long projectId, long workflowId, List<CustomFieldValueRequest> defaults) {
        valueRepository.deleteTemplateValuesNotInScope(projectId, List.of());
        if (defaults == null || defaults.isEmpty()) {
            return;
        }
        var byKey = inScopeByKey(projectId, workflowId);
        for (var request : defaults) {
            var field = byKey.get(request.key());
            if (field == null) {
                throw new BadRequestException("Custom field key '%s' is not in project scope".formatted(request.key()));
            }
            validateValue(field, request.value(), false);
            var template = new ProjectTicketTemplateCustomValue(projectId, field);
            writeTypedValue(template::clearTypedValues,
                            template::setStringValue,
                            template::setTextValue,
                            template::setIntegerValue,
                            template::setBooleanValue,
                            template::setEnumOption,
                            field,
                            request.value());
            valueRepository.saveTemplateValue(template);
        }
    }

    @Transactional
    public List<CustomFieldValueRequest> resolveCreateValues(long projectId,
                                                             long workflowId,
                                                             List<CustomFieldValueRequest> requested) {
        var merged = new LinkedHashMap<String, CustomFieldValueRequest>();
        for (var template : getTemplateDefaults(projectId, workflowId)) {
            merged.put(template.key(), new CustomFieldValueRequest(template.key(), template.value()));
        }
        if (requested != null) {
            for (var value : requested) {
                merged.put(value.key(), value);
            }
        }
        return new ArrayList<>(merged.values());
    }

    @Transactional
    public List<CustomFieldValueResponse> readValues(long ticketId, long projectId, long workflowId) {
        var inScopeIds = fieldRepository.listEnabledInScope(projectId, workflowId)
                                        .stream()
                                        .map(CustomField::getId)
                                        .collect(Collectors.toSet());
        return valueRepository.listByTicketId(ticketId)
                              .stream()
                              .map(value -> toValueResponse(value,
                                                            inScopeIds.contains(value.getCustomField().getId())
                                                                    && value.getCustomField().isEnabled()))
                              .toList();
    }

    @Transactional
    public Map<Long, List<CustomFieldValueResponse>> readValuesByTicketIds(Collection<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Map.of();
        }
        var valuesByTicket = valueRepository.listByTicketIds(ticketIds)
                                            .stream()
                                            .collect(Collectors.groupingBy(TicketCustomFieldValue::getTicketId,
                                                                           Collectors.mapping(value -> toValueResponse(value,
                                                                                                                       value.getCustomField()
                                                                                                                            .isEnabled()),
                                                                                              Collectors.collectingAndThen(Collectors.toList(),
                                                                                                                           List::copyOf))));
        return Map.copyOf(valuesByTicket);
    }

    @Transactional
    public List<CustomFieldValueResponse> copyCompatibleValues(long ticketId,
                                                               long sourceProjectId,
                                                               long sourceWorkflowId,
                                                               long targetProjectId,
                                                               long targetWorkflowId,
                                                               List<String> warnings) {
        var targetByKey = listInScope(targetProjectId, targetWorkflowId)
                                                                        .stream()
                                                                        .collect(Collectors.toMap(CustomFieldResponse::key, Function.identity()));
        var copied = new ArrayList<CustomFieldValueResponse>();
        for (var source : readValues(ticketId, sourceProjectId, sourceWorkflowId)) {
            if (!source.orphan() && !source.readOnly()) {
                var target = targetByKey.get(source.key());
                var incompatibility = cloneIncompatibility(source, target);
                if (incompatibility == null) {
                    copied.add(new CustomFieldValueResponse(source.key(), target.type(), source.value(), false, false));
                } else {
                    warnings.add("Custom field '%s' %s".formatted(source.key(), incompatibility));
                }
            }
        }
        return copied;
    }

    private String cloneIncompatibility(CustomFieldValueResponse source, CustomFieldResponse target) {
        if (target == null) {
            return "is not available in the target project";
        }
        if (target.type() != source.type()) {
            return "has a different target type";
        }
        if (target.type() == CustomFieldType.ENUM
                && target.enumOptions()
                         .stream()
                         .noneMatch(option -> Objects.equals(option.value(), source.value()))) {
            return "has an invalid target enum value";
        }
        if (!satisfiesCloneConstraints(target, source.value())) {
            return "does not satisfy target constraints";
        }
        return null;
    }

    private boolean satisfiesCloneConstraints(CustomFieldResponse target, Object value) {
        return switch (target.type()) {
            case STRING -> value instanceof String text
                    && text.length() <= (target.stringMaxLength() == null ? STRING_PLATFORM_MAX : target.stringMaxLength());
            case INTEGER -> value instanceof Number number
                    && (target.integerMin() == null || number.longValue() >= target.integerMin())
                    && (target.integerMax() == null || number.longValue() <= target.integerMax());
            case TEXT, BOOLEAN, ENUM -> true;
        };
    }

    @Transactional
    public List<CustomFieldValueChange> applyValuesToTicket(long ticketId,
                                                            long projectId,
                                                            long workflowId,
                                                            List<CustomFieldValueRequest> values,
                                                            boolean validateRequired) {
        var byKey = inScopeByKey(projectId, workflowId);
        if (validateRequired) {
            validateRequired(byKey, values, Map.of());
        }
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        var changes = new ArrayList<CustomFieldValueChange>();
        for (var request : values) {
            var field = byKey.get(request.key());
            if (field == null) {
                throw new BadRequestException("Custom field key '%s' is not in project scope".formatted(request.key()));
            }
            validateValue(field, request.value(), field.isRequired());
            var existing = valueRepository.find(ticketId, field.getId()).orElse(null);
            var oldDisplay = existing == null ? null : formatDisplay(field, extractValue(existing));
            if (isClearValue(request.value())) {
                if (existing != null) {
                    valueRepository.delete(existing);
                    changes.add(new CustomFieldValueChange(field.getKey(), oldDisplay, null));
                }
                continue;
            }
            var entity = existing != null ? existing : new TicketCustomFieldValue(ticketId, field);
            writeTypedValue(entity::clearTypedValues,
                            entity::setStringValue,
                            entity::setTextValue,
                            entity::setIntegerValue,
                            entity::setBooleanValue,
                            entity::setEnumOption,
                            field,
                            request.value());
            valueRepository.save(entity);
            var newDisplay = formatDisplay(field, extractValue(entity));
            if (!Objects.equals(oldDisplay, newDisplay)) {
                changes.add(new CustomFieldValueChange(field.getKey(), oldDisplay, newDisplay));
            }
        }
        return changes;
    }

    public void validateRequired(long projectId, long workflowId, List<CustomFieldValueRequest> values) {
        validateRequired(inScopeByKey(projectId, workflowId), values, Map.of());
    }

    public void validateRequiredForUpdate(long ticketId,
                                          long projectId,
                                          long workflowId,
                                          List<CustomFieldValueRequest> values) {
        var stored = valueRepository.listByTicketId(ticketId)
                                    .stream()
                                    .collect(Collectors.toMap(v -> v.getCustomField().getKey(), Function.identity()));
        validateRequired(inScopeByKey(projectId, workflowId), values, stored);
    }

    public void validateStatusRequired(long ticketId, long projectId, long workflowId, long statusId) {
        var valuesByKey = valueRepository.listByTicketId(ticketId)
                                         .stream()
                                         .collect(Collectors.toMap(v -> v.getCustomField().getKey(), Function.identity()));
        for (var field : fieldRepository.listEnabledInScope(projectId, workflowId)) {
            if (!isRequiredForStatus(field, statusId)) {
                continue;
            }
            var stored = valuesByKey.get(field.getKey());
            if (stored == null || isEmptyStored(field, stored)) {
                throw new BadRequestException("Custom field '%s' is required for this status".formatted(field.getKey()));
            }
        }
    }

    public void validateStatusRequiredForCreate(long projectId,
                                                long workflowId,
                                                long statusId,
                                                List<CustomFieldValueRequest> values) {
        var provided = indexValues(values);
        for (var field : fieldRepository.listEnabledInScope(projectId, workflowId)) {
            if (!isRequiredForStatus(field, statusId)) {
                continue;
            }
            var request = provided.get(field.getKey());
            if (request == null || isClearValue(request.value())) {
                throw new BadRequestException("Custom field '%s' is required for this status".formatted(field.getKey()));
            }
            validateValue(field, request.value(), true);
        }
    }

    /**
     * Validates CSV-imported custom field values for a project scope. Returns
     * human-readable errors instead of throwing so import preview can collect
     * per-row issues.
     */
    public List<String> validateImportValues(long projectId, long workflowId, Map<String, String> valuesByKey) {
        var errors = new ArrayList<String>();
        var byKey = inScopeByKey(projectId, workflowId);
        var provided = valuesByKey == null ? Map.<String, String>of() : valuesByKey;
        var requests = new ArrayList<CustomFieldValueRequest>();
        for (var entry : provided.entrySet()) {
            var field = byKey.get(entry.getKey());
            if (field == null) {
                errors.add("Unknown custom field key: %s".formatted(entry.getKey()));
                continue;
            }
            try {
                validateValue(field, blankToNullValue(entry.getValue()), false);
                if (!isClearValue(entry.getValue())) {
                    requests.add(new CustomFieldValueRequest(entry.getKey(), entry.getValue()));
                }
            } catch (BadRequestException ex) {
                errors.add(ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        try {
            validateRequired(byKey, requests, Map.of());
        } catch (BadRequestException ex) {
            errors.add(ex.getMessage());
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        var workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow != null && workflow.getStart() != null) {
            try {
                validateStatusRequiredForCreate(projectId, workflowId, workflow.getStart().getId(), requests);
            } catch (BadRequestException ex) {
                errors.add(ex.getMessage());
            }
        }
        return errors;
    }

    private static Object blankToNullValue(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateRequired(Map<String, CustomField> byKey,
                                  List<CustomFieldValueRequest> values,
                                  Map<String, TicketCustomFieldValue> storedByKey) {
        var provided = indexValues(values);
        for (var field : byKey.values()) {
            if (!field.isRequired()) {
                continue;
            }
            var request = provided.get(field.getKey());
            if (request != null) {
                if (isClearValue(request.value())) {
                    throw new BadRequestException("Custom field '%s' is required".formatted(field.getKey()));
                }
                validateValue(field, request.value(), true);
                continue;
            }
            var stored = storedByKey.get(field.getKey());
            if (stored == null || isEmptyStored(field, stored)) {
                throw new BadRequestException("Custom field '%s' is required".formatted(field.getKey()));
            }
        }
    }

    private boolean isRequiredForStatus(CustomField field, long statusId) {
        if (!field.isEnabled()) {
            return false;
        }
        if (field.isRequired() && field.isWorkflowOwned()) {
            return true;
        }
        return field.getStatusRequired()
                    .stream()
                    .anyMatch(sr -> Objects.equals(sr.getStatusId(), statusId));
    }

    private Map<String, CustomField> inScopeByKey(long projectId, long workflowId) {
        return fieldRepository.listEnabledInScope(projectId, workflowId)
                              .stream()
                              .collect(Collectors.toMap(CustomField::getKey, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, CustomFieldValueRequest> indexValues(List<CustomFieldValueRequest> values) {
        if (values == null) {
            return Map.of();
        }
        var map = new HashMap<String, CustomFieldValueRequest>();
        for (var value : values) {
            map.put(value.key(), value);
        }
        return map;
    }

    private CustomField newField(CustomFieldRequest request) {
        var field = new CustomField();
        field.setKey(request.key().trim());
        field.setLabel(request.label().trim());
        field.setType(request.type());
        field.setRequired(request.required());
        field.setEnabled(request.enabled() == null || request.enabled());
        field.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return field;
    }

    private void applyMutableFields(CustomField field, CustomFieldRequest request) {
        if (request.label() != null && !request.label().isBlank()) {
            field.setLabel(request.label().trim());
        }
        field.setRequired(request.required());
        if (request.enabled() != null) {
            field.setEnabled(request.enabled());
        }
        if (request.sortOrder() != null) {
            field.setSortOrder(request.sortOrder());
        }
    }

    private void validateCreateRequest(CustomFieldRequest request) {
        if (request.key() == null || request.key().isBlank()) {
            throw new BadRequestException("Custom field key is required");
        }
        if (request.type() == CustomFieldType.ENUM
                && (request.enumOptions() == null || request.enumOptions().isEmpty())) {
            throw new BadRequestException("Enum custom fields require at least one option");
        }
        if (request.type() == CustomFieldType.STRING) {
            validateStringMax(request.stringMaxLength());
        }
        if (request.type() == CustomFieldType.INTEGER) {
            validateIntegerBounds(request.integerMin(), request.integerMax());
        }
    }

    private void applyTypeConfig(CustomField field, CustomFieldRequest request) {
        switch (field.getType()) {
            case STRING -> {
                validateStringMax(request.stringMaxLength());
                field.setStringMaxLength(request.stringMaxLength() == null ? STRING_PLATFORM_MAX : request.stringMaxLength());
                field.setIntegerMin(null);
                field.setIntegerMax(null);
            }
            case TEXT -> {
                field.setStringMaxLength(null);
                field.setIntegerMin(null);
                field.setIntegerMax(null);
            }
            case INTEGER -> {
                validateIntegerBounds(request.integerMin(), request.integerMax());
                field.setStringMaxLength(null);
                field.setIntegerMin(request.integerMin());
                field.setIntegerMax(request.integerMax());
            }
            case BOOLEAN, ENUM -> {
                field.setStringMaxLength(null);
                field.setIntegerMin(null);
                field.setIntegerMax(null);
            }
        }
    }

    private void applyEnumOptions(CustomField field, List<EnumOptionRequest> options, boolean creating) {
        if (field.getType() != CustomFieldType.ENUM) {
            if (options != null && !options.isEmpty()) {
                throw new BadRequestException("Enum options are only allowed for ENUM fields");
            }
            field.getEnumOptions().clear();
            return;
        }
        if (options == null) {
            if (creating) {
                throw new BadRequestException("Enum custom fields require at least one option");
            }
            return;
        }
        var existingByValue = field.getEnumOptions()
                                   .stream()
                                   .collect(Collectors.toMap(CustomFieldEnumOption::getValue, Function.identity(), (a, b) -> a));
        var keepValues = new HashSet<String>();
        var next = new ArrayList<CustomFieldEnumOption>();
        var order = 0;
        for (var option : options) {
            keepValues.add(option.value());
            var existing = existingByValue.get(option.value());
            if (existing != null) {
                existing.setLabel(option.label());
                existing.setSortOrder(option.sortOrder() == null ? order : option.sortOrder());
                next.add(existing);
            } else {
                next.add(new CustomFieldEnumOption(field,
                                                   option.value(),
                                                   option.label(),
                                                   option.sortOrder() == null ? order : option.sortOrder()));
            }
            order++;
        }
        for (var existing : field.getEnumOptions()) {
            if (!keepValues.contains(existing.getValue())) {
                if (existing.getId() != null && fieldRepository.countValuesByEnumOptionId(existing.getId()) > 0) {
                    throw new BadRequestException("Cannot remove enum option '%s': it is in use".formatted(existing.getValue()));
                }
            }
        }
        field.getEnumOptions().clear();
        field.getEnumOptions().addAll(next);
    }

    private void applyStatusRequired(CustomField field, long workflowId, List<String> statusNames) {
        field.getStatusRequired().clear();
        if (statusNames == null || statusNames.isEmpty()) {
            return;
        }
        var workflow = workflowRepository.findById(workflowId)
                                         .orElseThrow(() -> new NotFoundException("Workflow with ID %d does not exist".formatted(workflowId)));
        var byName = workflow.getStatuses()
                             .stream()
                             .collect(Collectors.toMap(s -> s.getName(), Function.identity(), (a, b) -> a));
        for (var name : statusNames) {
            var status = byName.get(name);
            if (status == null) {
                throw new BadRequestException("Status '%s' is not part of workflow %d".formatted(name, workflowId));
            }
            field.getStatusRequired().add(new CustomFieldStatusRequired(field, status.getId()));
        }
    }

    private void assertNoKeyOnProject(long projectId, String key) {
        if (fieldRepository.existsKeyOnProject(projectId, key)) {
            throw new BadRequestException("Custom field key '%s' already exists on this project".formatted(key));
        }
    }

    private void assertNoKeyOnWorkflow(long workflowId, String key) {
        if (fieldRepository.existsKeyOnWorkflow(workflowId, key)) {
            throw new BadRequestException("Custom field key '%s' already exists on this workflow".formatted(key));
        }
    }

    private void assertDeletable(CustomField field) {
        if (fieldRepository.countValuesByFieldId(field.getId()) > 0) {
            throw new BadRequestException("Cannot delete custom field '%s': ticket values exist".formatted(field.getKey()));
        }
    }

    private void validateStringMax(Integer max) {
        if (max != null && (max < 1 || max > STRING_PLATFORM_MAX)) {
            throw new BadRequestException("String max length must be between 1 and %d".formatted(STRING_PLATFORM_MAX));
        }
    }

    private void validateIntegerBounds(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new BadRequestException("Integer min cannot be greater than max");
        }
    }

    private void validateValue(CustomField field, Object value, boolean required) {
        if (isClearValue(value)) {
            if (required) {
                throw new BadRequestException("Custom field '%s' is required".formatted(field.getKey()));
            }
            return;
        }
        switch (field.getType()) {
            case STRING -> {
                var text = asString(value, field.getKey());
                var max = field.getStringMaxLength() == null ? STRING_PLATFORM_MAX
                                                             : Math.min(field.getStringMaxLength(), STRING_PLATFORM_MAX);
                if (text.length() > max) {
                    throw new BadRequestException("Custom field '%s' exceeds max length %d".formatted(field.getKey(), max));
                }
            }
            case TEXT -> {
                var text = asString(value, field.getKey());
                if (PlainTextLength.of(text) > TEXT_MAX) {
                    throw new BadRequestException("Custom field '%s' exceeds max length %d".formatted(field.getKey(), TEXT_MAX));
                }
            }
            case INTEGER -> {
                var number = asInteger(value, field.getKey());
                if (field.getIntegerMin() != null && number < field.getIntegerMin()) {
                    throw new BadRequestException("Custom field '%s' is below minimum %d".formatted(field.getKey(),
                                                                                                    field.getIntegerMin()));
                }
                if (field.getIntegerMax() != null && number > field.getIntegerMax()) {
                    throw new BadRequestException("Custom field '%s' is above maximum %d".formatted(field.getKey(),
                                                                                                    field.getIntegerMax()));
                }
            }
            case BOOLEAN -> asBoolean(value, field.getKey());
            case ENUM -> resolveEnumOption(field, value);
        }
    }

    private void writeTypedValue(Runnable clear,
                                 java.util.function.Consumer<String> setString,
                                 java.util.function.Consumer<String> setText,
                                 java.util.function.Consumer<Integer> setInteger,
                                 java.util.function.Consumer<Boolean> setBoolean,
                                 java.util.function.Consumer<CustomFieldEnumOption> setEnum,
                                 CustomField field,
                                 Object value) {
        clear.run();
        if (isClearValue(value)) {
            return;
        }
        switch (field.getType()) {
            case STRING -> setString.accept(asString(value, field.getKey()));
            case TEXT -> setText.accept(htmlSanitizer.sanitize(asString(value, field.getKey())));
            case INTEGER -> setInteger.accept(asInteger(value, field.getKey()));
            case BOOLEAN -> setBoolean.accept(asBoolean(value, field.getKey()));
            case ENUM -> setEnum.accept(resolveEnumOption(field, value));
        }
    }

    private CustomFieldEnumOption resolveEnumOption(CustomField field, Object value) {
        var optionValue = asString(value, field.getKey());
        return field.getEnumOptions()
                    .stream()
                    .filter(o -> Objects.equals(o.getValue(), optionValue))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Custom field '%s' has invalid enum value '%s'".formatted(field.getKey(),
                                                                                                                         optionValue)));
    }

    private Object extractValue(TicketCustomFieldValue value) {
        return extractValue(value.getCustomField(),
                            value.getStringValue(),
                            value.getTextValue(),
                            value.getIntegerValue(),
                            value.getBooleanValue(),
                            value.getEnumOption());
    }

    private CustomFieldValueResponse toValueResponse(TicketCustomFieldValue value, boolean inScope) {
        return CustomFieldValueResponse.load(value.getCustomField(), extractValue(value), inScope);
    }

    private Object extractValue(CustomField field,
                                String stringValue,
                                String textValue,
                                Integer integerValue,
                                Boolean booleanValue,
                                CustomFieldEnumOption enumOption) {
        return switch (field.getType()) {
            case STRING -> stringValue;
            case TEXT -> textValue;
            case INTEGER -> integerValue;
            case BOOLEAN -> booleanValue;
            case ENUM -> enumOption == null ? null : enumOption.getValue();
        };
    }

    private boolean isEmptyStored(CustomField field, TicketCustomFieldValue value) {
        return extractValue(value) == null;
    }

    private boolean isClearValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isBlank();
        }
        return false;
    }

    private String asString(Object value, String key) {
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        throw new BadRequestException("Custom field '%s' expects a string value".formatted(key));
    }

    private Integer asInteger(Object value, String key) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Custom field '%s' expects an integer value".formatted(key));
            }
        }
        throw new BadRequestException("Custom field '%s' expects an integer value".formatted(key));
    }

    private Boolean asBoolean(Object value, String key) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
                return Boolean.parseBoolean(s);
            }
        }
        throw new BadRequestException("Custom field '%s' expects a boolean value".formatted(key));
    }

    private String formatDisplay(CustomField field, Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private CustomFieldResponse toResponse(CustomField field, long workflowId) {
        if (!field.isWorkflowOwned()) {
            return CustomFieldResponse.load(field);
        }
        var workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow == null) {
            return CustomFieldResponse.load(field);
        }
        var statusNamesById = workflow.getStatuses()
                                      .stream()
                                      .collect(Collectors.toMap(s -> s.getId(), s -> s.getName()));
        var names = field.getStatusRequired()
                         .stream()
                         .map(CustomFieldStatusRequired::getStatusId)
                         .map(statusNamesById::get)
                         .filter(Objects::nonNull)
                         .toList();
        return CustomFieldResponse.load(field, names);
    }

    private NotFoundException fieldNotFound(long fieldId) {
        return new NotFoundException("Custom field with ID %d does not exist".formatted(fieldId));
    }
}
