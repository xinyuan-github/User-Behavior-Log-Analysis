package com.spark.test

import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.sql.DriverManager

object TestStreaming {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .master("local[2]")
      .appName("Streaming")
      .getOrCreate()

    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(5))

    val lines = ssc.socketTextStream("bigdata-pro02.kfk.com", 9999)
    val words = lines.flatMap(_.split(" ")).map(word => (word, 1)).reduceByKey(_ + _)

    words.foreachRDD(rdd => rdd.foreachPartition(line => {
      Class.forName("com.mysql.jdbc.Driver")
      val conn = DriverManager
        .getConnection("jdbc:mysql://bigdata-pro03.kfk.com:3306/test", "root", "111")

      try {
        for(row <- line) {
          val sql = "insert into webCount(titleName, Count) values('"+row._1+"', "+row._2+")"
          conn.prepareStatement(sql).executeUpdate()
        }
      } finally {
        conn.close()
      }
    }))


    //words.print()
    ssc.start()
    ssc.awaitTermination()
  }
}
