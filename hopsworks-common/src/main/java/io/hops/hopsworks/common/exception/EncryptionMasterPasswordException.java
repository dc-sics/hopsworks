package io.hops.hopsworks.common.exception;

public class EncryptionMasterPasswordException extends Exception {
  public EncryptionMasterPasswordException(String message) {
    super(message);
  }
  
  public EncryptionMasterPasswordException(Throwable cause) {
    super(cause);
  }
  
  public EncryptionMasterPasswordException(String message, Throwable cause) {
    super(message, cause);
  }
}
