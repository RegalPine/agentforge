import { describe, it, expect, beforeEach } from "vitest";
import { useTenantStore } from "../stores/tenant-store.js";

describe("tenant-store", () => {
  beforeEach(() => {
    useTenantStore.setState({ tenantId: "default" });
  });

  describe("initial state", () => {
    it("should default to 'default' tenant", () => {
      expect(useTenantStore.getState().tenantId).toBe("default");
    });
  });

  describe("setTenantId", () => {
    it("should switch tenant", () => {
      useTenantStore.getState().setTenantId("tenant-abc");
      expect(useTenantStore.getState().tenantId).toBe("tenant-abc");
    });

    it("should switch back to default", () => {
      useTenantStore.getState().setTenantId("other");
      useTenantStore.getState().setTenantId("default");
      expect(useTenantStore.getState().tenantId).toBe("default");
    });

    it("should handle multiple switches", () => {
      useTenantStore.getState().setTenantId("t1");
      expect(useTenantStore.getState().tenantId).toBe("t1");

      useTenantStore.getState().setTenantId("t2");
      expect(useTenantStore.getState().tenantId).toBe("t2");

      useTenantStore.getState().setTenantId("t3");
      expect(useTenantStore.getState().tenantId).toBe("t3");
    });
  });
});
