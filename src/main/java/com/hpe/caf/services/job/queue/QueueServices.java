package com.hpe.caf.services.job.queue;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.binary.Base64;

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
    public void sendMessage(WorkerAction workerAction) throws IOException
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

        //  Construct the task message.
        final TaskMessage taskMessage = new TaskMessage(
                taskId,
                workerAction.getTaskClassifier(),
                workerAction.getTaskApiVersion(),
                taskData,
                TaskStatus.NEW_TASK,
                Collections.<String, byte[]>emptyMap());

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
