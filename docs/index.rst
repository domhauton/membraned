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
- Install using ``dpkg -i /path/to/membrane-1.0.0-alpha.7.deb`` or ``rpm -i /path/to/membrane-1.0.0-alpha.7.rpm``
- Start the daemon using ``sudo systemctl start membraned.service``
- Check status using ``membrane status``
- Run GUI using ``membrane-gui``
- Logs can be viewed using ``journalctl -u membraned.service``
- Backup your credentials from ``/root/.config/membrane/auth``
- To restore another account replace ``/root/.config/membrane/auth`` with your backed up credentials

.. _DEB: https://github.com/domhauton/membraned/releases/download/1.0.0-alpha.7/membrane-1.0.0-alpha.7.deb
.. _RPM: https://github.com/domhauton/membraned/releases/download/1.0.0-alpha.7/membrane-1.0.0.alpha.7.rpm

Requirements
------------

- Java 8 or above
- Systemd (if using packages)

Quick Start
-----------

- Use the GUI to monitor the membrane daemon ``membrane-gui``
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

- **URL** : /
- **Method** : GET
- **Response Params** : {hostname: [string], startTime: [dateTime], port: [number], version: [string], status: [string], tagline: [string]}
- **Response Codes** : Success (200 OK), Unauthorized (403), Internal Error (500)

Network
~~~~~~~

- **URL** : /status/network
- **Method** : GET
- **Response Params** : {enabled : [bool], connectedPeers: [number], networkUID: [string], maxConnectionCount: [number], peerListeningPort: [number], upnpAddress: [string]}
- **Response Codes** : Success (200 OK), Unauthorized (403), Internal Error (500)

Storage
~~~~~~~

- **URL** : /status/storage
- **Method** : GET
- **Response Params** : {currentFiles: [string[]], referencedFiles: [string[]], localShardStorageSize: [number], targetLocalShardStorageSize: [number], maxLocalShardStorageSize: [number], peerBlockStorageSize: [number], targetPeerBlockStorageSize: [number], maxPeerBlockStorageSize: [number]}
- **Response Codes** : Success (200 OK), Unauthorized (403), Internal Error (500)

Watcher
~~~~~~~

- **URL** : /status/watcher
- **Method** : GET
- **Response Params** : {trackedFolders: [string[]], trackedFiles: [string[]]}
- **Response Codes** : Success (200 OK), Unauthorized (403), Internal Error (500)

Watch Folders
~~~~~~~~~~~~~

- **URL** : /status/watch_folder
- **Method** : GET
- **Response Params** : {watchFolders: [watchFolder[]]}
- **Response Codes** : Success (200 OK), Unauthorized (403), Internal Error (500)
- **Other** : watchFolder = {directory: [string], recursive: [bool]}

Contract
~~~~~~~~

- **URL** : /status/contract
- **Method** : GET
- **Response Params** : {contractManagerActive: [boolean], contractTarget: [number], contractedPeers: [string[]], undeployedShards: [string[]], partiallyDistributedShards: [string[]], fullyDistributedShards: [string[]]}
- **Response Codes** : Success (200 OK), Unauthorized (403), Internal Error (500)

Modify Watch Folder
~~~~~~~~~~~~~~~~~~~

- **URL** : /configure/watch_folder
- **Method** : POST
- **Request Params** : {type: [string (ADD|REMOVE)], watchFolder: [watchFolder]}
- **Response Codes** : Success (200 OK), Partial Fail (304), Invalid Request (400), Unauthorized (403), Internal Error (500)
- **Other** : watchFolder = {directory: [string], recursive: [bool]}


Request Storage Cleanup
~~~~~~~~~~~~~~~~~~~~~~~

- **URL** : /request/cleanup
- **Method** : POST
- **Response Codes** : Success (200 OK), Invalid Request (400), Unauthorized (403), Internal Error (500)


Reconstruct File
~~~~~~~~~~~~~~~~

- **URL** : /request/reconstruct
- **Method** : POST
- **Request Params** : {filepath: [string]}
- **Response Codes** : Success (200 OK), Partial Fail (304), Invalid Request (400), Unauthorized (403), Internal Error (500)


Request File History
~~~~~~~~~~~~~~~~~~~~

- **URL** : /request/history
- **Method** : POST
- **Request Params** : {filepath: [string], targetFilePath: [string], dateTimeMillis: [number]}
- **Response Params** : {filePath: [string], fileHistoryEntryList: [fileHistoryEntry[]]}
- **Response Codes** : Success (200 OK), Partial Fail (304), Invalid Request (400), Unauthorized (403), Internal Error (500)
- **Other** : fileHistoryEntry = {dateTime: [string], hashes: [string[]], size: [number], remove: [boolean]}

Support
-------

If you are having issues, please let us know. We have a mailing list located at: support@mbrn.io

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`