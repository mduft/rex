#!/usr/bin/env bash

HOME=$(cd $(dirname $0); pwd)
EXEC_DIR=$(pwd)

. ${HOME}/rex-config.sh

${SSH} ${REX_USER}@${REX_SERVER} -p ${REX_PORT} path --roots="${REX_ROOTS}" "$@"

