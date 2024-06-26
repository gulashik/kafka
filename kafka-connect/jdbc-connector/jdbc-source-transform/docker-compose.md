JDBC Source (PostgreSQL), преобразования

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
Смотрим топики
```shell
docker exec kafka1 kafka-topics --list --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
```

Подключаемся к базе и загружаем данные
```shell
docker exec -ti postgres psql -U postgres

CREATE TABLE clients (id int PRIMARY KEY, first_name text, last_name text, gender text, card_number text, bill numeric(7,2), created_date timestamp, modified_date timestamp);

COPY clients FROM '/data/Demo.csv' WITH (FORMAT csv, HEADER true);

SELECT * FROM clients LIMIT 5;
\q
```

Создаём коннектор "clients-smt-connector"

Чуть другой пример из документации https://docs.confluent.io/kafka-connectors/jdbc/current/source-connector/overview.html#message-keys

```shell
cat ./clients-smt.json
```
```shell
curl -X POST --data-binary "@clients-smt.json" -H "Content-Type: application/json" http://localhost:8083/connectors | jq
```
Проверяем какие коннекторы есть
```shell
curl http://localhost:8083/connectors | jq
```
Проверяем коннектор "clients-smt-connector"
```shell
curl http://localhost:8083/connectors/clients-smt-connector/status | jq
```
Смотрим в UI сообщения 
1) [Kafka-ui Link](http://localhost:8081/)
2) [AKHQ UI Link](http://localhost:8080/)

Проверим смещение в топике "postgres.clients"

```shell
docker exec kafka1 kafka-get-offsets --topic postgres.clients --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
```

Читаем топик "postgres.clients"

Видим маскированые сообщения и в UI видим header-ы в сообщении
```shell
docker exec kafka1 kafka-console-consumer --topic postgres.clients --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 --from-beginning --property print.offset=true
```

Удаляем коннектор
```shell
curl -X DELETE http://localhost:8083/connectors/clients-smt-connector
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