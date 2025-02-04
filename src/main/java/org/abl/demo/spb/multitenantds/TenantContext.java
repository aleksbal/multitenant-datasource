package org.abl.demo.spb.multitenantds;

public class TenantContext {
  private static final ThreadLocal<String> tenantHolder = new ThreadLocal<>();

  public static void setTenant(String tenantId) {
    tenantHolder.set(tenantId);
  }

  public static String getTenant() {
    return tenantHolder.get();
  }

  public static void clear() {
    tenantHolder.remove();
  }
}
