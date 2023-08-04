# flying-pigs-hazelcast
Hazelcast-jet job for processing flights from OpenSky.

This project aims to showcase the capabilities of the Hazelcast Jet framework in a real-world data processing scenario. 
It involves processing flight data, derived from the public OpenSky Network, to provide insights and notifications.

### Overview

Flying Pigs Data Processing Project leverages Hazelcast Jet, a powerful open-source data processing engine, to analyze and process flight data. 
The project demonstrates how to build a data processing pipeline that extracts insights from flight data and sends notifications using various data sources and sinks.
In default implementation is gets data from a RabbitMQ exchange and posts results to a remote API with HTTP requests.
More details in [the article about it](https://sergiiwrites.online/2023/08/04/flying-pigs.html).

### License
MIT
