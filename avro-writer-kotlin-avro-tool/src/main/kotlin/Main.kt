package  org.gulash.kfk

import org.apache.avro.Schema
import org.apache.avro.io.BinaryDecoder
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream


fun main() {
    val user1: User = User.newBuilder().apply {
        name = "test"
        favoriteColor = "white"
        favoriteNumber = 1
        email = "test@example.com"
    }.build()

    val user2: User = User.newBuilder().apply {
        name = "test2"
        favoriteColor = "white2"
        favoriteNumber = 12
        email = "test@example2.com"
    }.build()

    user1.also(::println)
    user2.also(::println)
    /*{"name": "test", "email": "test@example.com", "favorite_number": 1, "favorite_color": "white"}
      {"name": "test2", "email": "test@example2.com", "favorite_number": 12, "favorite_color": "white2"}*/


    //-------------------------------------------------------------------------------------------
    // Переводим в массив байт и обратно согласно схеме(информация лежит в классе или файле )
    //-------------------------------------------------------------------------------------------
    // Пишем в Outstream
    val byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()

    // Кодируем согласно схеме(информация лежит в классе)
    val encoder = EncoderFactory.get().binaryEncoder(byteArrayOutputStream, null)
    val specificDatumWriter: SpecificDatumWriter<User> = SpecificDatumWriter(User::class.java)
    // записываем информацию
    specificDatumWriter.write(user1, encoder)
    specificDatumWriter.write(user2, encoder)

    encoder.flush()

    // Читаем из InputStream
    val bytes = byteArrayOutputStream.toByteArray()
    val byteArrayInputStream: ByteArrayInputStream = ByteArrayInputStream(bytes)

    //*Можно запустить только одину из функций. На вторую stream-а не хватит*//

    // Декодируем согласно схеме(информация лежит в классе)
    //readSchemaFromClass(byteArrayInputStream)

    // Декодируем согласно схем писателя и кладём в объект согласно схемы читателя
    readSchemaFromSchemaFile(byteArrayInputStream)
}
 /**Декодируем согласно схеме(информация читателя лежит в классе писателя в файле)*/
 fun readSchemaFromSchemaFile(inputStream: InputStream) {
     // Смысла тут не много, но в теории может объект прийти новой версии шире, а мы возьмём инфу только по старой версии
     val readerSchema: Schema = User.getClassSchema()
     val writerSchema: Schema = Schema.Parser().parse(File("avro-writer-kotlin-avro-tool/src/main/avro/user.avsc"))

     val userDatumReader = SpecificDatumReader<User>(writerSchema, readerSchema)
     val decoder = DecoderFactory.get().binaryDecoder(inputStream, null)

     val u1 = userDatumReader.read(null, decoder)
     val u2 = userDatumReader.read(null, decoder)

    println(u1)
    println(u2)
}

 /**Декодируем согласно схеме(информация лежит в классе)*/
 fun readSchemaFromClass(byteArrayInputStream: ByteArrayInputStream) {

    val decoder: BinaryDecoder = DecoderFactory.get().binaryDecoder(byteArrayInputStream, null)
    val userDatumReader: SpecificDatumReader<User> = SpecificDatumReader(User::class.java)

     // читаем информацию в том же порядке
    val u1 = userDatumReader.read(null, decoder)
    val u2 = userDatumReader.read(null, decoder)

    println(u1)
    println(u2)
    /*{"name": "test", "email": "test@example.com", "favorite_number": 1, "favorite_color": "white"}
      {"name": "test2", "email": "test@example2.com", "favorite_number": 12, "favorite_color": "white2"}*/
}