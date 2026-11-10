/**
 * ApiClientConfig - configuration for the AgentForge API client.
 */
export interface ApiClientConfig {
  baseUrl: string;
  tenantId: string;
  authToken?: string;
  /** 每次请求读取同一份身份快照，避免登录切换时使用旧配置。 */
  getAuth?: () => { authToken?: string; tenantId: string };
  timeoutMs?: number;
}

/**
 * Structured error body returned by the AgentForge Server API.
 */
export interface ApiErrorBody {
  code: string;
  message: string;
}

/**
 * ApiError - typed error from the API.
 *
 * Carries HTTP status, server error code, and human-readable message.
 * Pages should catch ApiError and display Toast notifications.
 */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }

  /** Whether this is a client-side error (4xx). */
  get isClientError(): boolean {
    return this.status >= 400 && this.status < 500;
  }

  /** Whether this is a server-side error (5xx). */
  get isServerError(): boolean {
    return this.status >= 500;
  }

  /** Whether this is an authentication/authorization error. */
  get isAuthError(): boolean {
    return this.status === 401 || this.status === 403;
  }

  /** Whether this is a not-found error. */
  get isNotFound(): boolean {
    return this.status === 404;
  }

  /** Whether this is an optimistic locking conflict. */
  get isConflict(): boolean {
    return this.status === 409;
  }

  /**
   * Create a user-friendly summary for Toast display.
   */
  toToastMessage(): { title: string; description: string; variant: "destructive" | "warning" } {
    if (this.isAuthError) {
      return {
        title: "Authentication Error",
        description: "Your session has expired. Please log in again.",
        variant: "destructive",
      };
    }
    if (this.isNotFound) {
      return {
        title: "Not Found",
        description: this.message,
        variant: "warning",
      };
    }
    if (this.isConflict) {
      return {
        title: "Conflict",
        description: "This resource was modified by another user. Please reload and try again.",
        variant: "warning",
      };
    }
    if (this.isClientError) {
      return {
        title: `Error ${this.status}`,
        description: this.message,
        variant: "warning",
      };
    }
    return {
      title: "Server Error",
      description: "An unexpected error occurred. Please try again later.",
      variant: "destructive",
    };
  }
}
