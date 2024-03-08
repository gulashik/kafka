Запуск контейнера
```shell
cd ./kafka/yamls/work
podman-compose -f kafka_b-3_z-1_ui-akhq,kafka-ui.yaml up
```

Переходим в Kafak UI [AKHQ UI Link](http://localhost:8080/)

Остановка контейнера
```shell
cd ./kafka/yamls/work
podman-compose -f kafka_b-3_z-1_ui-akhq,kafka-ui.yaml down
```
