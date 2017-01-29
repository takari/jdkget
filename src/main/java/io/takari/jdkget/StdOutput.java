package io.takari.jdkget;

public class StdOutput implements IOutput {
  
  public static final StdOutput INSTANCE = new StdOutput();
  
  private StdOutput() {
  }
  
  @Override
  public void info(String message) {
    System.out.println(message);
  }

  @Override
  public void error(String message) {
    System.err.println(message);
  }

  @Override
  public void error(String message, Throwable e) {
    System.err.println(message);
    e.printStackTrace(System.err);
  }

}
