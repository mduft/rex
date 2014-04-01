### CONFIGURATION FOR REX CLIENT SIDE ###

REX_SERVER=172.28.8.174
REX_PORT=9000
REX_USER=rex
REX_ROOT=/big/rex

# ATTENTION: all scripts use this variable to assure that the SSH options
#   are the same for all connections. This is essential when sharing connections
#   between calls (see ../README.md). The SendEnv='*' option that is on by
#   default will transfer all environment variables to the server to allow
#   for PATH expansions, etc. Otherwise you will only have the default env
#   of the server.
SSH="ssh -o SendEnv=*"

# Wrangle arguments so they are available in a quoted form for the scripts
# to use when calling through SSH.
args=()
for arg in "$@"; do
    args[${#args[@]}]="'${arg}'"
done
