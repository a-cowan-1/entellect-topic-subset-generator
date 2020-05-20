import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.admin._
import org.apache.kafka.common.config.TopicConfig.{CLEANUP_POLICY_COMPACT, CLEANUP_POLICY_CONFIG}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.ImplicitConversions._
import scala.collection.JavaConverters._
import org.apache.kafka.streams.scala.kstream.KTable
import scala.util.Try
object Main extends App {
  import Serdes._
  val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-first-fun")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass)
    p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.Long.getClass)
    p
  }
  val admin = AdminClient.create(props)
  val topicConfig = Map(CLEANUP_POLICY_CONFIG -> CLEANUP_POLICY_COMPACT).asJava
  val numPartitions = 1
  val numReplicas: Short = 1
  val inputTopic = "new-topic-input"
  val outputTopic = "new-topic-output"
  def createTopics(topic: String) = {
    admin.createTopics(
      List(new NewTopic(topic, numPartitions, numReplicas)).asJavaCollection
    ).all().get()
  }
  createTopics(inputTopic)
  createTopics(outputTopic)
  val builder = new StreamsBuilder
  val funStream = builder.stream[String, String](inputTopic)
  val ldp: KTable[String, Long] = funStream.flatMapValues(juv => juv.split("\\W+")).groupBy((a, b) => b).count()
  ldp.toStream.to(outputTopic)
  val kafkaStreams: KafkaStreams = new KafkaStreams(builder.build(), props)
  kafkaStreams.cleanUp()
  kafkaStreams.start()
  sys.ShutdownHookThread {
    kafkaStreams.close(Duration.ofSeconds(10))
  }
}