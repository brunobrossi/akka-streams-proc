package com.simplaex

import java.net.InetSocketAddress
import java.nio.file.Paths
import java.util.UUID
import java.nio.file.Files

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.alpakka.udp.Datagram
import akka.stream.alpakka.udp.scaladsl.Udp
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape}
import akka.util.ByteString
import com.simplaex.io.CsvHelper.CSVWrapper
import com.simplaex.model.{Message, MessageAggregate, Result}
import com.typesafe.config.ConfigFactory
import org.reactivestreams.Subscriber


object MessageConsumer extends App {

  implicit val system: ActorSystem = ActorSystem("message-consumer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val logAdapter: LoggingAdapter = Logging(system, "logger")

  lazy val conf = ConfigFactory.load()
  val pathConf = conf.getString("file.path")

  val socket = new InetSocketAddress(conf.getString("udp.address"), conf.getInt("udp.port"))
  val udpBindFlow = Udp.bindFlow(socket)

  /*
   Source, entry point of messages from UDP as Datagram, will go though a flow to create a case class
    and group by chunks of messages
   */
  def source(): Source[List[Message], Subscriber[Datagram]] = {
    Source.asSubscriber[Datagram]
        .via(udpBindFlow)
        .via(transformMessage())
        .grouped(1000)
        .map(_.toList)
        .log("batch_release", x => s"Batch released to analysis, size: ${x.size}")
        .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
  }

  /*
   Flow to transform the data coming from UDP port to a case class
   */
  def transformMessage(): Flow[Datagram, Message, NotUsed] = {
    Flow[Datagram]
        .map(message => message.data.utf8String.trim.split(","))
        .map(strArray => Message(
          UUID.fromString(strArray(0)),
          strArray(1),
          strArray(2).toDouble,
          strArray(3).toInt,
          strArray(4).toLong))
  }

  /*
   Flow to get sum of data point
   */
  def sumDataPoint(): Flow[List[Message], BigInt, NotUsed] = {
    Flow[List[Message]]
        .map(_.map(_.intValue2).sum)
  }

  /*
    Flow to get unique users
   */
  def uniqueUsers(): Flow[List[Message], Int, NotUsed] = {
    Flow[List[Message]]
        .map(_.map(_.userAccount).distinct.size)
        .log("unique_users", x => s"Unique Users: $x")
        .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
  }

  /*
    Flow to get the aggregated information about the users
   */
  def perUserInfo(): Flow[List[Message], List[MessageAggregate], NotUsed] = {
    Flow[List[Message]]
        .map {
          batch => {
            batch
                .groupBy(_.userAccount)
                .foldLeft(List[MessageAggregate]())(
                  (list, user) => MessageAggregate(user._1, user._2.map(_.floatPoint).sum / user._2.size, user._2.last.intValue1) :: list
                )
          }
        }
        .log("user_info", x => s"User aggregation (1 user) : ${x.head}")
        .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
  }

  /*
    Flow to write the CSV file. It will write on the determined path, if the path does not exist,
     its going to be created
   */
  def sinkToFile() = {
    val path = Paths.get(pathConf)
    if (!Files.exists(path)){
      Files.createDirectory(path)
    }
    Flow[Result]
        .map {
          result =>
            val fileName = Paths.get(pathConf + System.currentTimeMillis() + ".txt")
            val line1 = result.sumDataPoint5.toString()
            val line2 = result.uniqueUsers.toString
            val line3 = result.aggregates.map(x => x.toCSV).mkString("\n")

            Source(List(line1, line2, line3))
                .map {
                  line =>
                    val text = line + "\n"
                    ByteString(text)
                }
                .runWith(FileIO.toPath(fileName))
            fileName
        }
        .log("file_created", x => s"File created: $x")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
  }


  RunnableGraph.fromGraph(GraphDSL.create() {
    logAdapter.info("Server Started on Port 9000")
    implicit builder =>
      import GraphDSL.Implicits._
      val zip = builder.add(ZipWith((sum: BigInt, unique: Int, agg: List[MessageAggregate]) => Result(sum, unique, agg)))
      val bcast = builder.add(Broadcast[List[Message]](3))
      // @formatter:off
      Source.fromGraph(source()) ~> bcast ~> sumDataPoint ~> zip.in0
                                    bcast ~> uniqueUsers  ~> zip.in1
                                    bcast ~> perUserInfo  ~> zip.in2
                                                             zip.out ~> sinkToFile ~> Sink.ignore
      ClosedShape
    // @formatter:on

  }).run()

}
