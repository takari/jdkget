package io.takari.jdkget;

public class StdOutput implements IOutput {

  @Override
  public void info(String message) {
    System.out.println(message);
  }

  @Override
  public void error(String message) {
    System.err.println(message);
  }

}
