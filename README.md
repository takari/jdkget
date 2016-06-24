# JdkGet

JdkGet is a Java utility that allows you to download Linux/OSX/Windows JDKs from Oracle, and extract them in arbitrary directories in a fully automated way.

To install a JDK you can use something like the following:

```java
JdkGetter getter = JdkGetter.builder()
  .version("1.8.0_92-b14")
  .outputDirectory(jdkDirectory)
  .build();  
    
getter.get();  
```

You can find a list of available JDKs [here](Jdks.md).

By using this utilitiy you agree to the [Oracle Binary Code License Agreement for Java SE][1].

## Building

To build JDKGet use the Maven Wrapper script provided with the project:

```
./mvnw clean install
```

The result will produce a shaded JAR in the `target/` directory which can we executued using `java -jar jdkget-${version}.jar`

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