# Job Service Prism Mock Server

To run the Mock service:

    docker container run --init -dt --rm --name job-service-mock -p 4010:4010 cafinternal/prereleases:job-service-mock-3.2.0-SNAPSHOT

To view the logs:

    docker container logs -f job-service-mock

To stop the service:

    docker container stop job-service-mock
