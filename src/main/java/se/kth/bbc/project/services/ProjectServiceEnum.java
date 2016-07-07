package se.kth.bbc.project.services;

/**
 *
 * @author stig
 */
public enum ProjectServiceEnum {

  ZEPPELIN("Zeppelin"),
  SSH("Ssh"),
  KAFKA("Kafka"),
  //  BIOBANKING("Biobanking"),
  //  CHARON("Charon"),
  JOBS("Jobs");

  private final String readable;

  private ProjectServiceEnum(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
  }

}
