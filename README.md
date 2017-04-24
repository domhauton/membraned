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

- Download the [.deb package](https://github.com/domhauton/membraned/releases/download/1.0.0-alpha.5/membrane_1.0.0-alpha.5.deb)
- Install using `dpkg -i /path/to/download/membraned_1.0.0-alpha.5.deb`
- Start the daemon using `sudo systemctl start membraned.service`
- Check status using `membrane status`
- Logs can be viewed using `journalctl -u membraned.service`

Support
-------

If you are having issues, please let us know.

We have a mailing list located at: support@mbrn.io

License
-------

The project is licensed under the MIT license.
