package io.hops.hopsworks.common.exception;

public class CryptoPasswordNotFoundException extends Exception {
  public CryptoPasswordNotFoundException(String message) {
    super(message);
  }

  public CryptoPasswordNotFoundException(Throwable cause) {
    super(cause);
  }
  
  public CryptoPasswordNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
