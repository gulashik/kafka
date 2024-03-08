Avro schema <br>
[user.avsc](src%2Fmain%2Favro%2Fuser.avsc)

Перегенерить можно 
1) явно через task **other.customAvroCodeGeneration** (создали таск) [build.gradle.kts](build.gradle.kts)
2) не явно **через build**(добавили зависимость) [build.gradle.kts](build.gradle.kts)

Результат генерации в <br>
[User.java](src%2Fmain%2Fjava%2Forg%2Fgulash%2Fkfk%2FUser.java)

Класс доступен <br>
[Main.java](src%2Fmain%2Fjava%2Forg%2Fgulash%2Fkfk%2FMain.java)

Много настроек <br> 
[build.gradle.kts](build.gradle.kts)