package io.hops.hopsworks.common.dao.user.cluster;

public enum RegistrationStatusEnum {
  REGISTERD("Registerd"),
  REGISTRATION_PENDING("Registration pending"),
  UNREGISTRATION_PENDING("Unregistration pending");

  private final String readable;

  private RegistrationStatusEnum(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return this.readable;
  }

}
