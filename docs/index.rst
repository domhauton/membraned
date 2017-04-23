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

- The project is currently unavailable. Please bear with us.

API
---

The Rest API is available on port 13200 by default.

Available Calls include:

- GET: / RES: {hostname: string, startTime: dateTime, port: long, version: string, status: string, tagline: string}
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