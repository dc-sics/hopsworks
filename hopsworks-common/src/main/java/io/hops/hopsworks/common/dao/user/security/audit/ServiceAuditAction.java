package io.hops.hopsworks.common.dao.user.security.audit;

public enum ServiceAuditAction {

  // for adding role by the admin
  ROLE_ADDED("ADDED ROLE"),
  // for removing role by the admin
  ROLE_REMOVED("REMOVED ROLE"),

  ROLE_UPDATED("UPDATED ROLES"),
  
  SUCCESS("SUCCESS"),

  FAILED("FAILED"),

  // for getting all changing services
  ALL_SERVICE_STATUSES("ALL");

  private final String value;

  private ServiceAuditAction(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ServiceAuditAction getServiceAuditAction(String text) {
    if (text != null) {
      for (ServiceAuditAction b : ServiceAuditAction.values()) {
        if (text.equalsIgnoreCase(b.value)) {
          return b;
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }
}
