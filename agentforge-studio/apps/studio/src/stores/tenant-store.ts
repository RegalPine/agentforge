import { create } from "zustand";

/**
 * TenantStore - manages current tenant context.
 *
 * All API calls carry the current tenant ID.
 * Switching tenant invalidates all query caches.
 */
interface TenantState {
  tenantId: string;
  setTenantId: (id: string) => void;
}

export const useTenantStore = create<TenantState>((set) => ({
  tenantId: "default",
  setTenantId: (id) => set({ tenantId: id }),
}));
