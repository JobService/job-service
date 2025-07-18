#### Version Number
${version-number}

#### New Features
- None

#### Bug Fixes
- **D1038066:**  Memory leak  
  A memory leak resulting from the improper use of Spring ApplicationContext has been resolved.

#### Breaking Changes
- **D1038066:**  Defaults removed  
  Defaults for a number of configuration variables have been removed.  These defaults were never intended &mdash; they were mistakeably
  added by the [#115](https://github.com/JobService/job-service/pull/115) changes.

#### Known Issues
- None
