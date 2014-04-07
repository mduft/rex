#!/usr/bin/env bash

# registers the binfmt-misc handler. must be run as root

binfmt=/proc/sys/fs/binfmt_misc

[[ $(uname) != Linux ]] && { echo "$0 is intended for linux only!"; exit 1; }
[[ $(id -u) != 0 ]] && { echo "$0 must be run as root!"; exit 1; }
[[ ! -d ${binfmt} || $(cat ${binfmt}/status) != enabled ]] && { echo "CONFIG_BINFMT_MISC must be enabled in the kernel!"; exit 1; }

script="$(cd "$(dirname "$0")"; pwd)/rex-exec.sh"

for type in REXexe REXcmd REXbat; do
    [[ -e ${binfmt}/${type} ]] && echo -1 > ${binfmt}/${type}
done

echo ":REXexe:M::MZ::${script}:" > ${binfmt}/register
echo ":REXcmd:E::cmd::${script}:" > ${binfmt}/register
echo ":REXbat:E::bat::${script}:" > ${binfmt}/register

