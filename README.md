REX
===

Remote EXecution Service - A SSH based remote execution agent that allows to execute non-native binaries on shared filesystem remotely on another architecture (fex. run windows binaries from linux, executing on native windows).

Each server and each client has a "root" offset that is to be configured as command line argument. These prefixes are used to transform paths from the client to the server.

Used & bundled third party libraries
====================================

The following libraries are used (and re-packed into the final JAR). Please see their respective licenses for more information.

 * jopt-simple-4.6.jar          (--jopt-simple)
 * slf4j-api-1.7.6.jar          (SLF4J API)
 * slf4j-simple-1.7.6.jar       (SLF4J Simple Logger)
 * sshd-core-0.10.1.jar         (Apache MINA SSHD Core)

