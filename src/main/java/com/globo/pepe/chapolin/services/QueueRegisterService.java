package com.globo.pepe.chapolin.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globo.pepe.common.services.AmqpService;
import com.globo.pepe.common.services.JsonLoggerService;
import com.globo.pepe.common.services.JsonLoggerService.JsonLogger;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.amqp.core.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class QueueRegisterService {

    private final AmqpService amqpService;
    private final ObjectMapper mapper;
    private final JsonLoggerService jsonLoggerService;
    private final IStackStormService stackStormService = jsonNode -> {};

    public QueueRegisterService(
        AmqpService amqpService,
        ObjectMapper mapper,
        JsonLoggerService jsonLoggerService) {
        this.amqpService = amqpService;
        this.mapper = mapper;
        this.jsonLoggerService = jsonLoggerService;
    }

    public void register(String queue) {
        JsonLogger logger = jsonLoggerService.newLogger(getClass());
        final MessageListener messageListener = message -> {
            try {
                byte[] messageBody = message.getBody();
                logger.message("send " + new String(messageBody)).put("queue", queue).sendInfo();
                stackStormService.send(mapper.readTree(messageBody));
            } catch (IOException e) {
                logger.message(e.getMessage()).sendError(e);
            }
        };
        amqpService.prepareListenersMap(queue);
        amqpService.registerListener(queue, messageListener);
    }

    @Scheduled(fixedDelayString = "${pepe.chapolin.sync_delay}")
    public void syncDelay() {
        JsonLogger logger = jsonLoggerService.newLogger(getClass());
        if (logger.isDebugEnabled()) {
            logger.message("syncronizing queues").sendDebug();
        }

        final Set<String> queues = amqpService.queuesFromRabbit("pepe.trigger.");
        queues.stream().filter(q -> !amqpService.hasQueue(q)).forEach(this::register);
        final Set<String> queuesRemoved = new HashSet<>(amqpService.queuesRegistered());
        queuesRemoved.removeAll(queues);
        queuesRemoved.forEach(amqpService::stopListener);
    }

    @FunctionalInterface
    interface IStackStormService {
        void send(JsonNode jsonNode);
    }

}