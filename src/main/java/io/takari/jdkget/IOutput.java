package io.takari.jdkget;

public interface IOutput {
  
  void info(String message);
  
  void error(String message);
  
  void error(String message, Throwable t);
  
}
