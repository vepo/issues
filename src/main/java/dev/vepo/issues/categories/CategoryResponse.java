package dev.vepo.issues.categories;

public record CategoryResponse(long id, String name, String color) {

    public static CategoryResponse load(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }
}
