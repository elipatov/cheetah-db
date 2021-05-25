FROM adoptopenjdk/openjdk15

RUN mkdir -p /opt/cheetah-db
WORKDIR /opt/cheetah-db
COPY ./core/target/scala-2.13/cheetah-db-core.jar ./

EXPOSE 80

ENTRYPOINT ["java", \
            "-jar", \
            "cheetah-db-core.jar" \
]
