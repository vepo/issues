package dev.vepo.issues.user;

import java.util.Set;

import dev.vepo.issues.auth.AuthProvider;
import dev.vepo.issues.auth.AuthProviderConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "VARCHAR(15)")
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "encoded_password")
    private String encodedPassword;

    @Convert(converter = AuthProviderConverter.class)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Set<Role> roles;

    private boolean deleted;

    @Column(name = "ui_locale", nullable = false, length = 8)
    private String uiLocale = UiLocale.DEFAULT;

    public User() {}

    public User(String username, String name, String email, String encodedPassword, Set<Role> roles) {
        this(username, name, email, encodedPassword, roles, AuthProvider.LOCAL);
    }

    public User(String username, String name, String email, String encodedPassword, Set<Role> roles, AuthProvider authProvider) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.roles = roles;
        this.authProvider = authProvider == null ? AuthProvider.LOCAL : authProvider;
        this.deleted = false;
        this.uiLocale = UiLocale.DEFAULT;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider == null ? AuthProvider.LOCAL : authProvider;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getUiLocale() {
        return uiLocale;
    }

    public void setUiLocale(String uiLocale) {
        this.uiLocale = UiLocale.normalizeOrDefault(uiLocale);
    }

}
