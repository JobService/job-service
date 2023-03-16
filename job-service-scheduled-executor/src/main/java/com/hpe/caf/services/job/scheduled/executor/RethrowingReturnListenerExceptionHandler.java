package com.hpe.caf.services.job.scheduled.executor;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.StrictExceptionHandler;

final class RethrowingReturnListenerExceptionHandler extends StrictExceptionHandler
{
    @Override
    public void handleReturnListenerException(final Channel channel, final Throwable exception)
    {
        throw new RuntimeException(exception);
    }
}
