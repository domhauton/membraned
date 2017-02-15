# Membrane Daemon
[![Build Status](https://travis-ci.org/domhauton/membraned.svg?branch=master)](https://travis-ci.org/domhauton/membraned)

Membrane is a Distributed Backup System that can utilise peers to store your data.

The daemon monitors configured watch folders, storing file history locally and on the peer network to allow future recovery.

Membrane will store files both locally and on the peer network until it reaches the space limit, and then prunes the start of the file history.

You can interact with the daemon using the gui found [here](https://github.com/domhauton/membrane).
