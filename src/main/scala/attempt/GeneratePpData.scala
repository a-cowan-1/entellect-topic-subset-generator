package attempt

import Util._

import scala.annotation.tailrec
import scala.io.Source


class GeneratePpData {
  def dataGenerator() = {
    val fileContents = Source.fromFile("sample_data/drugs_excerpt.json").getLines.mkString
    val records:Any = getJsonObj(fileContents)

  }

}

object GeneratePpData {
  def main(args: Array[String]):Unit ={
    val generatePpData = new GeneratePpData
    generatePpData.dataGenerator()
  }
}


