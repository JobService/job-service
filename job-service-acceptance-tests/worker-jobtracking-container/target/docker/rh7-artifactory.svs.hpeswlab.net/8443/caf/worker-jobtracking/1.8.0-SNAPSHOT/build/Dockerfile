FROM java:8
LABEL Build.Date=2016-12-19T12:53:06Z caf.config.impl=config-file caf.worker-queue.impl=worker-queue-rabbit Git.Branch=develop Git.Commit=ad8b96c8977f75566c0508f24653fba7697b3a3b Build.Number=1.8.0-SNAPSHOT caf.worker-framework.version=1.0.0 caf.worker-store.impl=worker-store-cs caf.codec.impl=codec-json
COPY maven /maven/
CMD /maven/worker.sh
ENTRYPOINT ["/maven/tini-0.9.0.exe","-v","--"]
