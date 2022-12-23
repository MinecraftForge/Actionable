# Container image that runs your code
FROM amazoncorretto:17.0.5-al2022-RC-headless

COPY dist/app.jar /app.jar

COPY run.sh /run.sh
RUN chmod +x /run.sh

ENTRYPOINT /bin/bash /run.sh