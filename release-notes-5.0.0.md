!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- SCMOD-8730: Added support for Expiration policy.  
  We now have an expiration policy for each job, related to its status.
  The policy action is applied based on the operation (`Expire`, `Delete`) and the expiration_time provided. The expiration_time can be a specific date, or an offset from `create_date` or `last_update_date`.

#### Breaking Changes
- **SCMOD-13865:** propagate_failures toggle option has been removed
We propagate all failures, no more possibility to disable it (previously set to false by default)

#### Known Issues
- None
