Что запущено?
```shell
podman container ps -a
```

Запуск контейнера
```shell
podman-compose up -d
```

Переходим в UI [AKHQ Link](http://localhost:8080/)

Переходим в UI [Kafka-ui Link](http://localhost:8081/)

Создать топик
```shell
podman exec -ti kafka /usr/bin/kafka-topics --create --topic input-data --bootstrap-server localhost:9092
```

Получить список топиков
```shell
podman exec -ti kafka /usr/bin/kafka-topics --list --bootstrap-server localhost:9092
```

Отправить сообщение c ключом через двоеточие (key:value)(Каждая строка - одно сообщение. Прервать - Ctrl+Z)
```shell
podman exec -ti kafka /usr/bin/kafka-console-producer --topic input-data --sync --property "acks=all" --property "parse.key=true" --property "key.separator=:" --bootstrap-server localhost:9092
```

Получить все сообщения по consumer-group consumer-group1
```shell
podman exec -ti kafka /usr/bin/kafka-console-consumer --group consumer-group1 --from-beginning --topic input-data --bootstrap-server localhost:9092 
```

Остановка контейнера
```shell
podman-compose down
```