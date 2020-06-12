FROM openjdk:12-alpine

RUN apk update
RUN apk add chromium
RUN apk add chromium-chromedriver

ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/

WORKDIR /build

RUN rm ./build/libs/*
RUN ./gradlew shadowJar --info

RUN mkdir /app
WORKDIR /app
RUN mv /build/libs/temptaking-* ./

CMD ["java", "-jar", "./build/libs/temptaking.jar"]