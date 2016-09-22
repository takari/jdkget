package io.takari.jdkget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import io.takari.jdkget.JdkGetter.JdkVersion;

public class JdkGetterTest {

  @Test
  public void testRetries() throws IOException {

    DumbTransport t = new DumbTransport(false);
    try {
      JdkGetter.builder()
        .output(new NullOutput())
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

  public static class DumbTransport implements ITransport {
    private boolean valid;
    private int tries;

    public DumbTransport(boolean valid) {
      this.valid = valid;
      tries = 0;
    }

    @Override
    public void downloadJdk(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException {
      jdkImage.createNewFile();
      tries++;
    }

    @Override
    public boolean validate(Arch arch, JdkVersion jdkVersion, File jdkImage, IOutput output) throws IOException {
      return valid;
    }

    @Override
    public File getImageFile(File parent, Arch arch, JdkVersion version) throws IOException {
      return new File("image");
    }
  }

  public static class NullOutput implements IOutput {
    @Override
    public void info(String message) {}

    @Override
    public void error(String message) {}
  }
}
