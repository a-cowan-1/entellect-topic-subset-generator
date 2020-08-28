package materialise
import org.apache.kafka.streams.scala.{ByteArrayKeyValueStore, Serdes, StreamsBuilder}
import org.apache.kafka.streams.scala.kstream.{KGroupedStream, KStream, KTable}
import main.scala.streamed.Stream
import org.apache.kafka.streams.scala.kstream._
import main.scala.scalad.{Consumer, KafkaRecord, Producer}
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsConfig}
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.state.{KeyValueIterator, QueryableStoreTypes, ReadOnlyKeyValueStore, Stores}
import java.util.Properties

import scala.annotation.tailrec
import scala.collection.JavaConverters._
class PocMaterialise {

  val storeName = "store"
  val storeSupplier = Stores.persistentKeyValueStore(storeName)
  val materialized: Materialized[String, String, ByteArrayKeyValueStore] = Materialized
    .as[String, String](storeSupplier)
    .withLoggingEnabled(Map.empty[String, String].asJava)

  def runFibGenerator(producer: Producer[String, String]) = {
    @tailrec def fib(x: BigInt, xm1: BigInt, countdown: Int, func: (BigInt, Int) => Unit): Unit = {
      func(x, countdown)
      countdown match {
        case 0 =>
        case _ => fib(x + xm1, x, countdown - 1, func)
      }
    }

    fib(1, 0, 100, (x: BigInt, count: Int) => {
      producer.produce(KafkaRecord("fib", 0, 0, count.toString, x.toString()))
      Thread.sleep(2)
    })
  }

  val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "poc-materialise")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "http://oxygen-dev-01.nonprod.entellect.com/")
    p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass)
    p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass)
    p
  }

  def topologyPlan(): StreamsBuilder = {
    import org.apache.kafka.streams.scala.Serdes._
    val builder= new StreamsBuilder
    val fibMat: KTable[String, String] = builder.table("pp1", materialized)

    builder
  }




  def readMaterializedStore(kafkaStreams: KafkaStreams): Unit = {
    val keyValueStore: ReadOnlyKeyValueStore[String, String] = kafkaStreams.store(storeName, QueryableStoreTypes.keyValueStore[String, String]())
    val timezero = System.currentTimeMillis()
    println(System.currentTimeMillis()-timezero)
    println(s"key: 10, value: ${keyValueStore.get("10")}")
    println("time taken(millis):" + {System.currentTimeMillis()-timezero})
    println(s"key: 20, value: ${keyValueStore.get("20")}")
    println("time taken(millis):" + {System.currentTimeMillis()-timezero})
    println(s"key: 30, value: ${keyValueStore.get("30")}")
    println("time taken(millis):" + {System.currentTimeMillis()-timezero})
    println(s"key: 41, value: ${keyValueStore.get("41")}")
    println("time taken(millis):" + {System.currentTimeMillis()-timezero})
    println(s"key: 42, value: ${keyValueStore.get("42")}")
    println("time taken(millis):" + {System.currentTimeMillis()-timezero})
    println(s"key: 47, value: ${keyValueStore.get("47")}")
    println("time taken(millis):" + {System.currentTimeMillis()-timezero})
    println(s"key: 48, value: ${keyValueStore.get("48")}")
  }



  def run() = {
    val rawConsumer = Consumer[String, String]("grp1", "776")

    val transformerStream = Stream(topologyPlan())

    rawConsumer.subscribe("pp1", x=>println(s"raw= $x"))
    transformerStream.kafkaStreams.foreach(_.cleanUp())
    try {
      transformerStream.start()
      Thread.sleep(2000)
//      runFibGenerator(fibProducer)
      println("Now waiting1")
      transformerStream.kafkaStreams.foreach(readMaterializedStore(_))



      Thread.sleep(2000)

    }
    catch{
      case e: Throwable =>
        println(e)
      System.exit(1)}

  }

}

object PocMaterialise {
  def main(args: Array[String]): Unit = {
    val main = new PocMaterialise()
    main.run()
  }
}