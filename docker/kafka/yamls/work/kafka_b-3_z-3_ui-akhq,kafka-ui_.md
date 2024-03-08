Что запущено?
```shell
podman container ps -a
```

Запуск контейнера
```shell
podman-compose -f kafka_b-3_z-3_ui-akhq,kafka-ui.yaml up
```

Переходим в UI [AKHQ Link](http://localhost:8080/)

Переходим в UI [Kafka-ui Link](http://localhost:8081/)

Создать топик
```shell
podman exec -ti kafka1 /usr/bin/kafka-topics --create --topic mytopic --partitions 3 --replication-factor 3 --bootstrap-server localhost:19092, localhost:19093 ,localhost:19094
```

Получить список топиков
```shell
podman exec -ti kafka1 /usr/bin/kafka-topics --list --bootstrap-server localhost:19092
```

Отправить сообщение(Каждая строка - одно сообщение. Прервать - Ctrl+Z)
```shell
podman exec -ti kafka1 /usr/bin/kafka-console-producer --topic mytopic --sync --property "acks=all" --bootstrap-server localhost:19092, localhost:19093 ,localhost:19094
```

Отправить сообщение c ключом через двоеточие (key:value)
```shell
podman exec -ti kafka1 /usr/bin/kafka-console-producer --topic mytopic --sync --property "acks=all" --property "parse.key=true" --property "key.separator=:" --bootstrap-server localhost:19092, localhost:19093 ,localhost:19094
```

Получить все сообщения по consumer-group consumer-group1
```shell
podman exec -ti kafka1 /usr/bin/kafka-console-consumer --group consumer-group1 --from-beginning --topic mytopic --bootstrap-server localhost:19092 
```

Остановка контейнера
```shell
podman-compose -f kafka_b-3_z-3_ui-akhq,kafka-ui.yaml down
```