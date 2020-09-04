package materialise
import org.apache.kafka.streams.scala.{ByteArrayKeyValueStore, Serdes, StreamsBuilder}
import org.apache.kafka.streams.scala.kstream.{KGroupedStream, KStream, KTable}
import main.scala.streamed.Stream
import org.apache.kafka.streams.scala.kstream._
import main.scala.scalad.Consumer
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsConfig}
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.state.{KeyValueIterator, QueryableStoreTypes, ReadOnlyKeyValueStore, Stores}
import java.util.Properties

import attempt.{KafkaRecord, Producer}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
class PocMaterialise {


  val topic = sys.env("KAFKA_TOPIC")

  val storeName = "store"
  val storeSupplier = Stores.persistentKeyValueStore(storeName)
  val materialized: Materialized[String, String, ByteArrayKeyValueStore] = Materialized
    .as[String, String](storeSupplier)
    .withLoggingEnabled(Map.empty[String, String].asJava)



  val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "poc-materialise")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, sys.env("KAFKA_BROKER_URL"))
    p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass)
    p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass)
    p
  }

  def topologyPlan(): StreamsBuilder = {
    import org.apache.kafka.streams.scala.Serdes._
    val builder= new StreamsBuilder
    val fibMat: KTable[String, String] = builder.table(topic, materialized)

    builder
  }




  def readMaterializedStore(kafkaStreams: KafkaStreams): Unit = {
    Thread.sleep(10000)
    val keyValueStore: ReadOnlyKeyValueStore[String, String] = kafkaStreams.store(storeName, QueryableStoreTypes.keyValueStore[String, String]())
    val timezero = System.currentTimeMillis()
    println(System.currentTimeMillis()-timezero)
    println(s"key: https://data.elsevier.com/lifescience/entity/ppplus/drug/EqOs3QZu5TG, value: ${keyValueStore.get("https://data.elsevier.com/lifescience/entity/ppplus/drug/EqOs3QZu5TG")}")
    println("time taken(millis):" + {System.currentTimeMillis()-timezero})
    println(s"key: https://data.elsevier.com/lifescience/entity/ppplus/drug/ZNORjxbSZU4, value: ${keyValueStore.get("https://data.elsevier.com/lifescience/entity/ppplus/drug/ZNORjxbSZU4")}")
    println("time taken(millis):" + {System.currentTimeMillis()-timezero})

  }



  def run() = {
    val rawConsumer = Consumer[String, String]("grp1", "776")

    val transformerStream = Stream(topologyPlan())

    rawConsumer.subscribe(topic, x=>println(s"raw= $x"))
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