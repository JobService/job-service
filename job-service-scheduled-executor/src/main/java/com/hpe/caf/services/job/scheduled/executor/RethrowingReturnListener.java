package com.hpe.caf.services.job.scheduled.executor;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ReturnListener;

final class RethrowingReturnListener implements ReturnListener
{
    @Override
    public void handleReturn(final int replyCode,
                             final String replyText,
                             final String exchange,
                             final String routingKey,
                             final AMQP.BasicProperties properties,
                             final byte[] body)
    {
        final String errorMessage = String.format(
                "Message returned from RabbitMQ: replyCode=%d, replyText=%s, exchange=%s, routingKey=%s, properties=%s, body=%s",
                replyCode, replyText, exchange, routingKey, properties, new String(body));

        throw new RuntimeException(errorMessage);
    }
}
