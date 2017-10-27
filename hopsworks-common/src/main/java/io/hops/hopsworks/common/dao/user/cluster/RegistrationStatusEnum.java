package io.hops.hopsworks.common.dao.user.cluster;

public enum RegistrationStatusEnum {
  REGISTERD("Registerd"),
  REGISTRATION_PENDING("Registration pending"),
  UNREGISTRATION_PENDING("Unregistration pending");

  private final String readable;

  private RegistrationStatusEnum(String readable) {
    this.readable = readable;
  }

  public static RegistrationStatusEnum fromString(String shortName) {
    switch (shortName) {
      case "Registerd":
        return RegistrationStatusEnum.REGISTERD;
      case "Registration pending":
        return RegistrationStatusEnum.REGISTRATION_PENDING;
      case "Unregistration pending":
        return RegistrationStatusEnum.UNREGISTRATION_PENDING;
      default:
        throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
    }
  }

  @Override
  public String toString() {
    return this.readable;
  }

}
