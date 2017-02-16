# Membrane Daemon
[![Build Status](https://travis-ci.org/domhauton/membraned.svg?branch=master)](https://travis-ci.org/domhauton/membraned) [![Dependency Status](https://www.versioneye.com/user/projects/58a5efc6a782d10041e105d7/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/58a5efc6a782d10041e105d7)

Membrane is a Distributed Backup System that can utilise peers to store your data.

The daemon monitors configured watch folders, storing file history locally and on the peer network to allow future recovery.

Membrane will store files both locally and on the peer network until it reaches the space limit, and then prunes the start of the file history.

You can interact with the daemon using the gui found [here](https://github.com/domhauton/membrane).
