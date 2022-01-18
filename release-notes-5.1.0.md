!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **[SCMOD-5524](https://portal.digitalsafe.net/browse/SCMOD-5524)** taskData no longer sent as Base64.  
    taskData is no longer Base64 encoded in order to reduce message size. 

#### Breaking Changes
- **378627**: Workers build using framework version prior to 6.0.0 are no longer supported.
  - The format of the messages sent to the workers has been changed ( Base64 format no longer used )

#### Known Issues
- None
