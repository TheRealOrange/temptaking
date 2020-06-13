FROM gradle:6.5.0-jdk14 AS build

RUN mkdir /build
WORKDIR /build

RUN gradle -g .gradle

COPY ./gradle.properties ./
COPY build.gradle.kts ./
COPY ./settings.gradle ./
RUN gradle -g .gradle downloadDependencies

COPY ./src ./src
RUN gradle -g .gradle shadowJar --info

RUN mkdir /app
WORKDIR /app
RUN cp /build/build/libs/temptaking-* ./temptaking.jar

FROM openjdk:14-alpine AS run

RUN apk update
RUN apk add chromium
RUN apk add chromium-chromedriver

ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/

RUN mkdir /app
WORKDIR /app
COPY --from=build /app/temptaking.jar ./
COPY ./config ./app_config
RUN ls

CMD ["java", "-jar", "./temptaking.jar"]