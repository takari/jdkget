package io.takari.jdkget;

public class NullOutput implements IOutput {
  @Override
  public void info(String message) {}

  @Override
  public void error(String message) {}
}