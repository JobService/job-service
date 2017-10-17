/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.services.job.queue.services.queue;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * This class is responsible for sending task data to the target queue.
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
    public void sendMessage(TaskMessage tm) throws IOException
    {
        //  Serialise the task message.
        //  Wrap any CodecException as a RuntimeException as it shouldn't happen
        final byte[] taskMessageBytes;
        try {
            taskMessageBytes = codec.serialise(tm);
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
