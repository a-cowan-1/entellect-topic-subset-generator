package materialise

import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Properties
import java.util
import java.util.{Calendar, Properties}

import main.scala.scalad.KafkaRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PocConsumer{
  case class PpMigrationConfig(
                                bootstrapServers: String,
                                groupId: String = "localhost:9092",
                                offsetReset: String = "earliest",
                                partitions: Int = 10,
                                replication: Short = 1,
                                batchWeight: Int = 10e7.toInt,
                                //                                batchWeight: Int = 100000000,
                                batchWindowSec: Int = 60,
                                sessionTimeoutSec: Int = 60,
                                queryTimeoutSec: Int = 10,
                                parallelism: Int = 12)

  case class KafkaRecord(topic: String, partition: Int, offset: Long, key: String, value: String, recordCounter: Int = -1, timestamp: Long = -1) {
    //  val timestamp = Calendar.getInstance.getTime
    val timeFormat = new SimpleDateFormat("HH:mm:ss")
    override def toString: String = s"(${timeFormat.format(timestamp)})->(topic:$topic), (partition:$partition), (offset:$offset), (key: $key), (value: $value), (counter: $recordCounter)"
  }

  var kafkaConsumer: Option[KafkaConsumer[String,String]] = None

  val ser = "org.apache.kafka.common.serialization.StringDeserializer"

  var running = true


  def initialise(groupName: String, clientName: String) = {
    val consumerProps = Map(
      "bootstrap.servers"  -> "localhost:9092",
      "key.deserializer"   -> ser,
      "value.deserializer" -> ser,
      "auto.offset.reset"  -> "latest",
      "auto.commit.interval.ms" -> "100",
      "group.id"           -> groupName,
      "client.id"          -> clientName
    )

    val props = new Properties()

    props.putAll(consumerProps.mapValues(_.toString).asJava)
    kafkaConsumer = Some( new KafkaConsumer(props))
  }

  var subscribedTopic:Option[String] = None
  var consumedCount: Int = 0
  def incrementCount() = {consumedCount +=1; consumedCount}

  def setShutdownHook() = {
    val mainThread = Thread.currentThread
    // Registering a shutdown hook so we can exit cleanly// Registering a shutdown hook so we can exit cleanly

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        //        System.out.println("Starting exit...")
        shutdown()
        //        // Note that shutdownhook runs in a separate thread, so the only thing we can safely do to a consumer is wake it up
        //        kafkaConsumer.foreach {_.wakeup}
        //        try
        //          mainThread.join
        //        catch {
        //          case e: InterruptedException =>
        //            println("interrupted")
        //        }
      }
    })
  }



  def subscribe(
                 topic: String,
                 func: KafkaRecord=>Unit,
                 shutdownWhenFunc: KafkaRecord=>Boolean = {_=>false}
               ): Option[Future[Unit]] = {
    kafkaConsumer match {
      case None =>  None
      case Some(consumer) => {
        setShutdownHook()
        subscribedTopic = Some(topic)
        consumer.subscribe(util.Arrays.asList(topic))
        println(s"Subscribing to $topic")
        val fut = Future {
          try {
            println("Listening")
            while (running) {
              val record = consumer.poll(Duration.ofSeconds(5)).asScala
              var scalaFormRecords = Array[KafkaRecord]()
              for (data <- record.iterator) {
                scalaFormRecords :+= KafkaRecord(data.topic(), data.partition(), data.offset(), data.key(), data.value(), incrementCount(), data.timestamp())
              }
              scalaFormRecords.foreach(func(_))
              if (scalaFormRecords.foldLeft(false){ (a,b) => a || shutdownWhenFunc(b)})
                shutdown()
            }
          } catch {
            case e: Exception => println(e)
            case e: WakeupException => println(s" topic $topic listener woke up")
          } finally {
            println(s"topic '$topic' consumer closing down")
            kafkaConsumer.foreach(_.close)
            println(s"topic '$topic' consumer closed down")
          }
        }
        Some(fut)
      }
    }

  }
  def shutdown() = {
    println(s"Shutdown signal for '${subscribedTopic.getOrElse("Empty")}' consumer")
    running = false
  }
}

object PocConsumer {
  def apply(groupName: String, clientName: String): PocConsumer = {
    val consumer = new PocConsumer()
    consumer.initialise(groupName, clientName)
    consumer

  }
}