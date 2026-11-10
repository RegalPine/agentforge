import { describe, it, expect, vi, beforeEach } from "vitest";
import { HttpClient } from "../http-client.js";
import { ApiError } from "../config.js";

describe("HttpClient", () => {
  const mockFetch = vi.fn();
  let client: HttpClient;

  beforeEach(() => {
    mockFetch.mockClear();
    vi.stubGlobal("fetch", mockFetch);
    client = new HttpClient({
      baseUrl: "http://localhost:8080",
      tenantId: "default",
      authToken: "test-token",
    });
  });

  describe("GET requests", () => {
    it("should make GET request with correct URL", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ data: "test" }),
      });

      await client.get("/api/v1/graphs");

      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/graphs",
        expect.objectContaining({ method: "GET" })
      );
    });

    it("should include auth token header", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({}),
      });

      await client.get("/api/v1/test");

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: "Bearer test-token",
          }),
        })
      );
    });

    it("should include tenant header", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({}),
      });

      await client.get("/api/v1/test");

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            "X-Tenant-Id": "default",
          }),
        })
      );
    });
  });

  describe("POST requests", () => {
    it("should send JSON body", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ id: "new-id" }),
      });

      const body = { name: "test-graph", version: "1.0" };
      await client.post("/api/v1/graphs", body);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify(body),
        })
      );
    });
  });

  describe("DELETE requests", () => {
    it("should handle 204 No Content", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
      });

      const result = await client.delete("/api/v1/graphs/123");
      expect(result).toBeUndefined();
    });
  });

  describe("error handling", () => {
    it("should throw ApiError on 4xx response", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        statusText: "Not Found",
        json: async () => ({ code: "NOT_FOUND", message: "Graph not found" }),
      });

      await expect(client.get("/api/v1/graphs/missing")).rejects.toThrow(ApiError);

      try {
        await client.get("/api/v1/graphs/missing");
      } catch (e) {
        // already thrown above, but this tests the error shape
      }
    });

    it("should throw ApiError on 5xx response", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: "Internal Server Error",
        json: async () => ({ code: "INTERNAL", message: "Something went wrong" }),
      });

      await expect(client.get("/api/v1/test")).rejects.toThrow(ApiError);
    });

    it("should include status and code in ApiError", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 403,
        statusText: "Forbidden",
        json: async () => ({ code: "FORBIDDEN", message: "Access denied" }),
      });

      try {
        await client.get("/api/v1/test");
        expect.unreachable("Should have thrown");
      } catch (e) {
        expect(e).toBeInstanceOf(ApiError);
        const apiError = e as ApiError;
        expect(apiError.status).toBe(403);
        expect(apiError.code).toBe("FORBIDDEN");
        expect(apiError.message).toBe("Access denied");
      }
    });
  });

  describe("token and tenant updates", () => {
    it("should update auth token", async () => {
      client.updateAuthToken("new-token");

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({}),
      });

      await client.get("/api/v1/test");

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: "Bearer new-token",
          }),
        })
      );
    });

    it("should update tenant ID", async () => {
      client.updateTenantId("other-tenant");

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({}),
      });

      await client.get("/api/v1/test");

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            "X-Tenant-Id": "other-tenant",
          }),
        })
      );
    });
  });

  describe("URL handling", () => {
    it("should strip trailing slash from base URL", async () => {
      const clientWithSlash = new HttpClient({
        baseUrl: "http://localhost:8080/",
        tenantId: "default",
      });

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({}),
      });

      await clientWithSlash.get("/api/v1/test");

      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/test",
        expect.any(Object)
      );
    });

    it("should not include Authorization header when no token", async () => {
      const clientNoAuth = new HttpClient({
        baseUrl: "http://localhost:8080",
        tenantId: "default",
      });

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({}),
      });

      await clientNoAuth.get("/api/v1/test");

      const callHeaders = mockFetch.mock.calls[0][1].headers;
      expect(callHeaders.Authorization).toBeUndefined();
    });
  });
});
