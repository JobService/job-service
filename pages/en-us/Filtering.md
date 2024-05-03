---
layout: default
title: Filtering

banner:
    icon: 'assets/img/fork-lift.png'
    title: Job Service
    subtitle: Orchestration, Management and Monitoring of Data Processing
    links:
        - title: GitHub
          url: https://github.com/JobService/job-service
---

# Filtering

Filtering can be used to limit the results returned when searching for jobs.  
Jobs can filtered using a filter parameter written in RSQL syntax that can be passed to the get jobs RestApi call. 
This filter will allow the request to filter out jobs that do not meet the specified criteria. 
The RSQL filter syntax specifications can found [here](https://github.com/jirutka/rsql-parser).

Below is a list of the RSQL operators and what they can be used for.

| Operators | Meaning                                                    |
|-----------|------------------------------------------------------------|
|    ==     | Equal to                                                   |
|    !=     | Not equal to                                               |
|    >      | Greater than                                               |
|    >=     | Greater than or equal to                                   |
|    >      | Less than                                                  |
|    >=     | Less than or equal to                                      |
|   =in=    | Value present in array ["job1", "job2"]                    |
|   =out=   | Value not present in array ["job1", "job2"]                |
|   and     | Compounding operator that combines two statements together |
|   or      | Compounding operator that combines two statements together |


| Supported Filter Keys |  
|-----------------------|  
| labels.<labelKey>     |  
| name                  |  
| id                    |  
| status                |  
| percentageComplete    |   

Example RSQL filters:  
- `labels.<labelKey>==<labelValue>`  
Specified label has specified value.  
- `labels.<labelKey>!=<labelValue>`  
Specified label doesn't have specified value.  
- `status=in=('Active', 'Waiting')`  
Job status is either Active or Waiting.  
- `status=out=('Completed', 'Failed')`  
Job Status is not Completed or Failed.  
- `percentageComplete>50`  
Percentage complete is more than 50% complete.  
- `percentageComplete<50`  
Percentage complete is less than 50% complete.  
- `percentageComplete>=50`  
Percentage complete is more than or equal to 50% complete.  
- `percentageComplete<=50`  
Percentage complete is less than or equal to 50% complete.  
- `percentageComplete>50 and status!='Failed`  
Percentage complete is greater than 50% and status is not failed.  
