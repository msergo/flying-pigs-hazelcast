FROM maven:3-eclipse-temurin-11 as builder
WORKDIR /app
COPY src /app/src
COPY pom.xml /app
RUN mvn -f /app/pom.xml clean package


FROM hazelcast/hazelcast-jet
WORKDIR /jobs
COPY --from=builder /app/target/flying-pigs-hazelcast-1.0-SNAPSHOT-jar-with-dependencies.jar /jobs/job.jar
# copy config files to classpath to make it available for the job
COPY --from=builder /app/src/main/resources/ /opt/hazelcast-jet/lib

ENV JET_ADDRESS hazelcast-jet-service
CMD ["sh", "-c", "jet -t $JET_ADDRESS submit /jobs/job.jar"]
