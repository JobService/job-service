{
	"id": "JobService",
	"groups": [{
		"id": "jobservice-testing",
		"apps": [{
				"id": "worker-globfilter",
				"cpus": 0.5,
				"mem": 1024,
				"instances": 1,
				"container": {
					"docker": {
						"image": "jobservice/worker-globfilter:${worker-globfilter.version}",
						"network": "BRIDGE",
						"forcePullImage": true
					},
					"type": "DOCKER",
					"volumes": [
						{
							"containerPath": "/mnt/caf-datastore-root",
							"hostPath": "${WORKER_STORAGE_HOST_DATA_DIRECTORY}",
							"mode": "RW"
						},
						{
							"containerPath": "/mnt/caf-worker-input-dir",
							"hostPath": "${JOB_SERVICE_DEMO_INPUT_DIR}",
							"mode": "RO"
						}
					]
				},
				"env": {
					"CAF_RABBITMQ_HOST": "${CAF_RABBITMQ_HOST}",
					"CAF_RABBITMQ_PORT": "${CAF_RABBITMQ_PORT}",
					"CAF_RABBITMQ_PASSWORD": "${CAF_RABBITMQ_PASSWORD}",
					"CAF_RABBITMQ_USERNAME": "${CAF_RABBITMQ_USERNAME}",
					"CAF_WORKER_INPUT_QUEUE": "${CAF_WORKER_GLOBFILTER_INPUT_QUEUE}",
					"CAF_BATCH_WORKER_ERROR_QUEUE": "${CAF_BATCH_WORKER_ERROR_QUEUE}",
					"CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER": "${CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER}"
				}
			},
			{
				"id": "worker-langdetect",
				"cpus": 0.5,
				"mem": 1024,
				"instances": 1,
				"container": {
					"docker": {
						"image": "cafdataprocessing/worker-languagedetection:${worker-langdetect.version}",
						"network": "BRIDGE",
						"forcePullImage": true
					},
					"type": "DOCKER",
					"volumes": [
						{
							"containerPath": "/mnt/caf-datastore-root",
							"hostPath": "${WORKER_STORAGE_HOST_DATA_DIRECTORY}",
							"mode": "RW"
						},
						{
							"containerPath": "/mnt/caf-worker-input-dir",
							"hostPath": "${JOB_SERVICE_DEMO_OUTPUT_DIR}",
							"mode": "RW"
						}
					]
				},
				"env": {
					"CAF_RABBITMQ_HOST": "${CAF_RABBITMQ_HOST}",
					"CAF_RABBITMQ_PORT": "${CAF_RABBITMQ_PORT}",
					"CAF_RABBITMQ_PASSWORD": "${CAF_RABBITMQ_PASSWORD}",
					"CAF_RABBITMQ_USERNAME": "${CAF_RABBITMQ_USERNAME}",
					"CAF_WORKER_INPUT_QUEUE": "${CAF_WORKER_LANGDETECT_INPUT_QUEUE}",
					"CAF_WORKER_OUTPUT_QUEUE": "${CAF_WORKER_LANGDETECT_OUTPUT_QUEUE}",
					"CAF_LANG_DETECT_WORKER_OUTPUT_FOLDER": "${CAF_LANG_DETECT_WORKER_OUTPUT_FOLDER}"
				}
			}]
	}]
}