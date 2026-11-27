package helloworkflow.drone;

import helloworkflow.drone.proto.DronePosition;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DroneController {
  private static final Logger logger = LoggerFactory.getLogger(DroneController.class);
  private static final long PUBLISH_TIMEOUT_SECONDS = 10;

  private final KafkaTemplate<String, byte[]> kafkaTemplate;
  private final String topic;

  public DroneController(
    KafkaTemplate<String, byte[]> kafkaTemplate,
    @Value("${drone.kafka.topic}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  @PostMapping(path = "/sendProtoBuf/", consumes = "application/x-protobuf")
  public ResponseEntity<String> sendProtoBuf(@RequestBody byte[] payload) {
    final DronePosition position;
    try {
      position = DronePosition.parseFrom(payload);
    } catch (com.google.protobuf.InvalidProtocolBufferException exception) {
        return ResponseEntity.badRequest().body("Invalid protobuf payload");
    }

    try {
      kafkaTemplate
          .send(topic, Long.toString(position.getId()), payload)
          .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      return ResponseEntity.accepted()
          .contentType(MediaType.TEXT_PLAIN)
          .body("Drone position accepted");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      logger.warn("Interrupted while publishing drone position to Kafka", exception);
      return ResponseEntity.internalServerError().body("Unable to publish drone position");
    } catch (ExecutionException | TimeoutException exception) {
      logger.warn("Failed to publish drone position to Kafka", exception);
      return ResponseEntity.internalServerError().body("Unable to publish drone position");
    }
  }
}
