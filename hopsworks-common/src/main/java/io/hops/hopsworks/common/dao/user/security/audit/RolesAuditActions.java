package io.hops.hopsworks.common.dao.user.security.audit;

public enum RolesAuditActions {

  ADDROLE("ADDED ROLE"),
  REMOVEROLE("REMOVED ROLE"),
  UPDATEROLES("UPDATED ROLES"),
  SUCCESS("SUCCESS"),
  FAILED("FAILED"),
  ALLROLEASSIGNMENTS("ALL");

  private final String value;

  RolesAuditActions(String value) { this.value = value; }

  @Override
  public String toString() { return value; }
}
