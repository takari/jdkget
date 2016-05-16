# OsxJdkGet

OsxJdkGet is a Java utility that allows you to download OSX JDKs from Oracle, and install them in arbitrary directories in a fully automated way.

To install an OSX JDK you can use something like the following:

```java
OsxJdkGetter getter = OsxJdkGetter.builder()
  .version("1.8.0_92-b14")
  .outputDirectory(jdkDirectory)
  .build();  
    
getter.get();  
```

This will retrieve the Java equivalent of [turducken][2], that are the GZipped CPIO files of the JDK, wrapped in an XAR file, inside an HFS disk image. Can't we just use TarGz files? No, no, that would be too easy.

You can find a list of available JDKs [here](Jdks.md).

By using this utilitiy you agree to the [Oracle Binary Code License Agreement for Java SE][1].

## TODO

- replace internal XML code with dd-plist which is small and uses the JDK's XML classes
- replace plexus-utils with JDK directory/file code
- replace simpleframework XML in xar with JDK XML code: https://github.com/sprylab/xar/issues/1
- figure out how to get the <=1.6 JDKs from Apple automatically

## NOTES

- https://ivan-site.com/2012/05/download-oracle-java-jre-jdk-using-a-script/
- https://people.freebsd.org/~kientzle/libarchive/man/cpio.5.txt

[1]: http://www.oracle.com/technetwork/java/javase/terms/license/index.html
[2]: https://en.wikipedia.org/wiki/Turducken