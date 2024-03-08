Что запущено?
```shell
podman container ps -a
```

Запуск контейнера
```shell
podman-compose -f kafka-with-jmx.yml up
```

Запуск jconsole
```shell
jconsole 
```
указываем 
 - Zookeeper - localhost:9101
 - Kafka Broker - localhost:9102

![kafka-jconsole-01.jpg](./pictures/kafka-jconsole-01.jpg)

![kafka-jconsole-02.jpg](./pictures/kafka-jconsole-02.jpg)

![kafka-jconsole-03.jpg](./pictures/kafka-jconsole-03.jpg)

Остановка контейнера
```shell
podman-compose -f kafka-with-jmx.yml down
```

--------

