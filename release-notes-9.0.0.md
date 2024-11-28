#### Version Number
${version-number}

#### Breaking changes
- US975260: Refactor to remove HPE references in package names

#### New Features
- US969005: Add support for getting secrets from configurable sources.
  - Secrets can be retrieved from the following sources:
    - Environment variables (direct value) - enabled via `CAF_ENABLE_ENV_SECRETS` (defaults to `true`)
    - File content (path specified by environment variable with `_FILE` suffix) - enabled via `CAF_ENABLE_FILE_SECRETS` (defaults to `false`)

#### Known Issues
- None
