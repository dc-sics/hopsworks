package io.hops.hopsworks.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AsyncSystemCommandExecutor {

  private List<String> commandInformation;
  private String adminPassword;
  private ThreadedStreams inputStreamHandler;
  private ThreadedStreams errorStreamHandler;

  private static final Logger logger = LoggerFactory
          .getLogger(AsyncSystemCommandExecutor.class);

  public AsyncSystemCommandExecutor(final List<String> commandInformation) {
    if (commandInformation == null) {
      throw new NullPointerException("The commandInformation is required.");
    }
    this.commandInformation = commandInformation;
    this.adminPassword = null;
  }

  public int executeCommand()
          throws IOException, InterruptedException {
    int exitValue = -99;

    try {
      ProcessBuilder pb = new ProcessBuilder(commandInformation);
      Process process = pb.start();

      // you need this if you're going to write something to the command's input stream
      // (such as when invoking the 'sudo' command, and it prompts you for a password).
      OutputStream stdOutput = process.getOutputStream();

      // i'm currently doing these on a separate line here in case i need to set them to null
      // to get the threads to stop.
      // see http://java.sun.com/j2se/1.5.0/docs/guide/misc/threadPrimitiveDeprecation.html
      InputStream inputStream = process.getInputStream();
      InputStream errorStream = process.getErrorStream();

      // these need to run as java threads to get the standard output and error from the command.
      // the inputstream handler gets a reference to our stdOutput in case we need to write
      // something to it, such as with the sudo command
      inputStreamHandler = new ThreadedStreams(inputStream, stdOutput,
              adminPassword);
      errorStreamHandler = new ThreadedStreams(errorStream);

      // TODO the inputStreamHandler has a nasty side-effect of hanging if the given password is wrong; fix it
      inputStreamHandler.start();
      errorStreamHandler.start();

      // TODO a better way to do this?
//      exitValue = process.waitFor();

    } catch (IOException e) {
      // TODO deal with this here, or just throw it?
      throw e;
    }
    return exitValue;
  }

  /**
   * Get the standard output (stdout) from the command you just exec'd.
   */
  public String getStandardOutputFromCommand() {
    return inputStreamHandler.getOutputBuffer();
  }

  /**
   * Get the standard error (stderr) from the command you just exec'd.
   */
  public String getStandardErrorFromCommand() {
    return errorStreamHandler.getOutputBuffer();
  }

}
