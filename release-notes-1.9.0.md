#### Version Number
${version-number}

#### New Features
- CAF-1848 - Failed messages should now remain active and be given the chance to recover.
- CAF-2033 - Job Tracking worker now supports workers with no output, such as the batch worker.

#### Known Issues
- Batch worker processing a batch with zero sub tasks will not have its job table deleted.