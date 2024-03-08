Что запущено?
```shell
podman container ps -a
```

Запуск контейнера
```shell
podman-compose -f kafka-kraft_b3-compose.yml up
```

Переходим в UI [Kafka-ui Link](http://localhost:8080/)

Создать топик
```shell
podman exec -ti kafka-1 /opt/bitnami/kafka/bin/kafka-topics.sh --create --topic mytopic --partitions 1 --replication-factor 1 --bootstrap-server localhost:9092
```

Получить список топиков
```shell
podman exec -ti kafka-1 /opt/bitnami/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

Отправить сообщение(Каждая строка - одно сообщение. Прервать - Ctrl+Z)
```shell
podman exec -ti kafka-1 /opt/bitnami/kafka/bin/kafka-console-producer.sh --topic mytopic --bootstrap-server localhost:9092
```

Отправить сообщение c ключом через двоеточие (key:value)
```shell
podman exec -ti kafka-1 /opt/bitnami/kafka/bin/kafka-console-producer.sh --topic mytopic --property "parse.key=true" --property "key.separator=:" --bootstrap-server localhost:9092
```

Получить все сообщения по consumer-group consumer-group1
```shell
podman exec -ti kafka-1 /opt/bitnami/kafka/bin/kafka-console-consumer.sh --group consumer-group1 --from-beginning --topic mytopic --bootstrap-server localhost:9092 
```

Остановка контейнера
```shell
podman-compose -f kafka-kraft_b3-compose.yml down
```