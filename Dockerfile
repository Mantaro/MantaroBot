FROM openjdk:10-jre-slim

ARG version
ARG jattachVersion

RUN apt-get update
RUN apt-get install -y wget
RUN wget https://github.com/apangin/jattach/releases/download/$jattachVersion/jattach -O /bin/jattach
RUN chmod +x /bin/jattach

WORKDIR /mantaro

COPY assets assets
COPY assets/Mantaro-${version}.jar mantaro.jar

CMD [ \
     "java", \
     # Detect actual memory limit (remove when upadting to 11+, as they have this on by default)
     "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", \
     "-jar", "mantaro.jar" \
]