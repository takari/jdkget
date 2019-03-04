package io.takari.jdkget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.takari.jdkget.model.JdkReleases;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JdkGetterTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder(new File("target/")); // trash target and not system

  @Test
  public void testRetries() throws Exception {

    DumbTransport t = new DumbTransport(false);
    try {
      JdkGetter jdkGet = new JdkGetter(t, IOutput.NULL_OUTPUT);
      jdkGet.getJdk(Arch.NIX_64, temporaryFolder.newFolder());
      fail();
    } catch (IOException e) {
      assertEquals(4, t.tries);
    }
  }

  @Test
  public void testInterrupts() throws Exception {

    JdkReleases.get();

    SleepingTransport transport = new SleepingTransport();
    JdkGetter jdkGet = new JdkGetter(transport, IOutput.NULL_OUTPUT);

    Thread cur = Thread.currentThread();

    new Thread() {
      public void run() {
        synchronized (transport) {
          try {
            transport.wait();
          } catch (InterruptedException e) {
          }
        }
        cur.interrupt();
      }
    }.start();

    try {
      jdkGet.getJdk(Arch.NIX_64, temporaryFolder.newFolder());
      fail();
    } catch (InterruptedException e) {
    }
  }
}
