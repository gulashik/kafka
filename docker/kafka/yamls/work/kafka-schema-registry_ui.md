Что запущено?
```shell
docker container ps -a
```

Запуск контейнера
```shell
docker compose -f kafka-schema-registry_ui.yml up -d 
```
**На ARM процессорах хреново работает!**

Переходим в UI 
<br> [AKHQ Link](http://localhost:8082/)
<br> [Kafka-ui Link](http://localhost:8083/)
<br> [Kafdrop Link](http://localhost:9000/)

REST для работы Schema registry <br>
[kafka-schema-registry_ui.rest](kafka-schema-registry_ui.rest)

Остановка контейнера
```shell
docker compose -f kafka-schema-registry_ui.yml down
```