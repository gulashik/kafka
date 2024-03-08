Файловые источник и приёмник

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
Создаём топик "data"
```shell
docker exec kafka1 kafka-topics --list --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
```
```shell
docker exec kafka1 kafka-topics --create --topic data --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
```
```shell
docker exec kafka1 kafka-topics --list --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094
```
Создаём коннектор load-kafka
```shell
cat ./source.json
```
```shell
curl -X POST --data-binary "@source.json" -H "Content-Type: application/json" http://localhost:8083/connectors | jq
```
Проверяем какие коннекторы есть
```shell
curl http://localhost:8083/connectors | jq
```
Проверяем коннектор load-kafka
```shell
curl http://localhost:8083/connectors/load-kafka/status | jq
```
Смотрим в UI сообщения 
1) [Kafka-ui Link](http://localhost:8081/)
2) [AKHQ UI Link](http://localhost:8080/)

Читаем топик data
```shell
docker exec kafka1 kafka-console-consumer --topic data --bootstrap-server kafka1:19092,kafka2:19093,kafka3:19094 --from-beginning
```
Создаём коннектор dump-kafka
```shell
cat ./dump.json
```
```shell
curl -X POST --data-binary "@dump.json" -H "Content-Type: application/json" http://localhost:8083/connectors | jq
```
Проверяем список коннектор
```shell
curl http://localhost:8083/connectors | jq
```
Проверяем коннектор "dump-kafka"
```shell
curl http://localhost:8083/connectors/dump-kafka/status | jq
```
Проверяем выгрузку данных из топика в файл
```shell
docker exec connect ls -la /data
```
В папке data добавится файл dump.csv

1) Не все сообщения доходят СРАЗУ!!!
2) Иногда вообще не все попадают в файл, хотя в топике есть!! 
Но если в ручную добавить новую строку в файл-источник, то всё(новое и остатки появляются в файле)  

```shell
docker exec connect diff /data/source.csv /data/dump.csv
```

Удаляем коннекторы
```shell
curl -X DELETE http://localhost:8083/connectors/load-kafka
```
```shell
curl -X DELETE http://localhost:8083/connectors/dump-kafka
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