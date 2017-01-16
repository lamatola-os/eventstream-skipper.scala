package producer

import java.util
import java.util.{Date, Properties}

import kafka.admin.AdminUtils
import org.scalatest.FunSuite
import kafka.utils.ZkUtils
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.I0Itec.zkclient.{ZkClient, ZkConnection}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.collection.JavaConverters._

/**
  * Created by prayagupd
  * on 1/14/17.
  */

class KafkaEventPublisherIntegrationSpecs extends FunSuite {

  case class TestEvent(eventOffset: Long, hashValue: Long, created: Date) extends BaseEvent

  val kafkaPublisher = new KafkaEventPublisher

  test("produces a record to kafka store") {

    implicit val streamingConfig = EmbeddedKafkaConfig(kafkaPort = 9092, zooKeeperPort = 2181)

    EmbeddedKafka.start()

    val event = TestEvent(0l, 0l, new Date())

    val persistedEvent = kafkaPublisher.publish(event)
    assert(persistedEvent.eventOffset == 0)
    assert(persistedEvent.hashValue != 0)

    val config = new Properties() {
      {
        put("bootstrap.servers", "localhost:9092") //streaming.config
        put("group.id", "consumer_group_test")
        put("key.deserializer", classOf[StringDeserializer].getName)
        put("value.deserializer", classOf[StringDeserializer].getName)
      }
    }

    val kafkaConsumer = new KafkaConsumer[String, String](config)

    val topics = kafkaConsumer.listTopics().asScala

    assert(topics.map(_._1) == List("TestEvent"))

    kafkaConsumer.subscribe(util.Arrays.asList("TestEvent"))

    val topic = AdminUtils.topicExists(new ZkUtils(new ZkClient("localhost:2181", 10000, 15000),
      new ZkConnection("localhost:2181"), false), "TestEvent")

    assert(topic)

    var events: ConsumerRecords[String, String] = null

    events = kafkaConsumer.poll(1000)
    println(events.partitions().size())

    assert(events.count() == 1)

    EmbeddedKafka.stop()
  }

  test("produces multiple records to kafka store") {

    implicit val config = EmbeddedKafkaConfig(kafkaPort = 9092, zooKeeperPort = 2181)

    EmbeddedKafka.start()

    val event = TestEvent(0l, 0l, new Date())

    val persistedEvent = kafkaPublisher.publish(event)
    assert(persistedEvent.eventOffset == 0)
    assert((persistedEvent.hashValue + "").length > 0)

    val persistedEvent2 = kafkaPublisher.publish(TestEvent(0l, 0l, new Date()))
    assert(persistedEvent2.eventOffset == 1)
    assert((persistedEvent2.hashValue + "").length > 0)

    EmbeddedKafka.stop()
  }
}
