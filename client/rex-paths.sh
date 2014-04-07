function doConvert() {
    local sourceRoot="$1"
    local targetRoot="$2"
    local path="$3"

    [[ ${path} != ${sourceRoot}* ]] && {
        echo "ERROR: path is not inside target root ('${path}' is not a child of '${sourceRoot}')" 1>&2
        exit 1
    }

    [[ ${sourceRoot} == *"\\"* ]] && sourceRoot="${sourceRoot//\\/\\\\}"
    [[ ${targetRoot} == *"\\"* ]] && targetRoot="${targetRoot//\\/\\\\}"

    local sep="/"
    [[ ${targetRoot} == *":"* ]] && sep="\\\\"

    echo "${path//${sourceRoot}/${targetRoot}}" | sed -e "s,[\\/][\\/]*,${sep},g"
}

function splitRoot() {
    local rootPair="$1"
    local saveIDS=$IFS
    IFS=';'

    local items=( ${rootPair} )
    
    [[ ${#items[@]} != 2 ]] && {
        echo "ERROR: root definition must consist of pairs of paths. '${rootPair}' is not a valid pair." 1>&2
        exit 1
    }

    echo "server='${items[0]}'"
    echo "client='${items[1]}'"
}

function findRoot() {
    local path="$1"
    local toServer="$2"

    local saveIFS=$IFS
    IFS=','

    for pair in ${REX_ROOTS}; do
        IFS=${saveIFS}
        
        local server=
        local client=
        eval $(splitRoot "${pair}")

        if [[ ${toServer} == yes && ${path} == ${client}* ]]; then
            echo "source='${client}'"
            echo "target='${server}'"
        elif [[ ${toServer} == no && ${path} == ${server}* ]]; then
            echo "source='${server}'"
            echo "target='${client}'"
        else
            continue
        fi

        return 0
    done

    IFS=${saveIFS}
}

function convert() {
    local path="$1"
    local to_server="$2"

    local source=
    local target=
    eval $(findRoot "${path}" "${to_server}")

    doConvert "${source}" "${target}" "${path}"
}

function toClient() {
    convert "$1" "no"
}

function toServer() {
    convert "$1" "yes"
}

