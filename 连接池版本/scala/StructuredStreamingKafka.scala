package com.spark.test

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.ProcessingTime

/**
 * Created by Administrator on 2017/10/16.
 */
object StructuredStreamingKafka {

  case class Weblog(datatime: String,
                    userid: String,
                    searchname: String,
                    retorder: String,
                    cliorder: String,
                    cliurl: String)

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .master("local[2]")
      .appName("streaming").getOrCreate()

    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "bigdata-pro01.kfk.com:9092")
      .option("subscribe", "weblogs")
      .load()

    import spark.implicits._
    val lines = df.selectExpr("CAST(value AS STRING)").as[String]
    val weblog = lines.map(_.split(","))
      .map(x => Weblog(x(0), x(1), x(2), x(3), x(4), x(5)))
    val titleCount = weblog
      .groupBy("searchname").count().toDF("titleName", "count")

    val url = "jdbc:mysql://bigdata-pro01.kfk.com:3306/test"
    val username = "root"
    val password = "123456"

    val writer = new JDBCSink(url, username, password)
    val query = titleCount.writeStream
      .foreach(writer)
      .outputMode("update")
      .trigger(ProcessingTime("5 seconds"))
      .start()
    query.awaitTermination()
  }
}
