package org.msergo.flyingpigshazelcast.datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import com.typesafe.config.Config;
import org.msergo.flyingpigshazelcast.models.Location;
import org.msergo.flyingpigshazelcast.models.StateVector;
import org.msergo.flyingpigshazelcast.config.ConfigManager;
import org.msergo.flyingpigshazelcast.models.StateVectorsResponse;

import javax.jms.*;

public class RabbitMQDataSource {
    public static StreamSource<StateVector> getDataSource(Location location) {
        Config config = ConfigManager.getConfig();
        String destinationQueueName = location.getName().toLowerCase() + "-queue";

        return SourceBuilder.timestampedStream("rabbit-source", context -> {
                    RMQConnectionFactory rmqConnectionFactory = new RMQConnectionFactory();
                    rmqConnectionFactory.setHost(config.getString("rabbitmq.host"));
                    rmqConnectionFactory.setPort(config.getInt("rabbitmq.port"));
                    rmqConnectionFactory.setUsername(config.getString("rabbitmq.user"));
                    rmqConnectionFactory.setPassword(config.getString("rabbitmq.password"));

                    Connection connection = rmqConnectionFactory.createTopicConnection();
                    connection.start();

                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Topic topic = session.createTopic("#");
                    session.createDurableSubscriber(topic, destinationQueueName);

                    RMQDestination jmsDestination = new RMQDestination(destinationQueueName, true, false);
                    jmsDestination.setDestinationName("#"); // routing key
                    jmsDestination.setAmqp(true);
                    jmsDestination.setAmqpExchangeName("flying-pigs-exchange");

                    MessageConsumer consumer = session.createConsumer(jmsDestination);
                    return consumer;
                })
                .fillBufferFn((MessageConsumer consumer, SourceBuilder.TimestampedSourceBuffer<StateVector> buffer) -> {
                    try {
                        Message message = consumer.receive();
                        BytesMessage bytesMessage = (BytesMessage) message;
                        byte[] byteData;
                        byteData = new byte[(int) (bytesMessage).getBodyLength()];
                        bytesMessage.readBytes(byteData);
                        bytesMessage.reset();
                        ObjectMapper mapper = new ObjectMapper();
                        StateVectorsResponse stateVectorsResponse = mapper.readValue(byteData, StateVectorsResponse.class);
                        // Add all the state vectors to the buffer
                        stateVectorsResponse.getStates().forEach(buffer::add);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .destroyFn(MessageConsumer::close)
                .build();
    }
}