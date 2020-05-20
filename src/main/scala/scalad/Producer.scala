package main.scala.scalad

import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.{TypeTag, typeOf}

class Producer[K: TypeTag, V: TypeTag] {

  val ser = Map(
    "String" -> "org.apache.kafka.common.serialization.StringSerializer",
    "Int" -> "org.apache.kafka.common.serialization.IntegerSerializer"
  )


  var kafkaProducer: Option[KafkaProducer[K, V]] = None

  def initialise() = {
    val consumerProps = Map(
      "bootstrap.servers"  -> "localhost:9092",
      "key.serializer"   -> ser.getOrElse(typeOf[K].toString, ""),
      "value.serializer" -> ser.getOrElse(typeOf[V].toString, ""),
    )
    val props = new Properties()
    props.putAll(consumerProps.mapValues(_.toString).asJava)
    kafkaProducer = Some( new KafkaProducer[K, V](props))
  }

  def produce(kafkaRecord: KafkaRecord[K, V]) = {
    val record = new ProducerRecord[K, V](kafkaRecord.topic, kafkaRecord.key, kafkaRecord.value)
    kafkaProducer match {
      case Some(producer) => producer.send(record)
      case None => throw UninitializedFieldError("must initialise")
    }
  }

}

object Producer {
  def apply[K: TypeTag, V: TypeTag](): Producer[K, V] = {
    val producer = new Producer[K, V]()
    producer.initialise()
    producer
  }
}
