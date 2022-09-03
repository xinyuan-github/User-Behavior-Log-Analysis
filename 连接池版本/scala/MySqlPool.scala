package com.spark.test

/**
 * Created by Administrator on 2017/9/30.
 */

import java.sql.{Connection, DriverManager}
import java.util

class MySqlPool(url: String, user: String, pwd: String) extends Serializable {
  private val max = 3 //连接池连接总数
  private val connectionNum = 1 //每次产生连接数
  private var conNum = 0 //当前连接池已产生的连接数
  private val pool = new util.LinkedList[Connection]() //连接池

  //获取连接
  def getJdbcConn(): Connection = {
    //同步代码块
    AnyRef.synchronized({
      if (pool.isEmpty) {
        //加载驱动
        preGetConn()
        for (i <- 1 to connectionNum) {
          val conn = DriverManager.getConnection(url, user, pwd)
          pool.push(conn)
          conNum += 1
        }
      }
      //将连接提供给外面使用
      pool.poll()
    })
  }

  //释放连接，把连接还给连接池
  def releaseConn(conn: Connection): Unit = {
    pool.push(conn)
  }

  //加载驱动
  private def preGetConn(): Unit = {
    //控制加载
    if (conNum < max && !pool.isEmpty) {
      //连接池有连接没有使用，等一会再看
      println("Jdbc Pool has no connection now, please wait a moments!")
      Thread.sleep(2000)
      preGetConn()
    } else {
      Class.forName("com.mysql.jdbc.Driver");
    }

  }

}
