package com.knowledge.learning.progress.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.learning.dto.VideoProgressReportedEventDTO;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.entitlement.config.LearningRabbitConfiguration;
import java.time.Instant;
import java.net.ConnectException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProgressEventPublisherTest {
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ObjectMapper objectMapper;
    @InjectMocks ProgressEventPublisher publisher;
    private final VideoProgressReportedEventDTO event = new VideoProgressReportedEventDTO("event", "u", "c", "ch",
            "v", 1, "s", 100, 1, "PAUSE", 10, 100, List.of(), Instant.EPOCH, Instant.EPOCH, 1);

    @BeforeEach
    void prepare() throws Exception {
        ReflectionTestUtils.setField(publisher, "confirmTimeoutMs", 20L);
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
    }

    private void confirm(boolean ack, boolean returned) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            if (returned) {
                correlation.setReturned(new ReturnedMessage(new Message(new byte[0]), 312, "NO_ROUTE", "exchange", "route"));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(LearningRabbitConfiguration.EVENT_EXCHANGE),
                eq(LearningRabbitConfiguration.PROGRESS_ROUTING_KEY), eq("{}"), any(CorrelationData.class));
    }

    @Test void ackWithoutReturnIsAccepted() {
        confirm(true, false);
        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }

    @Test void nackIsUnavailable() {
        confirm(false, false);
        assertThatThrownBy(() -> publisher.publish(event)).isInstanceOf(BusinessException.class);
    }

    @Test void ackWithUnroutableReturnIsNotAccepted() {
        confirm(true, true);
        assertThatThrownBy(() -> publisher.publish(event)).isInstanceOf(BusinessException.class);
    }

    @Test void confirmTimeoutIsUnknownAndMustBeRetriedWithSameEvent() {
        assertThatThrownBy(() -> publisher.publish(event)).isInstanceOf(BusinessException.class);
    }

    @Test void brokerConnectionFailureIsUnavailable() {
        doAnswer(invocation -> { throw new AmqpConnectException(new ConnectException()); })
                .when(rabbitTemplate).convertAndSend(eq(LearningRabbitConfiguration.EVENT_EXCHANGE),
                        eq(LearningRabbitConfiguration.PROGRESS_ROUTING_KEY), eq("{}"), any(CorrelationData.class));
        assertThatThrownBy(() -> publisher.publish(event)).isInstanceOf(BusinessException.class);
    }
}
