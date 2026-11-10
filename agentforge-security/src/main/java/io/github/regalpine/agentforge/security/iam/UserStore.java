package io.github.regalpine.agentforge.security.iam;

import java.util.List;
import java.util.Optional;

/**
 * SPI for user persistence and lookup.
 * <p>
 * Implementations can back onto in-memory storage, relational databases,
 * LDAP directories, or external identity providers.
 */
public interface UserStore {

    /**
     * Save or update a user.
     *
     * @param user the user to save
     */
    void save(User user);

    /**
     * Find a user by their unique identifier.
     *
     * @param userId the user identifier
     * @return the user if found
     */
    Optional<User> findById(String userId);

    /**
     * Find a user by their username.
     *
     * @param username the username
     * @return the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their email address.
     *
     * @param email the email address
     * @return the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their external IAM subject and issuer.
     *
     * @param externalSubject the external subject identifier
     * @param externalIssuer the external issuer
     * @return the user if found
     */
    Optional<User> findByExternalSubject(
        String externalSubject, String externalIssuer
    );

    /**
     * List all users.
     *
     * @return all users
     */
    List<User> findAll();

    /**
     * Delete a user by their identifier.
     *
     * @param userId the user identifier
     * @return true if the user existed and was removed
     */
    boolean delete(String userId);
}
