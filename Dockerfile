FROM maven:3.8.4-jdk-11
COPY ./. /software/
WORKDIR /software/
RUN mvn install
RUN rm -r /software/src/main/java/*
ENTRYPOINT ["timeout","2m","mvn","test"]
