#!/usr/bin/env bash

if [ -L /usr/bin/membrane ] ; then
    rm /usr/bin/membrane
fi

if [ -d /usr/share/membrane ] ; then
    rm -rf /usr/share/membrane
fi

if [ -f /etc/systemd/system/membraned.service ] ; then
    rm /etc/systemd/system/membraned.service
fi

systemctl daemon-reload