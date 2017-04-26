.. mbrn-d documentation master file, created by
   sphinx-quickstart on Sun Mar  5 22:32:56 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Membrane Documentation
======================

.. toctree::
   :maxdepth: 2
   :caption: Contents:

Features
--------

- Both GUI_ and CLI_ interfaces
- File history ensures file versions are never removed until necessary.
- Log in from anywhere using your generated 4096-bit RSA key
- Deduplication reduces storage requirements for similar files.
- Prioritise backup to friends and family.

.. _GUI: https://github.com/domhauton/membrane-gui
.. _CLI: https://github.com/domhauton/membrane-cli

Installation
------------

- Download the DEB_ or  RPM_ package
- Install using ``dpkg -i /path/to/membrane-1.0.0-alpha.5.deb`` or ``rpm -i /path/to/membrane-1.0.0-alpha.5.rpm``
- Start the daemon using ``sudo systemctl start membraned.service``
- Check status using ``membrane status``
- Run GUI using ``membrane-gui``
- Logs can be viewed using ``journalctl -u membraned.service``

.. _DEB: https://github.com/domhauton/membraned/releases/download/1.0.0-alpha.5/membrane-1.0.0-alpha.5.deb
.. _RPM: https://github.com/domhauton/membraned/releases/download/1.0.0-alpha.5/membrane-1.0.0.alpha.5.rpm

Requirements
------------

- Java 8 or above
- Systemd (if using packages)

Quick Start
-----------

- Use the GUI to monitor the membrane daemon
- To add watch folders to backup use ``membrane watch-add <folder>``
- To check backed up files use ``membrane files``
- To explore file history use ``membrane history <file>``
- To recover file use ``membrane recover <file> <target> <optional: date>``
- Use the ``-h`` flag at any point for command help

API
---

The Rest API is available on port ``13200`` by default.

Available Calls include:

Status
~~~~~~

- **Title** : Get Status
- **URL** : /
- **Method** : GET
- **Data Params** : { u : { email : [string], name : [string], current_password : [alphanumeric] password : [alphanumeric], password_confirmation : [alphanumeric] } }
- **Response Codes** : Success (200 OK), Unauthorized (403), Internal Error (500)

Other
~~~~~

- GET: /status/watcher
- GET: /status/storage
- GET: /status/watch_folder RES: {currentFiles: string[], referencedFiles: string[], localShardStorageSize: long, targetLocalShardStorageSize: long, maxLocalShardStorageSize: long, peerBlockStorageSize: long, targetPeerBlockStorageSize: long, maxPeerBlockStorageSize: long
- POST: /configure/watch_folder  REQ: {type: "string (ADD|REMOVE)", watchFolder: {directory: "string", recursive: bool}}
- POST: /request/cleanup
- POST: /request/reconstruct REQ: {filepath: "string"}
- GET: /request/history REQ: {filepath: "string", targetFilePath: "string", dateTimeMillis: "long"}

Support
-------

If you are having issues, please let us know. We have a mailing list located at: support@mbrn.io

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`