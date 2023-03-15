package datasources;

import com.hazelcast.jet.pipeline.Sources;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;

import javax.jms.Session;
import javax.jms.Topic;
import java.util.HashMap;
import java.util.Map;

// TODO!
public class RabbitMqDataSource {
    private RMQConnectionFactory rmqConnectionFactory;
    private RMQDestination jmsDestination;

    public RabbitMqDataSource(String host, int port, String password, String username, String destinationName) {
        rmqConnectionFactory = new RMQConnectionFactory();
        rmqConnectionFactory.setPassword(password);
        rmqConnectionFactory.setHost(host);
        rmqConnectionFactory.setPort(port);
        rmqConnectionFactory.setUsername(username);
//        rmqConnectionFactory.setSsl(true);
        Map<String, Object> args = new HashMap<String, Object>();
        //        RMQDestination jmsDestination = new RMQDestination("opensky", "opensky", "opensky", "flight-queue");
        args.put("x-message-ttl", 60000);
        args.put("auto_delete", true);
        jmsDestination = new RMQDestination(destinationName, false, false, args);

        jmsDestination.setAmqpRoutingKey("flight-queue");
        jmsDestination.setAmqpExchangeName("opensky");
        jmsDestination.setDestinationName("opensky");
        jmsDestination.setAmqp(true);
    }

    public com.hazelcast.jet.pipeline.StreamSource<javax.jms.Message> getDataSource() {
        return Sources
                .jmsTopicBuilder(() -> {
                    this.rmqConnectionFactory.createTopicConnection()
                            .createTopicSession(false, Session.AUTO_ACKNOWLEDGE)
                            .createTopic(jmsDestination.getTopicName());
                    return this.rmqConnectionFactory;
                })
                .consumerFn((session -> {
                    Topic topic = session.createTopic(jmsDestination.getTopicName());
                    session.createDurableSubscriber(topic, jmsDestination.getAmqpQueueName());
                    return session.createConsumer(jmsDestination);
                }))
                .sharedConsumer(true)
                .build();
    }
}
