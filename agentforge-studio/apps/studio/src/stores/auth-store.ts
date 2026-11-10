import { create } from "zustand";

/**
 * AuthStore - manages authentication state.
 *
 * Handles login/logout, session token persistence,
 * and current user information.
 *
 * Session tokens are persisted in localStorage to survive
 * page refreshes. On app startup, the stored token is
 * restored automatically.
 */

export interface AuthUser {
  userId: string;
  username: string;
  displayName: string;
  tenantId: string;
  roles: string[];
  permissions: string[];
}

interface AuthState {
  /** Current session token (Bearer). */
  token: string | null;
  /** Authenticated user info. */
  user: AuthUser | null;
  /** Whether an auth check is in progress. */
  loading: boolean;
  /** Whether session restoration is in progress. */
  restoring: boolean;
  /** Last login error message, if any. */
  error: string | null;

  /**
   * Attempt to login with username and password.
   * Sends Basic auth to the server, receives a session token.
   */
  login: (username: string, password: string) => Promise<boolean>;

  /**
   * Logout and clear session.
   */
  logout: () => void;

  /**
   * Restore session from localStorage (called on app startup).
   */
  restoreSession: () => void;

  /**
   * Clear any error state.
   */
  clearError: () => void;
}

const TOKEN_KEY = "agentforge.auth.token";
const USER_KEY = "agentforge.auth.user";

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  loading: false,
  restoring: true,
  error: null,

  login: async (username: string, password: string) => {
    set({ loading: true, error: null });

    try {
      const credentials = btoa(`${username}:${password}`);
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          Authorization: `Basic ${credentials}`,
          Accept: "application/json",
        },
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        const message =
          (body as Record<string, string>).message ??
          (response.status === 401
            ? "Invalid username or password"
            : "Login failed");
        set({ loading: false, error: message });
        return false;
      }

      const data = (await response.json()) as {
        token: string;
        user: AuthUser;
      };

      localStorage.setItem(TOKEN_KEY, data.token);
      localStorage.setItem(USER_KEY, JSON.stringify(data.user));

      set({
        token: data.token,
        user: data.user,
        loading: false,
        error: null,
      });

      return true;
    } catch {
      set({
        loading: false,
        error: "Unable to connect to server",
      });
      return false;
    }
  },

  logout: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    set({ token: null, user: null, error: null });
  },

  restoreSession: () => {
    const token = localStorage.getItem(TOKEN_KEY);
    const userJson = localStorage.getItem(USER_KEY);

    if (token && userJson) {
      try {
        const user = JSON.parse(userJson) as AuthUser;
        set({ token, user, error: null, restoring: false });
      } catch {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        set({ restoring: false });
      }
    } else {
      set({ restoring: false });
    }
  },

  clearError: () => set({ error: null }),
}));

/**
 * Check if the user is currently authenticated.
 */
export function selectIsAuthenticated(state: AuthState): boolean {
  return state.token !== null && state.user !== null;
}
