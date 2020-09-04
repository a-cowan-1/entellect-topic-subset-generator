package main.scala.materialise


import attempt.{KafkaRecord, Producer}
import main.scala.scalad.Consumer
import main.scala.streamed.Stream
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.state.{KeyValueIterator, QueryableStoreTypes, ReadOnlyKeyValueStore, Stores}
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsConfig}

import scala.annotation.tailrec
import scala.collection.JavaConverters._

// https://sachabarbs.wordpress.com/2019/01/28/kafkastreams-aggregating/

class Main {
  val storeName = "Count-store"
  val storeSupplier = Stores.inMemoryKeyValueStore(storeName)
  val materialized: Materialized[String, Long, ByteArrayKeyValueStore] = Materialized
    .as[String, Long](storeSupplier)
    .withLoggingEnabled(Map.empty[String, String].asJava)

  def topologyPlan(): StreamsBuilder = {
    import org.apache.kafka.streams.scala.Serdes._
    val builder= new StreamsBuilder
    val fibValues: KStream[String, String] = builder.stream[String, String]("fib")
    fibValues.to("uncounted")
    val groupedStream: KGroupedStream[String, String] = fibValues.groupBy((k, v) => (k.toInt/3).toString)
    val counted: KTable[String, Long] = groupedStream.count()(materialized)
    counted.toStream.to("counted")
    builder
  }

  def runFibGenerator(producer: Producer[String, String]) = {
    @tailrec def fib(x: BigInt, xm1: BigInt, countdown: Int, func: (BigInt, Int) => Unit): Unit = {
      func(x, countdown)
      countdown match {
        case 0 =>
        case _ => fib(x + xm1, x, countdown - 1, func)
      }
    }

    fib(1, 0, 49, (x: BigInt, count: Int) => {
      producer.produce(KafkaRecord("fib", 0, 0, count.toString, x.toString()))
      Thread.sleep(200)
    })
  }

  def readMaterializedStore(kafkaStreams: KafkaStreams): Unit = {
    val keyValueStore: ReadOnlyKeyValueStore[String, Long] = kafkaStreams.store(storeName, QueryableStoreTypes.keyValueStore[String, Long]())
    println(s"value: ${keyValueStore.get("10")}")
    val iter: Iterator[KeyValue[String, Long]] = keyValueStore.all().asScala
    for (item <- iter) {
      println(s"key: ${item.key}, val: ${item.value}")
    }
  }

  def run() = {
    val fibProducer = Producer[String, String]()
    val rawConsumer = Consumer[String, String]("grp1", "776")
    val transformerStream = Stream(topologyPlan())
    val consumer2 = Consumer[String, String]("grp2", "554")
    val consumer3 = Consumer[String, Long]("grp3", "544")

    rawConsumer.subscribe("fib", x=>println(s"raw= $x"))
    consumer2.subscribe("uncounted", x=>println(s"uncounted = $x"))
    consumer3.subscribe("counted", x=>println(s"counted = $x"))
    transformerStream.kafkaStreams.foreach(_.cleanUp())

    try {
      transformerStream.start()
      Thread.sleep(2000)
      runFibGenerator(fibProducer)
      println("Now waiting1")
      println(transformerStream.kafkaStreams.isEmpty)
      transformerStream.kafkaStreams.foreach(readMaterializedStore(_))
      Thread.sleep(2000)
    } catch {
      case e: Throwable =>
        System.exit(1)
    }
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    val main = new Main()
    main.run()
  }
}

