JDBC Sink (PostgreSQL)

Проверяем чего есть
```shell
docker compose ps -a
```

Запускаем Kafka и Kafka Connect
```shell
docker compose up -d
```

Проверям логи Kafka Connect(выйти ^C)
```shell
docker logs -f connect
```

Проверяем статус Kafka Connect
```shell
curl http://localhost:8083 | jq
```

Проверяем плагины коннекторов
```shell
curl http://localhost:8083/connector-plugins | jq
```

Подключаемся к базе и проверяем таблицы(таблиц нет)
```shell
docker exec -ti postgres psql -U postgres
\d
\q
```

Создаём топик "customers"
```shell
docker exec kafka1 kafka-topics --create --topic customers --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
```

Смотрим топики
```shell
docker exec kafka1 kafka-topics --list --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
```

Запишем несколько сообщений в топик customers(заканчиваем ^D)
```shell
docker exec -ti kafka1 kafka-console-producer --topic customers --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
{"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"}]},"payload":{"id":1,"name":"Jane Doe"}}
{"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"}]},"payload":{"id":2,"name":"John Smith"}}
{"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"}]},"payload":{"id":3,"name":"Ann Black"}}
```

Смотрим в UI сообщения
1) [Kafka-ui Link](http://localhost:8081/)
2) [AKHQ UI Link](http://localhost:8080/)

Проверяем сообщения в топике "customers"(выходим ^C)
```shell
docker exec -ti kafka1 kafka-console-consumer --topic customers --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 --from-beginning --property print.offset=true
```

Создаём коннектор "customers-connector"
```shell
cat ./customers.json
```
```shell
curl -X POST --data-binary "@customers.json" -H "Content-Type: application/json" http://localhost:8083/connectors | jq
```

Проверяем какие коннекторы есть
```shell
curl http://localhost:8083/connectors | jq
```

Проверяем коннектор "customers-connector"
```shell
curl http://localhost:8083/connectors/customers-connector/status | jq
```

Подключаемся к базе и проверяем таблицы
```shell
docker exec -ti postgres psql -U postgres
\d
SELECT * FROM customers;
\q
```

Запишем несколько сообщений в топик customers(заканчиваем ^D)
```shell
docker exec -ti kafka1 kafka-console-producer --topic customers --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
{"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"}]},"payload":{"id":4,"name":"Agatha Christie"}}
{"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"}]},"payload":{"id":5,"name":"Arthur Conan Doyle"}}
{"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"}]},"payload":{"id":6,"name":"Edgar Allan Poe"}}
```

Подключаемся к базе и проверяем таблицу customers
```shell
docker exec -ti postgres psql -U postgres
SELECT * FROM customers;
\q
```

Проверяем сообщения в топике "customers"(выходим ^C)
```shell
docker exec -ti kafka1 kafka-console-consumer --topic customers --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 --from-beginning --property print.offset=true
```

Удаляем коннектор
```shell
curl -X DELETE http://localhost:8083/connectors/customers-connector
```

```shell
curl http://localhost:8083/connectors | jq
```

Останавливаем Kafka и Kafka Connect
```shell
docker compose stop
docker container prune -f
docker volume prune -f
```