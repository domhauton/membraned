#!/usr/bin/env bash

cp /usr/share/membrane/bin/membraned.service /etc/systemd/system/membraned.service
cp /usr/share/membrane/bin/autocomplete /etc/bash_completion.d/membrane

systemctl daemon-reload
systemctl enable membraned
