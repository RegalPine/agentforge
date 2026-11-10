package io.github.regalpine.agentforge.server.persistence;

import io.github.regalpine.agentforge.security.iam.*;
import javax.sql.DataSource;
import java.util.*;
import java.util.function.Function;

/** 共享 IAM 权威存储；控制面写操作使用数据库锁，初始化可跨进程串行化。 */
public final class JdbcIamStores {
    private static final String PREFIX = "agentforge_server.";
    private final JdbcAccess db;
    private final UserStore users = new Users();
    private final TenantStore tenants = new Tenants();
    private final RoleStore roles = new Roles();

    public JdbcIamStores(DataSource source) { this(new JdbcAccess(source)); }
    private JdbcIamStores(JdbcAccess db) { this.db = db; }
    public UserStore users() { return users; }
    public TenantStore tenants() { return tenants; }
    public RoleStore roles() { return roles; }

    public IamBootstrap.BootstrapResult bootstrap(String password) {
        return write(tx -> {
            var stores = new JdbcIamStores(tx);
            return new IamBootstrap(stores.tenants, stores.users, stores.roles).execute(password);
        });
    }

    private <T> T write(Function<JdbcAccess, T> work) {
        return db.transaction(tx -> {
            if (tx.query("SELECT lock_id FROM " + PREFIX + "iam_bootstrap_lock WHERE lock_id=1 FOR UPDATE",
                    row -> row.getInt(1)).isEmpty()) throw new IllegalStateException("IAM lock is missing");
            return work.apply(tx);
        });
    }

    private <T> List<T> records(String sql, Class<T> type, Object... parameters) {
        return db.query(sql, row -> PersistenceJson.read(row.getString("record_json"), type), parameters);
    }

    private static void nonBlank(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Identity key is required");
    }

    private final class Users implements UserStore {
        @Override public void save(User user) {
            nonBlank(user.userId()); nonBlank(user.username());
            String json = PersistenceJson.write(user);
            write(tx -> {
                if (tx.update("UPDATE " + PREFIX + "iam_user SET username=?,email=?,external_subject=?,external_issuer=?,record_json=? WHERE user_id=?",
                        user.username(), user.email(), user.externalSubject(), user.externalIssuer(), json, user.userId()) == 0) {
                    tx.update("INSERT INTO " + PREFIX + "iam_user(user_id,username,email,external_subject,external_issuer,record_json) VALUES (?,?,?,?,?,?)",
                        user.userId(), user.username(), user.email(), user.externalSubject(), user.externalIssuer(), json);
                }
                return null;
            });
        }
        @Override public Optional<User> findById(String id) {
            return records("SELECT record_json FROM " + PREFIX + "iam_user WHERE user_id=?", User.class, id).stream().findFirst();
        }
        @Override public Optional<User> findByUsername(String name) {
            return records("SELECT record_json FROM " + PREFIX + "iam_user WHERE username=?", User.class, name).stream().findFirst();
        }
        @Override public Optional<User> findByEmail(String email) {
            if (email == null || email.isBlank()) return Optional.empty();
            return records("SELECT record_json FROM " + PREFIX + "iam_user WHERE email=? ORDER BY user_id", User.class, email).stream().findFirst();
        }
        @Override public Optional<User> findByExternalSubject(String subject, String issuer) {
            if (subject == null || issuer == null) return Optional.empty();
            return records("SELECT record_json FROM " + PREFIX + "iam_user WHERE external_subject=? AND external_issuer=?",
                User.class, subject, issuer).stream().findFirst();
        }
        @Override public List<User> findAll() { return records("SELECT record_json FROM " + PREFIX + "iam_user ORDER BY user_id", User.class); }
        @Override public boolean delete(String id) {
            return write(tx -> tx.update("DELETE FROM " + PREFIX + "iam_user WHERE user_id=?", id) == 1);
        }
    }

    private final class Tenants implements TenantStore {
        @Override public void save(Tenant tenant) {
            nonBlank(tenant.tenantId()); nonBlank(tenant.name());
            String json = PersistenceJson.write(tenant);
            write(tx -> {
                if (tx.update("UPDATE " + PREFIX + "iam_tenant SET tenant_name=?,record_json=? WHERE tenant_id=?",
                        tenant.name(), json, tenant.tenantId()) == 0) {
                    tx.update("INSERT INTO " + PREFIX + "iam_tenant(tenant_id,tenant_name,record_json) VALUES (?,?,?)",
                        tenant.tenantId(), tenant.name(), json);
                }
                return null;
            });
        }
        @Override public Optional<Tenant> findById(String id) {
            return records("SELECT record_json FROM " + PREFIX + "iam_tenant WHERE tenant_id=?", Tenant.class, id).stream().findFirst();
        }
        @Override public Optional<Tenant> findByName(String name) {
            return records("SELECT record_json FROM " + PREFIX + "iam_tenant WHERE tenant_name=?", Tenant.class, name).stream().findFirst();
        }
        @Override public List<Tenant> findAll() { return records("SELECT record_json FROM " + PREFIX + "iam_tenant ORDER BY tenant_id", Tenant.class); }
        @Override public List<Tenant> findByOrganization(String id) {
            if (id == null) return List.of();
            return findAll().stream().filter(t -> id.equals(t.settings().get("organizationId"))).toList();
        }
        @Override public boolean delete(String id) {
            return write(tx -> {
                tx.update("DELETE FROM " + PREFIX + "iam_role_assignment WHERE tenant_scope=?", id);
                return tx.update("DELETE FROM " + PREFIX + "iam_tenant WHERE tenant_id=?", id) == 1;
            });
        }
    }

    private final class Roles implements RoleStore {
        @Override public void saveRole(Role role) {
            nonBlank(role.roleId());
            String json = PersistenceJson.write(role);
            write(tx -> {
                String scope = role.isGlobal() ? null : role.tenantId();
                Role existing = new JdbcIamStores(tx).roles.findRoleById(role.roleId()).orElse(null);
                if (existing != null && !Objects.equals(existing.isGlobal() ? null : existing.tenantId(), scope)) {
                    throw new IllegalArgumentException("Role scope is immutable");
                }
                if (tx.update("UPDATE " + PREFIX + "iam_role SET tenant_id=?,record_json=? WHERE role_id=?", scope, json, role.roleId()) == 0) {
                    tx.update("INSERT INTO " + PREFIX + "iam_role(role_id,tenant_id,record_json) VALUES (?,?,?)", role.roleId(), scope, json);
                }
                return null;
            });
        }
        @Override public Optional<Role> findRoleById(String id) {
            return records("SELECT record_json FROM " + PREFIX + "iam_role WHERE role_id=?", Role.class, id).stream().findFirst();
        }
        @Override public List<Role> findRolesByTenant(String id) {
            return records("SELECT record_json FROM " + PREFIX + "iam_role WHERE tenant_id=? ORDER BY role_id", Role.class, id);
        }
        @Override public List<Role> findGlobalRoles() {
            return records("SELECT record_json FROM " + PREFIX + "iam_role WHERE tenant_id IS NULL ORDER BY role_id", Role.class);
        }
        @Override public List<Role> findAllRoles() { return records("SELECT record_json FROM " + PREFIX + "iam_role ORDER BY role_id", Role.class); }
        @Override public boolean deleteRole(String id) {
            return write(tx -> tx.update("DELETE FROM " + PREFIX + "iam_role WHERE role_id=?", id) == 1);
        }
        @Override public void assignRole(RoleAssignment assignment) {
            write(tx -> {
                var stores = new JdbcIamStores(tx);
                Role role = stores.roles.findRoleById(assignment.roleId()).orElseThrow(() -> new IllegalArgumentException("Unknown role"));
                if (!role.isGlobal() && !Objects.equals(role.tenantId(), assignment.tenantId())) {
                    throw new IllegalArgumentException("Role does not belong to assignment tenant");
                }
                if (!assignment.isGlobal() && stores.tenants.findById(assignment.tenantId()).isEmpty()) {
                    throw new IllegalArgumentException("Unknown tenant");
                }
                String scope = assignment.isGlobal() ? "" : assignment.tenantId();
                var found = tx.query("SELECT user_id FROM " + PREFIX + "iam_role_assignment WHERE user_id=? AND role_id=? AND tenant_scope=?",
                    row -> row.getString(1), assignment.userId(), assignment.roleId(), scope);
                if (found.isEmpty()) tx.update("INSERT INTO " + PREFIX + "iam_role_assignment(user_id,role_id,tenant_scope) VALUES (?,?,?)",
                    assignment.userId(), assignment.roleId(), scope);
                return null;
            });
        }
        @Override public boolean removeRoleAssignment(String userId, String roleId, String tenantId) {
            return write(tx -> tx.update("DELETE FROM " + PREFIX + "iam_role_assignment WHERE user_id=? AND role_id=? AND tenant_scope=?",
                userId, roleId, tenantId == null || tenantId.isBlank() ? "" : tenantId) == 1);
        }
        @Override public Set<RoleAssignment> getAssignmentsForUserInTenant(String userId, String tenantId) {
            return assignments(" WHERE user_id=? AND (tenant_scope='' OR tenant_scope=?)", userId, tenantId);
        }
        @Override public Set<RoleAssignment> getAllAssignmentsForUser(String userId) {
            return assignments(" WHERE user_id=?", userId);
        }
        private Set<RoleAssignment> assignments(String where, Object... parameters) {
            return Set.copyOf(db.query("SELECT user_id,role_id,tenant_scope FROM " + PREFIX + "iam_role_assignment" + where,
                row -> new RoleAssignment(row.getString(1), row.getString(2), row.getString(3).isEmpty() ? null : row.getString(3)), parameters));
        }
    }
}
