How to use REX from the Client
==============================

REX is in theory completely platform independent. The only thing required to be able to
remotely execute "something" is a SSH client. However there are some convenience bash
scripts that help in easily executing binaries. Also, for Linux, this script can be
registered as a binfmt-misc handler with the kernel, to automatically execute win32
stuff through the script.

On any machine, adapt config.sh to contain the correct settings. You should then be
able to call the binfmt-misc.sh script with any executable as argument already, provided
you have started and configured the REX agent on the server.

The "client" directory should be added to your PATH variable so that helper scripts
that are used to maintain a certain level of SUA compatibility are reachable.

To register the binfmt-misc handler with linux:

`echo ":REXexe:M::MZ::/path/to/binfmt-misc.sh:" > /proc/sys/fs/binfmt_misc/register`
`echo ":REXcmd:E::cmd::/path/to/binfmt-misc.sh:" > /proc/sys/fs/binfmt_misc/register`
`echo ":REXbat:E::bat::/path/to/binfmt-misc.sh:" > /proc/sys/fs/binfmt_misc/register`

Be aware that the extension for .cmd and .bat are case sensitive! So you might need more.

Sharing a filesystem
====================

There are several tricky things in getting to a propper shared filesystem. The way
I did this in my experiments is, to share C:\ on a windows host and mount that using
CIFS on Linux. I also experimented with NFS, but CIFS had less problems, although
it was harder to configure properly.

On the client, I was using mount options `rw,noserverino,cache=none`. This however
does NOT disable all caches. The lookup cache still remains, that will cache directory
contents, and refresh at most once a second or so. To disable the lookup cache you
have to (as root):

 echo 0 > /proc/fs/cifs/LookupCacheEnabled

This will result in minimum roundtrips slightly below 100ms from call to return with
a file existing on the client that was created on the server.

Sharing multiple filesystems
============================

A typical scenario for REX is to be able to execute a binary on the server that writes
a file that the client will read afterwards. This has a few problems (caching, symlinks, ...)
as described above. To be able to take full advantage of the capabilities of each system
we typically want to:
 * be able to find binaries on the server (thus we need to somehow mount it's root fs)
 * be able to take advantage of the power of a local filesystem for "work" folders.

This leads to a setup where server and client mount each others shares in a "criss cross"
setup. I.e. the client mounts the servers share to get access to the binaries (and adds
paths in this share to it's PATH), and the server mounts the clients share to be able
to write output files directly to that filesystem.

In case the host cannot use symlinks (Windows), the client still can on his share.

REX supports this scenario by allowing to map multiple roots. The configuration happens
on the client side, so that multiple clients could go with different setups. The server
is completely stateless, except for restricting the allowed root mappings.

This can be configured in config.sh like

 `REX_ROOTS='C:\;/some/path,F:\;/another/path'`

All paths in '/some/path/...' will be mapped to 'C:\...', etc.

(Deprecated) Symbolic links when sharing a single filesystem
============================================================

(NOTE: a better way of overcoming this issues is described in the previous (new) chapter)

SMB/CIFS can not currently do symlinks on mounts served from windows (please prove
me wrong!). To overcome this (as symlinks are often required in builds, etc.), the
only current solution is a "criss-cross" mount. Let me illustrate this.

The Windows machine shares drive C: as 'C'.  
The Linux machine mounts this share to '/mnt/C' to have access to all the binaries.  
The Windows machine creates an empty directory 'C:\work'
The Linux machine bind-mounts a local directory ('/work') to '/mnt/C/work'.  
The Windows machine deteles the empty directory 'C:\work'
The Linux machine shares '/work', so that the windows host has access to it.  
The Windows machine symlinks the 'work' share to C:\work using
`mklink /d C:\work \\server\work`

Now both machines see the same filesystem consistently, where all of it resides on
the windows machine, /except/ for the 'work' folder, which resides on linux and is
shared to the windows machine.

This mechanism opens up another bunch of caches, this time on the windows side. To
be able to interact on a file basis in realtime, you will have to tune those caches
dramatically. Useful information can be found here:
 http://technet.microsoft.com/en-us/library/ff686200(v=ws.10).aspx

I suggest deactivating the directory cache by setting 
`HKEY_LOCAL_MACHINE\System\CurrentControlSet\Services\Lanmanworkstation\Parameters\DirectoryCacheLifetime` to 0


