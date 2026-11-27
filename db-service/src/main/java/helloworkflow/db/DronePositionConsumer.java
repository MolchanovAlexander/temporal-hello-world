package helloworkflow.db;

import helloworkflow.drone.proto.DronePosition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DronePositionConsumer {
  private static final String UPSERT_SQL =
      """
      INSERT INTO drone_position (id, pos1, pos2)
      VALUES (?, ?, ?)
      ON CONFLICT (id) DO UPDATE
      SET pos1 = EXCLUDED.pos1, pos2 = EXCLUDED.pos2
      """;

  private final JdbcTemplate jdbcTemplate;
  private final DroneWebSocketHandler webSocketHandler;

  public DronePositionConsumer(
      JdbcTemplate jdbcTemplate, DroneWebSocketHandler webSocketHandler) {
    this.jdbcTemplate = jdbcTemplate;
    this.webSocketHandler = webSocketHandler;
  }

  @KafkaListener(topics = "${drone.kafka.topic}")
  public void consume(byte[] payload) throws com.google.protobuf.InvalidProtocolBufferException {
    DronePosition position = DronePosition.parseFrom(payload);
    jdbcTemplate.update(
        UPSERT_SQL, position.getId(), position.getPos1(), position.getPos2());
    webSocketHandler.notifySaved(position.getId());
  }
}
