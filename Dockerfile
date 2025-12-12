FROM openjdk:26-ea-24-jdk-oraclelinux9

WORKDIR /home
COPY target/chat-1.0.0.jar chat-1.0.0.jar

ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "chat-1.0.0.jar"]