package io.takari.jdkget;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import io.takari.jdkget.model.JCE;
import io.takari.jdkget.model.BinaryType;
import io.takari.jdkget.model.JdkBinary;
import io.takari.jdkget.model.JdkRelease;
import io.takari.jdkget.model.JdkReleases;
import io.takari.jdkget.model.JdkVersion;

public class MirrorRemote {

  private final ITransport transport;
  private final int threads;
  private final boolean silent;

  public MirrorRemote(ITransport transport, int threads, boolean silent) {
    this.transport = transport;
    this.threads = threads;
    this.silent = silent;
  }

  public void mirrorRemote(JdkReleases rels, String vfrom, String vto, Set<Arch> arch, String[] types, File outDir)
      throws IOException, InterruptedException {
    JdkVersion vf = vfrom != null ? JdkVersion.parse(vfrom) : null;
    JdkVersion vt = vto != null ? rels.select(JdkVersion.parse(vto)).getVersion() : null;


    for (String v : Arrays.asList("8", "7", "6")) {
      JdkVersion version = JdkVersion.parse(v);
      JCE jce = rels.getJCE(version);
      if (jce == null) {
        continue;
      }
      File jceFile = new File(outDir, jce.getPath());
      if (!jceFile.exists()) {
        CachingOutput output = new CachingOutput();
        JdkGetter context = new JdkGetter(transport, output);
        context.setSilent(silent);

        jceFile.getParentFile().mkdirs();
        output.info("** Downloading jce policy files to " + jceFile);
        transport.downloadJce(context, jce, jceFile);
        output.output(System.out);
      }
    }

    ExecutorService ex = Executors.newFixedThreadPool(threads);
    for (JdkRelease rel : rels.getReleases()) {
      JdkVersion v = rel.getVersion();
      if (vt != null && vt.compareTo(v) < 0) {
        continue;
      }
      if (vf != null && v.compareTo(vf) < 0) {
        break;
      }

      Set<BinaryType> binTypes = rel.getTypes(BinaryType.forNames(types));
      if (binTypes == null) {
        continue;
      }

      for (BinaryType t : binTypes) {
        Set<Arch> reqArches = EnumSet.copyOf(arch);
        if (reqArches.isEmpty()) {
          reqArches.addAll(rel.getArchs(t));
        } else {
          reqArches.retainAll(rel.getArchs(t));
        }
        if (reqArches.isEmpty()) {
          continue;
        }
        mirrorRemoteDownloading(outDir, rel, v, reqArches, t, ex);
      }
    }
    ex.shutdown();
    ex.awaitTermination(7, TimeUnit.DAYS);
  }

  private void mirrorRemoteDownloading(File outDir, JdkRelease rel, JdkVersion v, Collection<Arch> arches, BinaryType type,
      ExecutorService ex) throws IOException, InterruptedException {
    for (Arch a : arches) {
      for (JdkBinary bin : rel.getBinaries(type, a)) {

        ex.submit(() -> {
          CachingOutput output = new CachingOutput();
          JdkGetter ctx = new JdkGetter(transport, output);
          ctx.setSilent(silent || threads > 1);
          try {
            File out = new File(outDir, bin.getPath()).getAbsoluteFile();
            if (out.exists()) {
              output.info("** Checking " + type + "-" + v.shortBuild() + " (" + a.name() + ") in " + out);
              if (transport.validate(ctx, bin, out)) {
                return;
              } else {
                ctx.getLog().info("Existing file failed validation, deleting");
                FileUtils.forceDelete(out);
              }
            }
            output.info("** Downloading " + type + "-" + v.shortBuild() + " (" + a.name() + ") to " + out);
            FileUtils.forceMkdir(out.getParentFile());

            transport.downloadJdk(ctx, bin, out);
            if (!transport.validate(ctx, bin, out)) {
              ctx.getLog().error("Invalid image file " + out);
            }
          } catch (Exception e) {
            output.error("Error downloading", e);
          } finally {
            output.output(System.out);
          }
        });

      }
    }
  }
}
