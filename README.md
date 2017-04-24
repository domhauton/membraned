# Membrane Daemon
[![Build Status](https://travis-ci.org/domhauton/membraned.svg?branch=master)](https://travis-ci.org/domhauton/membraned) [![Dependency Status](https://www.versioneye.com/user/projects/58a5efc6a782d10041e105d7/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/58a5efc6a782d10041e105d7) [![codecov](https://codecov.io/gh/domhauton/membraned/branch/master/graph/badge.svg)](https://codecov.io/gh/domhauton/membraned) [![rtfd](https://readthedocs.org/projects/mbrn/badge/?version=latest)](http://mbrn.rtfd.io/) [![license](https://img.shields.io/github/license/mashape/apistatus.svg)]()

Membrane is a Distributed Backup System that swaps your storage space with other users in the Membrane swarm to backup your data.

Features
--------

- Both [GUI](https://github.com/domhauton/membrane-gui) and [CLI](https://github.com/domhauton/membrane-cli) interfaces
- File history ensures file versions are never removed until necessary.
- Log in from anywhere using your generated 4096-bit RSA key
- Twofish encryption for data privacy.
- Deduplication reduces storage requirements for similar files.
- Prioritise backup to friends and family.

Installation
------------

- Download the [deb](https://github.com/domhauton/membraned/releases/download/1.0.0-alpha.5/membrane-1.0.0-alpha.5.deb) or  [rpm](https://github.com/domhauton/membraned/releases/download/1.0.0-alpha.5/membrane-1.0.0.alpha.5.rpm) package
- Install using `dpkg -i /path/to/membrane-1.0.0-alpha.5.deb` or `rpm -i /path/to/membrane-1.0.0-alpha.5.rpm`
- Start the daemon using `sudo systemctl start membraned.service`
- Check status using `membrane status`
- Run GUI using `membrane-gui`
- Logs can be viewed using `journalctl -u membraned.service`

Requirements
------------

- Java 8 or above
- Systemd (if using packages)

Usage
-----

- Use the GUI to monitor the membrane daemon
- To add watch folders to backup use `membrane watch-add <folder>`
- To check backed up files use `membrane files`
- To explore file history use `membrane history <file>`
- To recover file use `membrane recover <file> <target> <optional: date>`
- Use the `-h` flag at any point for command help

Support
-------

If you are having issues, please let us know.

We have a mailing list located at: support@mbrn.io

License
-------

The project is licensed under the MIT license.
