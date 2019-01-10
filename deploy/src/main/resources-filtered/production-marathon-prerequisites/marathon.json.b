{
    "id": "/jobservice/jobservice-prerequisites",
    "apps": [{
            "id": "/jobservice/jobservice-prerequisites/postgres",
            "cpus": 0.5,
            "mem": 1024,
            "instances": 1,
            "container": {
                "docker": {
                    "image": "postgres:9.6",
                    "network": "BRIDGE",
                    "portMappings": [{
                        "containerPort": 5432,
                        "hostPort": 0,
                        "protocol": "tcp",
                        "servicePort": ${JOB_SERVICE_DB_PORT}
                    }],
                    "forcePullImage": true
                },
                "type": "DOCKER"
            },
            "env": {
                "POSTGRES_USER": "${JOB_SERVICE_DB_USER}",
                "POSTGRES_PASSWORD": "${JOB_SERVICE_DB_PASSWORD}"
            }
        },
        {
            "id": "/jobservice/jobservice-prerequisites/rabbitmq",
            "cpus": 0.4,
            "mem": 1024,
            "instances": 1,
            "container": {
                "docker": {
                    "image": "rabbitmq:${rabbitmq.version}",
                    "network": "BRIDGE",
                    "portMappings": [{
                        "containerPort": 5672,
                        "hostPort": 0,
                        "protocol": "tcp",
                        "servicePort": ${CAF_RABBITMQ_PORT}
                    },
                    {
                        "containerPort": 15672,
                        "hostPort": 0,
                        "protocol": "tcp",
                        "servicePort": ${CAF_RABBITMQ_MANAGEMENT_PORT}
                    }],
                    "forcePullImage": true
                },
                "type": "DOCKER",
                "volumes": [
                    {
                        "containerPath": "/var/lib/rabbitmq",
                        "hostPath": "rabbitmq",
                        "mode": "RW"
                    }
                ]
                }
        }]
}
