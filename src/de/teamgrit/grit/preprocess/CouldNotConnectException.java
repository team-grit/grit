package de.teamgrit.grit.preprocess;

public class CouldNotConnectException extends Exception {

  /**
   * Indicates that a {@link Connection} to a location could not be established.
   */
  private static final long serialVersionUID = 4802721861573934360L;

  public CouldNotConnectException() {
    // TODO Auto-generated constructor stub
  }

  public CouldNotConnectException(String message) {
    super(message);
    // TODO Auto-generated constructor stub
  }
}
