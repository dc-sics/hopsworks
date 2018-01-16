package io.hops.hopsworks.common.dao.user.security.audit;

public enum ServiceStatusAction {

  // for adding role by the admin
  SERVICE_ADDED("ADDED ROLE"),
  // for removing role by the admin
  SERVICE_REMOVED("REMOVED ROLE"),

  SERVICES_UPDATED("UPDATED ROLES"),
  
  SUCCESS("SUCCESS"),

  FAILED("FAILED"),

  // for getting all changing services
  ALL_SERVICE_STATUSES("ALL");

  private final String value;

  private ServiceStatusAction(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ServiceStatusAction getServiceStatusAction(String text) {
    if (text != null) {
      for (ServiceStatusAction b : ServiceStatusAction.values()) {
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
