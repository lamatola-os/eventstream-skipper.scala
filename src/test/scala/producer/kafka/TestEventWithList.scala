package producer.kafka

import java.util.Date

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import offset.EventOffsetAndHashValue
import producer.BaseEvent

/**
  * Created by prayagupd
  * on 1/23/17.
  */

case class SomeObject(field1: String, field2: String)

case class TestEventWithList(eventOffset: Long, hashValue: Long, eventType: String, createdDate: Date,
                             someObjects: List[SomeObject])  extends BaseEvent {

  def this(){
    this(0, 0, "", new Date(), List.empty)
  }

  override def toJSON(): String = null

  override def fromPayload(offset: EventOffsetAndHashValue, payload: String): BaseEvent = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    mapper.readValue(payload, classOf[TestEventWithList])
      .copy(eventOffset = offset.offset, hashValue = offset.checksum)
  }
}
