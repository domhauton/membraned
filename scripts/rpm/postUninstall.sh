#!/bin/bash

if [ -L /usr/bin/membrane ] ; then
    rm /usr/bin/membrane
fi

if [ -L /usr/bin/membraned ] ; then
    rm /usr/bin/membraned
fi

if [ -L /usr/bin/membrane-gui ] ; then
    rm /usr/bin/membraned-gui
fi

if [ -d /usr/share/membrane ] ; then
    rm -rf /usr/share/membrane
fi

if [ -f /etc/systemd/system/membraned.service ] ; then
    rm /etc/systemd/system/membraned.service
fi

if [ -f /etc/bash_completion.d/membrane ] ; then
    rm /etc/bash_completion.d/membrane
fi

systemctl daemon-reload