#!/usr/bin/env bash

tar -C /usr/share/membrane/lib -xzf /usr/share/membrane/lib/membrane-gui-v1.0.0-alpha.5.tar.gz

rm /usr/share/membrane/lib/membrane-gui-v1.0.0-alpha.5.tar.gz
cp /usr/share/membrane/bin/membraned.service /etc/systemd/system/membraned.service
cp /usr/share/membrane/bin/autocomplete /etc/bash_completion.d/membrane

systemctl daemon-reload
systemctl enable membraned
