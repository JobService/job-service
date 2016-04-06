package com.hpe.caf.services.job.queue;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.configuration.AppConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * This class is responsible sending task data to the target queue.
 */
public final class QueueServices {

    private final Connection connection;
    private final Channel publisherChannel;
    private final String targetQueue;
    private final Codec codec;

    public QueueServices(Connection connection, Channel publisherChannel, String targetQueue, Codec codec) {

        this.connection = connection;
        this.publisherChannel = publisherChannel;
        this.targetQueue = targetQueue;
        this.codec = codec;
    }

    /**
     * Send task data message to the target queue.
     */
    public void sendMessage(String jobId, WorkerAction workerAction, AppConfig config) throws IOException
    {
        //  Generate a random task id.
        String taskId = UUID.randomUUID().toString();

        //  Serialise the data payload. Encoding type is provided in the WorkerAction.
        byte[] taskData = null;
        if (workerAction.getTaskDataEncoding() == WorkerAction.TaskDataEncodingEnum.UTF8) {
            taskData = workerAction.getTaskData().getBytes(StandardCharsets.UTF_8);
        } else if (workerAction.getTaskDataEncoding() == WorkerAction.TaskDataEncodingEnum.BASE64){
            taskData = Base64.decodeBase64(workerAction.getTaskData());
        }

        //set up string for statusCheckUrl
        String statusCheckUrl = config.getWebserviceUrl() +"/jobs/" +jobId +"/isActive";

        //  Construct the task message.
        final TrackingInfo trackingInfo = new TrackingInfo(jobId, calculateStatusCheckDate(config.getStatusCheckTime()),
                statusCheckUrl, config.getTrackingPipe(), workerAction.getTargetPipe());

        final TaskMessage taskMessage = new TaskMessage(
                taskId,
                workerAction.getTaskClassifier(),
                workerAction.getTaskApiVersion(),
                taskData,
                TaskStatus.NEW_TASK,
                Collections.<String, byte[]>emptyMap(),
                targetQueue,
                trackingInfo);

        //  Serialise the task message.
        //  Wrap any CodecException as a RuntimeException as it shouldn't happen
        final byte[] taskMessageBytes;
        try {
            taskMessageBytes = codec.serialise(taskMessage);
        } catch (CodecException e) {
            throw new RuntimeException(e);
        }

        //  Send the message.
        publisherChannel.basicPublish(
                "", targetQueue, MessageProperties.TEXT_PLAIN, taskMessageBytes);
    }

    private Date calculateStatusCheckDate(String statusCheckTime){
        //make sure statusCheckTime is a valid long
        long seconds = 0;
        try{
            seconds = Long.parseLong(statusCheckTime);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Please provide a valid integer for statusCheckTime in seconds. " +e);
        }

        //set up date for statusCheckTime. Get current date-time and add statusCheckTime seconds.
        Instant now = Instant.now();
        Instant later = now.plusSeconds(seconds);
        return java.util.Date.from( later );
    }

    /**
     * Closes the queue connection.
     */
    public void close() throws Exception {
        try {
            //  Close channel.
            if (publisherChannel != null) {
                    publisherChannel.close();
            }

            //  Close connection.
            if (connection != null) {
                    connection.close();
            }

        } catch (IOException | TimeoutException e) {
            throw new Exception("Failed to close the queuing connection.");
        }
    }

}
