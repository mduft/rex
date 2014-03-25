REX
===

Remote EXecution Service - A SSH based remote execution agent that allows to execute non-native binaries on shared filesystem remotely on another architecture (fex. run windows binaries from linux, executing on native windows).

Each server and each client has a "root" offset that is to be configured as command line argument. These prefixes are used to transform paths from the client to the server.

Here is a demo of REX in action: http://www.youtube.com/watch?v=GGsWSY1oJVw

Used & bundled third party libraries
====================================

The following libraries are used (and re-packed into the final JAR). Please see their respective licenses for more information.

 * jopt-simple-4.6.jar          (--jopt-simple)
 * slf4j-api-1.7.6.jar          (SLF4J API)
 * slf4j-simple-1.7.6.jar       (SLF4J Simple Logger)
 * sshd-core-0.10.1.jar         (Apache MINA SSHD Core)

QuickStart Guide
================

You need to configure both server and client correctly for remote execution to succeed. Prerequisite is that ther is a filesystem that is reachable from both machines using a certain path. In the following
example I just shared the drive C: on a Windows VM, and mounted it using CIFS on the Linux host.

Start the Server
----------------

Make sure you have Java7 installed. Then run the server jar file like this from a command prompt

`java -jar at.mduft.rex.jar --pubkeys=C:\REX\pubkeys.txt --hostkey=C:\REX\hostkey.txt --root=C:\ --user=rex`

For more information on what the arguments mean, try ***--help***. Make sure *C:\REX* exists. Put at least one public key (RSA or DSA) into the *C:\REX\pubkeys.txt* file that is authorized to connect. The file will be monitored for changes at runtime, so adding additional keys does not require a server restart.

Make sure the argument to ***--root*** points to the servers mount point for the shared filesystem. Note that only executables that reside on this shared filesystem or are reachable through the default system path can be executed through REX.

Use the Client
--------------

Read client/README.md for information on how to configure the client. Basically after filling out some information in config.sh you should be able to cd to the client root directory and execute `/path/to/rex/client/binfmt-misc.sh /path/to/client/root/exe.exe`. You can set PATH on the client to point to directories containing windows binaries (fex. `PATH=${PATH}:/path/to/root/C/Windows:/path/to/root/C/Windows/System32`) and execute them without path (fex. `/path/to/rex/client/binfmt-misc.sh cmd.exe`).

Performance Tuning
==================

To get better connection speeds through SSH, you can setup a persistent control connection. To do so, put this on the client machine in the file *~/.ssh/config* for the user connecting:

```
Host <IP_OR_HOSTNAME_OF_REX_SERVER>
    ControlPath ~/.ssh/sockets/%l-%r@%h:%p
    ControlMaster auto
    ControlPersist yes
```

Make sure *~/.ssh/sockets* exists.
