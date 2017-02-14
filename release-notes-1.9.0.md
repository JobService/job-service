#### Version Number
${version-number}

#### New Features
- CAF-1848 - Failed messages should now remain active and be given the chance to recover.
- CAF-2033 - Job Tracking worker now supports workers with no output, such as the batch worker.
- [CAF-2100](https://jira.autonomy.com/browse/CAF-2100) - `create_date` column in Job and Task tables now stores a UTC timestamp.

#### Known Issues
- Batch worker processing a batch with zero sub tasks will not have its job table deleted.