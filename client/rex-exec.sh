#!/usr/bin/env bash

HOME=$(cd $(dirname $0); pwd)
EXEC_DIR=$(pwd)

. ${HOME}/rex-config.sh

if [[ ${EXEC_DIR} != ${REX_ROOT}* ]]; then
    echo "error: current directory not inside REX client root"
    exit 1
fi

${SSH} ${REX_USER}@${REX_SERVER} -p ${REX_PORT} exec --roots="${REX_ROOTS}" --pwd="${EXEC_DIR}" -- "${args[@]}"

