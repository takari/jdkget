package io.takari.jdkget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class JdkGetterTest {

  @Test
  public void testRetries() throws Exception {

    DumbTransport t = new DumbTransport(false);
    try {
      JdkGetter.builder()
        .output(IOutput.NULL_OUTPUT)
        .retries(3)
        .transport(t)
        .arch(Arch.NIX_64)
        .outputDirectory(new File(""))
        .build().get();
      fail();
    } catch (IOException e) {
      assertEquals(4, t.tries);
    }
  }

  @Test
  public void testInterrupts() throws Exception {

    JdkGetter b = JdkGetter.builder()
      .output(IOutput.NULL_OUTPUT)
      .retries(3)
      .transport(new SleepingTransport())
      .arch(Arch.NIX_64)
      .outputDirectory(new File(""))
      .build();

    Thread cur = Thread.currentThread();

    new Thread() {
      public void run() {
        try {
          Thread.sleep(300L);
        } catch (InterruptedException e) {
        }
        cur.interrupt();
      }
    }.start();

    try {
      b.get();
      fail();
    } catch (InterruptedException e) {
    }
  }
}
