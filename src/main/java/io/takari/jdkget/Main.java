package io.takari.jdkget;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import com.fasterxml.jackson.databind.ext.Java7SupportImpl;
import com.google.common.base.Preconditions;

import io.takari.jdkget.model.JCE;
import io.takari.jdkget.model.BinaryType;
import io.takari.jdkget.model.JdkRelease;
import io.takari.jdkget.model.JdkReleases;
import io.takari.jdkget.model.JdkVersion;

public class Main {

  private static final Options cliOptions = new Options();
  static {
    cliOptions.addOption("o", true, "Output dir");
    cliOptions.addOption("v", true, "JDK Version");
    cliOptions.addOption("t", true, "Java release type of: jdk(default), jre, serverjre");
    cliOptions.addOption("a", true, "Architecture");
    cliOptions.addOption("l", false, "List versions");
    cliOptions.addOption("u", true, "Alternate url to oracle.com/otn-pub");
    cliOptions.addOption("jce", false, "Also install unlimited jce policy");
    cliOptions.addOption("otnUser", true, "OTN username");
    cliOptions.addOption("otnPassword", true, "OTN password");
    cliOptions.addOption("releases", true, "Alternate url to jdkreleases yaml configuration");
    cliOptions.addOption("mirror", false,
        "Mirror remote storage by only downloading binaries; can be used with -v, -vf, -vt, -t and -a, otherwise will download everything");
    cliOptions.addOption("threads", true, "Number of threads to run mirror with");
    cliOptions.addOption("vf", true, "When used with -mirror, specifies version range 'from'");
    cliOptions.addOption("vt", true, "When used with -mirror, specifies version range 'to'");
    cliOptions.addOption("s", false, "Silence download messages");
    cliOptions.addOption("?", "help", false, "Help");
  }

  public static void main(String[] args) throws Exception {
    Preconditions.checkNotNull(Java7SupportImpl.class); // prime it to prevent errors later

    CommandLine cli = new PosixParser().parse(cliOptions, args);

    if (cli.hasOption("l")) {
      System.out.println("Available JDK versions:");
      for (JdkRelease r : JdkReleases.get().getReleases()) {
        JdkVersion v = r.getVersion();
        System.out.println("  " + v.longBuild() + " / " + v.shortBuild() + (r.isPsu() ? " PSU" : ""));
      }
      return;
    }

    String o = cli.getOptionValue("o");

    String u = cli.getOptionValue("u");
    String otnu = cli.getOptionValue("otnUser");
    String otnp = cli.getOptionValue("otnPassword");
    String relDoc = cli.getOptionValue("releases");

    boolean mirror = cli.hasOption("mirror");
    String v = cli.getOptionValue("v");// "1.8.0_92-b14";
    String[] a = cli.getOptionValues("a");
    String[] t = cli.getOptionValues("t");
    boolean silent = cli.hasOption("s");

    boolean jceOpt = cli.hasOption("jce");

    if (cli.hasOption('?')) {
      usage();
      return;
    }

    if (o == null) {
      System.err.println("No output dir specified");
      usage();
      return;
    }

    JdkReleases rels;
    if (relDoc != null) {
      rels = JdkReleases.readFromUrl(relDoc);
    } else {
      rels = JdkReleases.get();
    }

    File outDir = new File(o);
    Set<Arch> arches = parseArches(a);

    Map<String, String> trParams = new HashMap<>();
    System.getProperties().forEach((k, val) -> trParams.put(k.toString(), val.toString()));
    if (u != null) {
      trParams.put(ITransportFactory.PARAM_BASEURL, u);
    }
    if (otnu != null) {
      trParams.put(ITransportFactory.PARAM_USERNAME, otnu);
    }
    if (otnp != null) {
      trParams.put(ITransportFactory.PARAM_PASSWORD, otnp);
    }
    ITransport transport = rels.createTransportFactory().createTransport(trParams);

    if (mirror) {
      String vf = cli.getOptionValue("vf");
      String vt = cli.getOptionValue("vt");

      int threads = 1;
      if (cli.hasOption("threads")) {
        threads = Integer.parseInt(cli.getOptionValue("threads"));
      }

      new MirrorRemote(transport, threads, silent)
          .mirrorRemote(rels, v != null ? v : vf, v != null ? v : vt, arches, t, outDir);
      return;
    }

    if (v == null) {
      System.err.println("No version specified");
      usage();
      return;
    }

    Arch arch = null;
    if (!arches.isEmpty()) {
      if (arches.size() == 1) {
        arch = arches.iterator().next();
      } else {
        System.err.println("Only one arch is allowed");
      }
    }
    if (arch == null) {
      usage();
      return;
    }

    if (t != null && t.length != 1) {
      System.err.println("Only one type is allowed: " + Arrays.toString(t));
      usage();
      return;
    }

    BinaryType type = null;
    if (t != null) {
      type = BinaryType.forName(t[0], null);
    }
    if (t != null && type == null) {
      System.err.println("Release type is not supported: " + t[0]);
      System.err.print("Avalable release types: " +
          Arrays.asList(BinaryType.values()).stream().map(at -> at.getName())
              .collect(Collectors.joining(", ")));
      usage();
      return;
    }

    JdkRelease rel = rels.select(v);

    JdkGetter jdkGet = new JdkGetter(transport, StdOutput.INSTANCE);
    jdkGet.setSilent(silent);

    JCE jce = null;
    if (jceOpt) {
      jce = rels.getJCE(rel.getVersion());
    }

    jdkGet.get(rel, jce, arch, type, outDir);
  }

  private static void usage() {
    String ver = JdkGetter.class.getPackage().getImplementationVersion();
    new HelpFormatter().printHelp("java -jar jdkget-" + ver + ".jar", cliOptions);
    System.out
        .println("\nVersion format: 1.<major>.0_<rev>-<build> or <major>u<rev>-<build> or <ver>+<build> (for 9+)");
    System.out.println("\nAvailable architectures:");
    for (Arch ar : Arch.values()) {
      System.out.println("    " + simpleArch(ar.name()));
    }

    System.out.println("\nExamples:");
    System.out.println("  List versions:");
    System.out.println("    jdkget-" + ver + ".jar -l");
    System.out.println("  Download and extract:");
    System.out.println("    jdkget-" + ver + ".jar -o <outputDir> -v <jdkVersion> [-t <type>] [-a <arch>]");
    System.out.println("  Mirror remote:");
    System.out.println("    jdkget-" + ver
        + ".jar -mirror -o <outputDir> [-t <type1> ... -t <typeN>] [-v <jdkVersion>] [-vf <fromVersion>] [-vt <toVersion>] [-a <arch>]");
  }

  private static Set<Arch> parseArches(String[] as) {
    Set<Arch> arches = EnumSet.noneOf(Arch.class);
    if (as != null) {
      for (String a : as) {
        a = simpleArch(a);
        for (Arch arch : Arch.values()) {
          String av = simpleArch(arch.name());
          if (a.equals(av)) {
            arches.add(arch);
          }
        }
      }
    }
    return arches;
  }

  private static String simpleArch(String a) {
    return a.toLowerCase().replaceAll("[^a-z0-9]", "");
  }
}
