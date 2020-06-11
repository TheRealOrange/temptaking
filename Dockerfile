FROM openjdk:12-alpine

RUN apk update
RUN apk add chromium
RUN apk add chromium-chromedriver

ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/

RUN mkdir /app
COPY . /app

RUN rm /app/build/libs/*
RUN /app/gradlew shadowJar --no-daemon

CMD ["java", "/app/build/libs/temptaking-*"]