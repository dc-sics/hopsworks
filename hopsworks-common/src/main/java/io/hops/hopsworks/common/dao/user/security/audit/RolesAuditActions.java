package io.hops.hopsworks.common.dao.user.security.audit;

public enum RolesAuditActions {

  // for adding role by the admin
  ADDROLE("ADDED ROLE"),
  // for removing role by the admin
  REMOVEROLE("REMOVED ROLE"),

  SUCCESS("SUCCESS"),

  FAILED("FAILED"),

  // for getting all changin rele
  ALLROLEASSIGNMENTS("ALL");

  private final String value;

  private RolesAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static RolesAuditActions getRolesAuditActions(String text) {
    if (text != null) {
      for (RolesAuditActions b : RolesAuditActions.values()) {
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
