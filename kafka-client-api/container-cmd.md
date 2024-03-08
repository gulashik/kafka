
Что запущено?
```shell
podman container ps -a
```

Запуск контейнера
```shell
cd ../docker/kafka/yamls/work
pwd 
podman-compose -f kafka_b-1_z-1_ui-akhq.yaml up
```
Переходим в UI [Kafka-ui Link](http://localhost:8081/)

Переходим в Kafak UI [AKHQ UI Link](http://localhost:8080/)

Создать топик
```shell
podman exec -ti kafka1 /usr/bin/kafka-topics --create --topic mytopic --partitions 1 --replication-factor 1 --bootstrap-server localhost:9092
```

Получить список топиков
```shell
podman exec -ti kafka1 /usr/bin/kafka-topics --list --bootstrap-server localhost:9092
```

Отправить сообщение(Каждая строка - одно сообщение. Прервать - Ctrl+Z)
```shell
podman exec -ti kafka1 /usr/bin/kafka-console-producer --topic mytopic --bootstrap-server localhost:9092
```

Отправить сообщение c ключом через двоеточие (key:value)
```shell
podman exec -ti kafka1 /usr/bin/kafka-console-producer --topic mytopic --property "parse.key=true" --property "key.separator=:" --bootstrap-server localhost:9092
```

Получить все сообщения по consumer-group consumer-group1
```shell
podman exec -ti kafka1 /usr/bin/kafka-console-consumer --group consumer-group1 --from-beginning --topic mytopic --bootstrap-server localhost:9092 
```

Остановка контейнера
```shell
cd ../docker/kafka/yamls/work
pwd 
podman-compose -f kafka_b-1_z-1_ui-akhq.yaml down
```