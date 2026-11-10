import { describe, it, expect, vi, beforeEach } from "vitest";
import { useAuthStore, selectIsAuthenticated } from "../stores/auth-store.js";

describe("auth-store", () => {
  beforeEach(() => {
    // Reset store state
    useAuthStore.setState({
      token: null,
      user: null,
      loading: false,
      restoring: true,
      error: null,
    });
  });

  describe("initial state", () => {
    it("should have null token and user", () => {
      const state = useAuthStore.getState();
      expect(state.token).toBeNull();
      expect(state.user).toBeNull();
    });

    it("should not be authenticated initially", () => {
      const state = useAuthStore.getState();
      expect(selectIsAuthenticated(state)).toBe(false);
    });
  });

  describe("login", () => {
    it("should login successfully and store token", async () => {
      const mockUser = {
        userId: "u1",
        username: "admin",
        displayName: "Admin",
        tenantId: "default",
        roles: ["sys-admin"],
        permissions: ["admin:system"],
      };

      vi.mocked(globalThis.fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ token: "test-token-123", user: mockUser }),
      } as Response);

      const result = await useAuthStore.getState().login("admin", "password");

      expect(result).toBe(true);
      const state = useAuthStore.getState();
      expect(state.token).toBe("test-token-123");
      expect(state.user).toEqual(mockUser);
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
    });

    it("should set error on failed login", async () => {
      vi.mocked(globalThis.fetch).mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ message: "Invalid credentials" }),
      } as Response);

      const result = await useAuthStore.getState().login("admin", "wrong");

      expect(result).toBe(false);
      const state = useAuthStore.getState();
      expect(state.token).toBeNull();
      expect(state.error).toBe("Invalid credentials");
    });

    it("should handle network error", async () => {
      vi.mocked(globalThis.fetch).mockRejectedValueOnce(new Error("Network error"));

      const result = await useAuthStore.getState().login("admin", "password");

      expect(result).toBe(false);
      const state = useAuthStore.getState();
      expect(state.error).toBe("Unable to connect to server");
    });

    it("should send Basic auth header", async () => {
      vi.mocked(globalThis.fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ token: "t", user: {} }),
      } as Response);

      await useAuthStore.getState().login("admin", "pass");

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/auth/login",
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: expect.stringContaining("Basic "),
          }),
        })
      );
    });

    it("should persist token to localStorage on success", async () => {
      const mockUser = {
        userId: "u1",
        username: "admin",
        displayName: "Admin",
        tenantId: "default",
        roles: [],
        permissions: [],
      };

      vi.mocked(globalThis.fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ token: "persisted-token", user: mockUser }),
      } as Response);

      await useAuthStore.getState().login("admin", "password");

      expect(localStorage.getItem("agentforge.auth.token")).toBe("persisted-token");
    });
  });

  describe("logout", () => {
    it("should clear token and user", () => {
      useAuthStore.setState({
        token: "some-token",
        user: {
          userId: "u1",
          username: "admin",
          displayName: "Admin",
          tenantId: "default",
          roles: [],
          permissions: [],
        },
      });

      useAuthStore.getState().logout();

      const state = useAuthStore.getState();
      expect(state.token).toBeNull();
      expect(state.user).toBeNull();
    });

    it("should remove from localStorage", () => {
      localStorage.setItem("agentforge.auth.token", "token");
      localStorage.setItem("agentforge.auth.user", "{}");

      useAuthStore.getState().logout();

      expect(localStorage.getItem("agentforge.auth.token")).toBeNull();
      expect(localStorage.getItem("agentforge.auth.user")).toBeNull();
    });
  });

  describe("restoreSession", () => {
    it("should restore session from localStorage", () => {
      const mockUser = {
        userId: "u1",
        username: "admin",
        displayName: "Admin",
        tenantId: "default",
        roles: [],
        permissions: [],
      };

      localStorage.setItem("agentforge.auth.token", "restored-token");
      localStorage.setItem("agentforge.auth.user", JSON.stringify(mockUser));

      useAuthStore.getState().restoreSession();

      const state = useAuthStore.getState();
      expect(state.token).toBe("restored-token");
      expect(state.user).toEqual(mockUser);
      expect(state.restoring).toBe(false);
    });

    it("should handle missing localStorage data", () => {
      useAuthStore.getState().restoreSession();

      const state = useAuthStore.getState();
      expect(state.token).toBeNull();
      expect(state.user).toBeNull();
      expect(state.restoring).toBe(false);
    });

    it("should handle corrupted localStorage JSON", () => {
      localStorage.setItem("agentforge.auth.token", "token");
      localStorage.setItem("agentforge.auth.user", "invalid-json{");

      useAuthStore.getState().restoreSession();

      const state = useAuthStore.getState();
      expect(state.token).toBeNull();
      expect(state.restoring).toBe(false);
    });
  });

  describe("selectIsAuthenticated", () => {
    it("should return true when token and user exist", () => {
      expect(
        selectIsAuthenticated({
          token: "t",
          user: { userId: "u1", username: "a", displayName: "A", tenantId: "d", roles: [], permissions: [] },
          loading: false,
          restoring: false,
          error: null,
          login: async () => false,
          logout: () => {},
          restoreSession: () => {},
          clearError: () => {},
        })
      ).toBe(true);
    });

    it("should return false when token is null", () => {
      expect(
        selectIsAuthenticated({
          token: null,
          user: null,
          loading: false,
          restoring: false,
          error: null,
          login: async () => false,
          logout: () => {},
          restoreSession: () => {},
          clearError: () => {},
        })
      ).toBe(false);
    });
  });

  describe("clearError", () => {
    it("should clear error state", () => {
      useAuthStore.setState({ error: "some error" });
      useAuthStore.getState().clearError();
      expect(useAuthStore.getState().error).toBeNull();
    });
  });
});
