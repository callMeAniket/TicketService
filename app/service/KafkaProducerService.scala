package service

import com.typesafe.config.{Config, ConfigFactory}
import jakarta.inject.{Inject, Singleton}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

import java.util.Properties

@Singleton
class KafkaProducerService @Inject()(){
  private val config = ConfigFactory.load()
  private val kafkaConfig = config.getConfig("kafka")
  // Kafka producer properties
  private val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getString("bootstrap.servers"))
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")

  // Initialize Kafka producer
  private val kafkaProducer = new KafkaProducer[String, String](props)
  private val topic = kafkaConfig.getString("topic")
  // Method to send a message to Kafka
  def sendMessage(message: String): Unit = {
    val record = new ProducerRecord[String, String](topic, message)
    kafkaProducer.send(record)
    println("success to kafka")
  }
  // Method to close the Kafka producer
  def close(): Unit = {
    kafkaProducer.close()
  }
}
