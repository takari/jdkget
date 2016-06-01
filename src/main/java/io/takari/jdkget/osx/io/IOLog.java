/*-
 * Copyright (C) 2009 Erik Larsson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package io.takari.jdkget.osx.io;

/**
 * Logging class for the I/O package.
 *
 * @author Erik Larsson
 */
class IOLog {
  /** The default setting for the 'trace' log level. */
  public static boolean defaultTrace = false;

  /** The default setting for the 'debug' log level. */
  public static boolean defaultDebug = false;

  /** The current setting of the 'trace' log level for this instance. */
  public boolean trace = defaultTrace;

  /** The current setting of the 'debug' log level for this instance. */
  public boolean debug = defaultDebug;

  private IOLog() {}

  /** Emits a 'debug' level message. */
  public void debug(String message) {
    if (debug)
      System.err.println("DEBUG: " + message);
  }

  /**
   * Free form trace level log message.
   * @param msg the message to emit.
   */
  public void trace(String msg) {
    if (trace)
      System.err.println("TRACE: " + msg);
  }

  /**
   * Called upon method entry, and generates a trace level message starting
   * with "ENTER: ".
   *
   * @param methodName the name of the method. <code>null</code> indicates a
   * constructor and the message will be formatted accordingly.
   * @param args the method/constructor's arguments.
   */
  public void traceEnter(Object... args) {
    if (trace) {
      final StackTraceElement ste =
        Thread.currentThread().getStackTrace()[2];
      final String className = ste.getClass().getSimpleName();
      final String methodName = ste.getMethodName();

      StringBuilder sb = new StringBuilder("ENTER: ");
      sb.append(className);
      if (methodName != null)
        sb.append(".").append(methodName);
      sb.append("(");
      for (int i = 0; i < args.length; ++i) {
        if (i != 0) {
          sb.append(", ");
        }
        sb.append(args[i]);
      }
      sb.append(")");

      System.err.println(sb.toString());
    }
  }

  /**
   * Called upon method exit, and generates a trace level message starting
   * with "LEAVE: ".
   *
   * @param methodName the name of the method. <code>null</code> indicates a
   * constructor and the message will be formatted accordingly.
   * @param args the method/constructor's arguments.
   */
  public void traceLeave(Object... args) {
    if (trace) {
      final StackTraceElement ste =
        Thread.currentThread().getStackTrace()[2];
      final String className = ste.getClass().getSimpleName();
      final String methodName = ste.getMethodName();

      StringBuilder sb = new StringBuilder("LEAVE: ");
      sb.append(className);
      if (methodName != null)
        sb.append(".").append(methodName);
      sb.append("(");
      for (int i = 0; i < args.length; ++i) {
        if (i != 0) {
          sb.append(", ");
        }
        sb.append(args[i]);
      }
      /*
      if(retval != null)
          sb.append("): ").append(retval);
      else*/
      sb.append(")");

      System.err.println(sb.toString());
    }
  }

  /**
   * Called before a method returns with a value.
   * @param retval the value returned.
   */
  public void traceReturn(Object retval) {
    if (trace)
      System.err.println("RETURN: " + retval);
  }

  /**
   * Returns an IOLog instance for a specific class.
   *
   * @param cls the class for which the instance should be valid.
   * @return an IOLog instance.
   */
  public static IOLog getInstance() {
    return new IOLog();
  }

  /*
  public static void traceLeaveVoid(String methodName, Object... args) {
      if(trace) {
          traceLeave(methodName, null, args);
      }
  }
  */
}
