# JdkGet

JdkGet is a Java utility that allows you to download Linux/OSX/Windows JDKs from Oracle, and extract them in arbitrary directories in a fully automated way.

To install a JDK you can use something like the following:

```java
JdkReleases rels = JdkReleases.get();
ITransport transport = createTransportFactory().createTransport();

JdkRelease rel = rels.select("8u201");
JCE jce = rels.getJCE(rel.getVersion());

JdkGetter getter = new JdkGetter(transport, StdOutput.INSTANCE);

File outputDir = ...;
getter.getJdk(rel, jce, Arch.autodetect(), outputDir);
```

You can find a list of available JDKs [here](src/main/resources/java_releases_v1.yml).

By using this utilitiy you agree to the [Oracle Binary Code License Agreement for Java SE][1].

## Building

To build JDKGet use the Maven Wrapper script provided with the project:

```
./mvnw clean install
```

## Integration testing

Testing downloads of all versions can be performed using `it` profile and providing your OTN credentials:

```
mvn clean verify -Pit -Dio.takari.jdkget.username=<otnUsername> -Dio.takari.jdkget.password=<otnPassword>
```

The result will produce a shaded JAR in the `target/` directory which can we executed using `java -jar jdkget-${version}.jar`

## *nix and Solaris
Downloads and extracts a .tar.gz, simple and easy

## OSX
This will retrieve the Java equivalent of a [turducken][2]: the GZipped CPIO files of the JDK, wrapped in an XAR file, inside an HFS disk image. Can't we just use TarGz files? No, no, that would be too easy.

## Windows
Similar to OSX, but with tools.zip (with some jars pack200'd) in CAB in EXE in EXE.

## NOTES

- https://ivan-site.com/2012/05/download-oracle-java-jre-jdk-using-a-script/
- https://people.freebsd.org/~kientzle/libarchive/man/cpio.5.txt

[1]: http://www.oracle.com/technetwork/java/javase/terms/license/index.html
[2]: https://en.wikipedia.org/wiki/Turducken