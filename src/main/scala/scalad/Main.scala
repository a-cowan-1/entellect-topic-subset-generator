package main.scala.scalad

object Main {

  def main(args: Array[String]): Unit = {
    val consumer1 =  Consumer[String, String]("tree", "76")
    consumer1.subscribe("test-1", (record) => println("1",record))
    val consumer2 =  Consumer[String, String]("tree", "876")
    consumer2.subscribe("test-1", (record) => println("2", record))
    Thread.sleep(3000)
    val producer = Producer[String, String]()
    for (i <- 1 to 25) {
      producer.produce(KafkaRecord("test-1", 0, 0, "test"+i, i.toString))
      Thread.sleep(1000)
    }
    Thread.sleep(6000)
    println("Pause over")

  }

}
