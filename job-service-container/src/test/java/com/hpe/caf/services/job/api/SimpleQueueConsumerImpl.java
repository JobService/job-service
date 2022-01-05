/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.services.job.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.QueueTaskMessage;
import com.hpe.caf.util.rabbitmq.Delivery;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.rabbitmq.client.Channel;

public class SimpleQueueConsumerImpl implements QueueConsumer
{
    private final BlockingQueue<Event<QueueConsumer>> eventQueue;
    private final Channel channel;
    private final ResultHandler resultHandler;
    private final Codec codec;
    private final ArrayList<Delivery> deliveries = new ArrayList();
    private static final Object syncLock = new Object();

    public SimpleQueueConsumerImpl(BlockingQueue<Event<QueueConsumer>> queue, Channel channel, ResultHandler resultHandler, Codec codec)
    {
        this.eventQueue = queue;
        this.channel = channel;
        this.resultHandler = resultHandler;
        this.codec = codec;
    }

    public void processDelivery(Delivery delivery)
    {
        try {
            final QueueTaskMessage taskMessage = this.codec.deserialise(delivery.getMessageData(), QueueTaskMessage.class,
                                                                        DecodeMethod.LENIENT);
            synchronized (syncLock) {
                this.resultHandler.handleResult(taskMessage);
            }
        } catch (CodecException var6) {
            var6.printStackTrace();
            throw new AssertionError("Failed: " + var6.getMessage());
        } catch (Exception var7) {
            var7.printStackTrace();
            throw new AssertionError("Failed: " + var7.getMessage());
        }
    }

    public void processAck(long tag)
    {
        try {
            this.channel.basicAck(tag, false);
        } catch (IOException var4) {
            var4.printStackTrace();
        }
    }

    public void processReject(long tag)
    {
    }

    public void processDrop(long tag)
    {
    }
}
