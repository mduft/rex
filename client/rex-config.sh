### CONFIGURATION FOR REX CLIENT SIDE ###

export REX_SERVER=<hostname>
export REX_PORT=9000
export REX_USER=rex
export REX_ROOTS='C:\;/mnt/C,D:\;/work'

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

# rename "dangerous" variables, so they will not destroy the SSH executable
dangerous=( "LD_LIBRARY_PATH" "LD_PRELOAD" )
for var in ${dangerous[@]}; do
    val="$(eval echo \${${var}})"
    [[ -n "${val}" ]] && \
        export _REX_${var}="${val}"
    unset ${var}
done
