Avro schema <br>
[user.avsc](avro/user.avsc)

Перегенерить можно 
1) явно через task **other.customAvroCodeGeneration** (создали таск) [build.gradle](../../build.gradle)
2) не явно через build(добавили зависимость) [build.gradle](../../build.gradle)

Результат генерации в <br>
[User.java](java/org/gulash/kfk/avro/model/User.java)

Класс доступен <br>
[org.gulash.kfk.Main.java](java/org/gulash/Fkfk/org.gulash.kfk.Main.java)

Много настроек <br> 
[build.gradle](../../build.gradle)