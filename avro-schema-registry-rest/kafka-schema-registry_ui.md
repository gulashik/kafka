Что запущено?
```shell
podman container ps -a
```

Запуск контейнера
```shell
podman compose -f ../docker/kafka/yamls/work/kafka-schema-registry_ui.yml up -d 
```

Переходим в UI 
<br> [AKHQ Link](http://localhost:8082/)
<br> [Kafka-ui Link](http://localhost:8083/)

REST для работы Schema registry <br>
[kafka-schema-registry_ui.rest](..%2Fdocker%2Fkafka%2Fyamls%2Fwork%2Fkafka-schema-registry_ui.rest)

Остановка контейнера
```shell
podman compose -f ../docker/kafka/yamls/work/kafka-schema-registry_ui.yml down
```