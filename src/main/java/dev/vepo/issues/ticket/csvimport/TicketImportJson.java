package dev.vepo.issues.ticket.csvimport;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class TicketImportJson {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};
    private static final TypeReference<List<String>> ERRORS_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    @Inject
    public TicketImportJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String writeHeaders(List<String> headers) {
        return write(headers);
    }

    public List<String> readHeaders(String json) {
        return read(json, STRING_LIST);
    }

    public String writeRawValues(Map<String, String> values) {
        return write(values);
    }

    public Map<String, String> readRawValues(String json) {
        return read(json, STRING_MAP);
    }

    public String writeErrors(List<String> errors) {
        return write(errors);
    }

    public List<String> readErrors(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return read(json, ERRORS_LIST);
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid import JSON payload");
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Unable to serialize import payload");
        }
    }
}
