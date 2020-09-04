package attempt

import java.text.SimpleDateFormat

case class KafkaRecord[K, V](topic: String, partition: Int, offset: Long, key: K, value: V, recordCounter: Int = -1, timestamp: Long = -1) {
  //  val timestamp = Calendar.getInstance.getTime
  val timeFormat = new SimpleDateFormat("HH:mm:ss")

  override def toString: String = s"(${timeFormat.format(timestamp)})->(topic:$topic), (partition:$partition), (offset:$offset), (key: $key), (value: $value), (counter: $recordCounter)"
}
