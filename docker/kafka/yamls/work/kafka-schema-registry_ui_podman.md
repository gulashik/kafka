Что запущено?
```shell
podman container ps -a
```

Запуск контейнера
```shell
podman compose -f kafka-schema-registry_ui_podman.yml up -d 
```
**На ARM процессорах хреново работает!**

Переходим в UI 
<br> [AKHQ Link](http://localhost:8082/)
<br> [Kafka-ui Link](http://localhost:8083/)

REST для работы Schema registry <br>
[kafka-schema-registry_ui.rest](kafka-schema-registry_ui.rest)

Остановка контейнера
```shell
podman compose -f kafka-schema-registry_ui_podman.yml down
```