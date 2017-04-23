#!/usr/bin/env bash

cp /usr/share/membrane/bin/membraned.service /etc/systemd/system/membraned.service
systemctl daemon-reload
systemctl enable membraned
