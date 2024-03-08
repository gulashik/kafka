PostgreSQL CDC достаём состояние после

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

Создаём таблицу и запишем туда данные
```shell
docker exec -ti postgres psql -U postgres
CREATE TABLE customers (id INT PRIMARY KEY, name TEXT, age INT);
INSERT INTO customers (id, name, age) VALUES (5, 'Fred', 34);
INSERT INTO customers (id, name, age) VALUES (7, 'Sue', 25);
INSERT INTO customers (id, name, age) VALUES (2, 'Bill', 51);
SELECT * FROM customers;
\q
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

Смотрим топики
Видим топик postgres.public.customers
```shell
docker exec kafka1 kafka-topics --list --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
```
Сообщения. payload, тип операции + столбец

Смотрим в UI сообщения
1) [Kafka-ui Link](http://localhost:8081/)
2) [AKHQ UI Link](http://localhost:8080/)

Читаем топик "postgres.public.customers"
```shell
docker exec kafka1 kafka-console-consumer --topic postgres.public.customers --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 --property print.offset=true --property print.key=true --from-beginning
```

Обновляем запись в таблице
```shell
docker exec -ti postgres psql -U postgres
INSERT INTO customers (id, name, age) VALUES (3, 'Ann', 18);
UPDATE customers set age = 35 WHERE id = 5;
DELETE FROM customers WHERE id = 2;
\q
```

Видим новые сообщения в топике

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