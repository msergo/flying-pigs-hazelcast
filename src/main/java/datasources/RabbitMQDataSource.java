package datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import models.OpenSkyStates;

import javax.jms.*;

public class RabbitMQDataSource {
    public static StreamSource<OpenSkyStates> getDataSource() {
        return SourceBuilder.timestampedStream("rabbit-source", context -> {
                    RMQConnectionFactory rmqConnectionFactory = new RMQConnectionFactory();
                    rmqConnectionFactory.setHost("0.0.0.0");
                    rmqConnectionFactory.setPort(5672);
                    rmqConnectionFactory.setUsername("user");
                    rmqConnectionFactory.setPassword("bitnami");
                    Connection connection = rmqConnectionFactory.createTopicConnection();
                    connection.start();

                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Topic topic = session.createTopic("#");
                    session.createDurableSubscriber(topic, "estonia-queue");

                    RMQDestination jmsDestination = new RMQDestination("estonia-queue", true, false);
                    jmsDestination.setDestinationName("#"); // routing key
                    jmsDestination.setAmqp(true);
                    jmsDestination.setAmqpExchangeName("flying-pigs-exchange");

                    MessageConsumer consumer = session.createConsumer(jmsDestination);
                    return consumer;
                })
                .fillBufferFn((MessageConsumer consumer, SourceBuilder.TimestampedSourceBuffer<OpenSkyStates> buffer) -> {
                    try {
                        Message message = consumer.receive();
                        BytesMessage bytesMessage = (BytesMessage) message;
                        byte[] byteData;
                        byteData = new byte[(int) (bytesMessage).getBodyLength()];
                        bytesMessage.readBytes(byteData);
                        bytesMessage.reset();
                        ObjectMapper mapper = new ObjectMapper();
                        OpenSkyStates openSkyStates = mapper.readValue(byteData, OpenSkyStates.class);
                        buffer.add(openSkyStates);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }
}