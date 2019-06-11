package com.simplaex

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.alpakka.udp.Datagram
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.TestKit
import akka.util.ByteString
import com.simplaex.io.CsvHelper.CSVWrapper
import com.simplaex.model.Message
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class MessageConsumerTest extends TestKit(ActorSystem("ListsDiscoveryTest")) with FlatSpecLike with Matchers {


	implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system)
			.withSupervisionStrategy(Supervision.getStoppingDecider))


	val listMessages = List[String](
		"0977dca4-9906-3171-bcec-87ec0df9d745,kFFzW4O8gXURgP8ShsZ0gcnNT5E=,0.18715484122922377,982761284,8442009284719321817",
		"5fac6dc8-ea26-3762-8575-f279fe5e5f51,cBKFTwsXHjwypiPkaq3xTr8UoRE=,0.7626710614484215,1005421520,6642446482729493998",
		"0977dca4-9906-3171-bcec-87ec0df9d745,9ZWcYIblJ7ebN5gATdzzi4e8K7Q=,0.9655429720343038,237475359,3923415930816731861",
		"4d968baa-fe56-3ba0-b142-be9f457c9ff4,RnJNTKLYpcUqhjOey+wEIGHC7aw=,0.6532229483547558,1403876285,4756900066502959030",
		"0977dca4-9906-3171-bcec-87ec0df9d745,N0fiZEPBjr3bEHn+AHnpy7I1RWo=,0.8857966322563835,1851028776,6448117095479201352",
		"0977dca4-9906-3171-bcec-87ec0df9d745,P/wNtfFfa8jIn0OyeiS1tFvpORc=,0.8851653165728414,1163597258,8294506528003481004",
		"0977dca4-9906-3171-bcec-87ec0df9d745,Aizem/PgVMKsulLGquCAsLj674U=,0.5869654624020274,1012454779,2450005343631151248",
		"023316ec-c4a6-3e88-a2f3-1ad398172ada,TRQb8nSQEZOA5Ccx8NntYuqDPOw=,0.3790267017026414,652953292,4677453911100967584",
		"023316ec-c4a6-3e88-a2f3-1ad398172ada,UfL8VetarqYZparwV4AJtyXGgFM=,0.26029423666931595,1579431460,5620969177909661735",
		"0977dca4-9906-3171-bcec-87ec0df9d745,uZNIcWQtwst+9mjQgPkV2rvm7QY=,0.039107542861771316,280709214,4450245425875000740"
	)

	"Incomming elements" should "transform" in {
		val future =
			Source(listMessages)
				.map(i => ByteString(i))
				.map(Datagram(_, new InetSocketAddress("localhost", 9001)))
    		.via(MessageConsumer.transformMessage())
    		.runWith(Sink.seq)

		val result = Await.result(future, 3.seconds)
		assert(result.size == 10)
		assert(result.head.isInstanceOf[Message])
	}

	"Incomming elements" should "sum of data point 5 overall " in {
		val future =
			Source(listMessages)
					.map(i => ByteString(i))
					.map(Datagram(_, new InetSocketAddress("localhost", 9001)))
					.via(MessageConsumer.transformMessage())
					.grouped(5)
					.map(_.toList)
    			.via(MessageConsumer.sumDataPoint())
					.runWith(Sink.seq)

		val result = Await.result(future, 5.seconds)
		assert(result.head == BigInt("30212888860247708058"))
		assert(result.last == BigInt("25493180386520262311"))
	}

	"Incomming elements" should "return number of unique users" in {
		val future =
			Source(listMessages)
					.map(i => ByteString(i))
					.map(Datagram(_, new InetSocketAddress("localhost", 9001)))
					.via(MessageConsumer.transformMessage())
					.grouped(5)
					.map(_.toList)
					.via(MessageConsumer.uniqueUsers())
					.runWith(Sink.seq)

		val result = Await.result(future, 5.seconds)
		assert(result.head == 3)
		assert(result.last == 2)
	}

	"Incomming elements" should "return UUID, average of float and recent data point 4" in {
		val future =
			Source(listMessages)
					.map(i => ByteString(i))
					.map(Datagram(_, new InetSocketAddress("localhost", 9001)))
					.via(MessageConsumer.transformMessage())
					.grouped(5)
					.map(_.toList)
					.via(MessageConsumer.perUserInfo())
					.runWith(Sink.seq)

		val result = Await.result(future, 5.seconds)
		assert(result.head.size == 3)
		assert(result.last.size == 2)
		assert(result.head.map(x => x.toCSV).contains("0977dca4-9906-3171-bcec-87ec0df9d745,0.6794981485066369,1851028776"))
		assert(result.head.map(x => x.toCSV).contains("5fac6dc8-ea26-3762-8575-f279fe5e5f51,0.7626710614484215,1005421520"))
		assert(result.head.map(x => x.toCSV).contains("4d968baa-fe56-3ba0-b142-be9f457c9ff4,0.6532229483547558,1403876285"))
	}

}
