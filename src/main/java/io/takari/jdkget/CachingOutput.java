package io.takari.jdkget;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class CachingOutput implements IOutput {

  private List<String> msgs = new ArrayList<>();

  @Override
  public void info(String message) {
    msgs.add("[INFO] " + message);
  }

  @Override
  public void error(String message) {
    msgs.add("[ERROR] " + message);
  }

  @Override
  public void error(String message, Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    error(message + ": " + t + "\n" + sw.toString());
  }

  public void output(PrintStream out) {
    for (String msg : msgs) {
      out.println(msg);
    }
  }

}