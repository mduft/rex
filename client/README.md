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
