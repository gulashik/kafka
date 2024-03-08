package org.gulash.kfk

import org.apache.avro.reflect.ReflectData


// генерация Avro-схемы в формате JSON
fun main() {
    // Делает все поля NOT null при желании можно исправить
    ReflectData.get().getSchema(Course::class.java)
        .also { println( it.toString(true) )}
    /*
    {
      "type" : "record",
      "name" : "Course",
      "namespace" : "org.gulash.kfk",
      "fields" : [ {
        "name" : "id",
        "type" : "int"
      }, {
        "name" : "title",
        "type" : "string"
      } ]
    }
    */
    // Делает все поля по возможности NULL при желании можно исправить
    ReflectData.AllowNull.get().getSchema(Course::class.java)
        .also { println( it.toString(true) )}
    /*
    {
      "type" : "record",
      "name" : "Course",
      "namespace" : "org.gulash.kfk",
      "fields" : [ {
        "name" : "description",
        "type" : [ "null", "string" ],
        "default" : null
      }, {
        "name" : "id",
        "type" : "int"
      }, {
        "name" : "title",
        "type" : [ "null", "string" ],
        "default" : null
      } ]
    }
    */
}
