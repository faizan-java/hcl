package com.careconnect.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** One-time, opt-in import for installations moving from the former SQLite backend to Oracle. */
@Component
@Order(2)
public class LegacySqliteImportRunner implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(LegacySqliteImportRunner.class);

  private final JdbcTemplate oracle;
  private final String configuredPath;
  private final boolean production;

  public LegacySqliteImportRunner(
      JdbcTemplate oracle,
      @Value("${careconnect.legacy-sqlite-path:}") String configuredPath,
      @Value("${careconnect.production:false}") boolean production) {
    this.oracle = oracle;
    this.configuredPath = configuredPath;
    this.production = production;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (configuredPath.isBlank()) return;
    if (production) {
      throw new IllegalStateException("SQLite imports are disabled in the production profile.");
    }

    Path source = Path.of(configuredPath).toAbsolutePath().normalize();
    if (!Files.isRegularFile(source)) {
      throw new IllegalStateException("SQLite import file does not exist: " + source);
    }

    String checksum = checksum(source);
    Integer imported =
        oracle.queryForObject(
            "SELECT COUNT(*) FROM sqlite_import_runs WHERE source_sha256 = ?",
            Integer.class,
            checksum);
    if (imported != null && imported > 0) {
      log.info("SQLite import skipped; this database file has already been imported.");
      return;
    }

    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(
          "SQLite JDBC driver is unavailable. Rebuild the backend before running an import.", e);
    }

    try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + source)) {
      importRows(sqlite, "users", this::importUser);
      importRows(sqlite, "appointments", this::importAppointment);
      importRows(sqlite, "medications", this::importMedication);
      importRows(sqlite, "health_records", this::importHealthRecord);
      importRows(sqlite, "invoices", this::importInvoice);
      importRows(sqlite, "vitals", this::importVital);
      importRows(sqlite, "patient_body_complaints", this::importComplaint);
      importRows(sqlite, "audit_log", this::importAuditEntry);
      oracle.update(
          "INSERT INTO sqlite_import_runs (source_sha256, source_path) VALUES (?, ?)",
          checksum,
          source.toString());
      log.info("Imported legacy CareConnect records from {} into Oracle.", source);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not import the legacy SQLite database.", e);
    }
  }

  private void importRows(Connection sqlite, String table, Consumer<ResultSet> importer)
      throws SQLException {
    try (PreparedStatement statement = sqlite.prepareStatement("SELECT * FROM " + table);
        ResultSet rows = statement.executeQuery()) {
      while (rows.next()) importer.accept(rows);
    } catch (ImportFailure e) {
      throw e.sqlException;
    }
  }

  private void importUser(ResultSet row) {
    execute(
        "MERGE INTO users target USING (SELECT ? id, ? name, ? email, ? role, ? password_hash, ? created_at FROM dual) source "
            + "ON (target.id = source.id) "
            + "WHEN MATCHED THEN UPDATE SET target.name=source.name, target.email=source.email, target.role=source.role, target.password_hash=source.password_hash, target.created_at=source.created_at "
            + "WHEN NOT MATCHED THEN INSERT (id,name,email,role,password_hash,created_at) VALUES (source.id,source.name,source.email,source.role,source.password_hash,source.created_at)",
        row,
        1,
        2,
        3,
        4,
        5,
        timestamp(row, 6));
  }

  private void importAppointment(ResultSet row) {
    execute(
        "MERGE INTO appointments target USING (SELECT ? id, ? patient_id, ? provider_id, ? specialty, ? starts_at, ? status, ? location, ? created_at FROM dual) source "
            + "ON (target.id = source.id) "
            + "WHEN MATCHED THEN UPDATE SET target.patient_id=source.patient_id, target.provider_id=source.provider_id, target.specialty=source.specialty, target.starts_at=source.starts_at, target.status=source.status, target.location=source.location, target.created_at=source.created_at "
            + "WHEN NOT MATCHED THEN INSERT (id,patient_id,provider_id,specialty,starts_at,status,location,created_at) VALUES (source.id,source.patient_id,source.provider_id,source.specialty,source.starts_at,source.status,source.location,source.created_at)",
        row,
        1,
        2,
        3,
        4,
        timestamp(row, 5),
        6,
        7,
        timestamp(row, 8));
  }

  private void importMedication(ResultSet row) {
    execute(
        "MERGE INTO medications target USING (SELECT ? id, ? patient_id, ? name, ? dosage, ? instructions, ? active FROM dual) source "
            + "ON (target.id = source.id) "
            + "WHEN MATCHED THEN UPDATE SET target.patient_id=source.patient_id, target.name=source.name, target.dosage=source.dosage, target.instructions=source.instructions, target.active=source.active "
            + "WHEN NOT MATCHED THEN INSERT (id,patient_id,name,dosage,instructions,active) VALUES (source.id,source.patient_id,source.name,source.dosage,source.instructions,source.active)",
        row,
        1,
        2,
        3,
        4,
        5,
        6);
  }

  private void importHealthRecord(ResultSet row) {
    execute(
        "MERGE INTO health_records target USING (SELECT ? id, ? patient_id, ? provider_id, ? title, ? summary, ? recorded_at FROM dual) source "
            + "ON (target.id = source.id) "
            + "WHEN MATCHED THEN UPDATE SET target.patient_id=source.patient_id, target.provider_id=source.provider_id, target.title=source.title, target.summary=source.summary, target.recorded_at=source.recorded_at "
            + "WHEN NOT MATCHED THEN INSERT (id,patient_id,provider_id,title,summary,recorded_at) VALUES (source.id,source.patient_id,source.provider_id,source.title,source.summary,source.recorded_at)",
        row,
        1,
        2,
        3,
        4,
        5,
        timestamp(row, 6));
  }

  private void importInvoice(ResultSet row) {
    execute(
        "MERGE INTO invoices target USING (SELECT ? id, ? patient_id, ? invoice_number, ? description, ? amount_cents, ? status, ? due_at, ? created_at FROM dual) source "
            + "ON (target.id = source.id) "
            + "WHEN MATCHED THEN UPDATE SET target.patient_id=source.patient_id, target.invoice_number=source.invoice_number, target.description=source.description, target.amount_cents=source.amount_cents, target.status=source.status, target.due_at=source.due_at, target.created_at=source.created_at "
            + "WHEN NOT MATCHED THEN INSERT (id,patient_id,invoice_number,description,amount_cents,status,due_at,created_at) VALUES (source.id,source.patient_id,source.invoice_number,source.description,source.amount_cents,source.status,source.due_at,source.created_at)",
        row,
        1,
        2,
        3,
        4,
        5,
        6,
        LocalDate.parse(value(row, 7)),
        timestamp(row, 8));
  }

  private void importVital(ResultSet row) {
    execute(
        "MERGE INTO vitals target USING (SELECT ? id, ? patient_id, ? metric, ? vital_value, ? unit, ? range_status, ? recorded_at FROM dual) source "
            + "ON (target.id = source.id) "
            + "WHEN MATCHED THEN UPDATE SET target.patient_id=source.patient_id, target.metric=source.metric, target.vital_value=source.vital_value, target.unit=source.unit, target.range_status=source.range_status, target.recorded_at=source.recorded_at "
            + "WHEN NOT MATCHED THEN INSERT (id,patient_id,metric,vital_value,unit,range_status,recorded_at) VALUES (source.id,source.patient_id,source.metric,source.vital_value,source.unit,source.range_status,source.recorded_at)",
        row,
        1,
        2,
        3,
        rowDouble(row, 4),
        5,
        6,
        timestamp(row, 7));
  }

  private void importComplaint(ResultSet row) {
    execute(
        "MERGE INTO patient_body_complaints target USING (SELECT ? id, ? patient_id, ? body_part, ? body_part_id, ? symptom, ? pain_level, ? duration, ? description, ? diagnosis, ? doctor_notes, ? treatment_recommendation, ? status, ? created_at, ? updated_at FROM dual) source "
            + "ON (target.id = source.id) "
            + "WHEN MATCHED THEN UPDATE SET target.patient_id=source.patient_id, target.body_part=source.body_part, target.body_part_id=source.body_part_id, target.symptom=source.symptom, target.pain_level=source.pain_level, target.duration=source.duration, target.description=source.description, target.diagnosis=source.diagnosis, target.doctor_notes=source.doctor_notes, target.treatment_recommendation=source.treatment_recommendation, target.status=source.status, target.created_at=source.created_at, target.updated_at=source.updated_at "
            + "WHEN NOT MATCHED THEN INSERT (id,patient_id,body_part,body_part_id,symptom,pain_level,duration,description,diagnosis,doctor_notes,treatment_recommendation,status,created_at,updated_at) VALUES (source.id,source.patient_id,source.body_part,source.body_part_id,source.symptom,source.pain_level,source.duration,source.description,source.diagnosis,source.doctor_notes,source.treatment_recommendation,source.status,source.created_at,source.updated_at)",
        row,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        blankAsSpace(value(row, 8)),
        blankAsSpace(value(row, 9)),
        blankAsSpace(value(row, 10)),
        blankAsSpace(value(row, 11)),
        12,
        timestamp(row, 13),
        timestamp(row, 14));
  }

  private void importAuditEntry(ResultSet row) {
    String actorId = value(row, 2);
    String action = value(row, 3);
    String entity = value(row, 4);
    String entityId = value(row, 5);
    OffsetDateTime createdAt = timestamp(row, 6);
    Integer exists =
        oracle.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE actor_id = ? AND action = ? AND entity = ? AND entity_id = ? AND created_at = ?",
            Integer.class,
            actorId,
            action,
            entity,
            entityId,
            createdAt);
    if (exists == null || exists == 0) {
      oracle.update(
          "INSERT INTO audit_log (actor_id, action, entity, entity_id, created_at) VALUES (?, ?, ?, ?, ?)",
          actorId,
          action,
          entity,
          entityId,
          createdAt);
    }
  }

  private void execute(String sql, ResultSet row, Object... columns) {
    Object[] values = new Object[columns.length];
    for (int i = 0; i < columns.length; i++) {
      Object column = columns[i];
      values[i] = column instanceof Integer index ? safeValue(row, index) : column;
    }
    oracle.update(sql, values);
  }

  private String safeValue(ResultSet row, int column) {
    try {
      return row.getString(column);
    } catch (SQLException e) {
      throw new ImportFailure(e);
    }
  }

  private String value(ResultSet row, int column) {
    return safeValue(row, column);
  }

  private OffsetDateTime timestamp(ResultSet row, int column) {
    try {
      String value = row.getString(column);
      try {
        return Instant.parse(value).atOffset(ZoneOffset.UTC);
      } catch (RuntimeException ignored) {
        return LocalDateTime.parse(value.replace(' ', 'T')).atOffset(ZoneOffset.UTC);
      }
    } catch (SQLException e) {
      throw new ImportFailure(e);
    }
  }

  private double rowDouble(ResultSet row, int column) {
    try {
      return row.getDouble(column);
    } catch (SQLException e) {
      throw new ImportFailure(e);
    }
  }

  private static String blankAsSpace(String value) {
    return value == null || value.isBlank() ? " " : value.trim();
  }

  private static String checksum(Path source) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Could not read the SQLite import file.", e);
    }
  }

  private static final class ImportFailure extends RuntimeException {
    private final SQLException sqlException;

    private ImportFailure(SQLException sqlException) {
      super(sqlException);
      this.sqlException = sqlException;
    }
  }
}
