package com.experienzia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Esquemas antiguos de PostgreSQL pueden tener {@code eventos_estado_check} sin el valor
 * {@code FINALIZADO}, lo que provoca fallos al persistir eventos ya terminados.
 */
@Component
@Order(0)
public class PostgresEventoEstadoCheckFix implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgresEventoEstadoCheckFix.class);

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;

    public PostgresEventoEstadoCheckFix(Environment environment, JdbcTemplate jdbcTemplate) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.parseBoolean(environment.getProperty("experienzia.db.fix-eventos-estado-check", "true"))) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.contains("jdbc:postgresql")) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE eventos DROP CONSTRAINT IF EXISTS eventos_estado_check");
            jdbcTemplate.execute(
                    "ALTER TABLE eventos ADD CONSTRAINT eventos_estado_check CHECK (estado IN ("
                            + "'PENDIENTE','APROBADO','RECHAZADO','ACTIVO','FINALIZADO','CANCELADO',"
                            + "'PENDIENTE_REVISION','PENDIENTE_SUPLEMENTO','PENDIENTE_CANCELACION'))");
            log.info("Constraint eventos_estado_check alineado con EstadoEvento (incluye flujos de edición/cancelación).");
        } catch (Exception e) {
            log.warn("No se pudo actualizar eventos_estado_check: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE eventos ALTER COLUMN estado TYPE VARCHAR(40)");
        } catch (Exception e) {
            log.debug("ALTER COLUMN estado (ancho): {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE pagos ALTER COLUMN comprobante_url DROP NOT NULL");
            log.info("Columna pagos.comprobante_url permite NULL (comprobante pendiente de subir).");
        } catch (Exception e) {
            log.warn("No se pudo alinear pagos.comprobante_url: {}", e.getMessage());
        }
        alinearForeignKeysEventoNovedades();
    }

    /**
     * La tabla usada por la app es {@code evento_novedades} (no {@code eventos_novedades}).
     * Sin {@code @ManyToOne} Hibernate no creaba FK y el ERD mostraba la tabla aislada.
     */
    private void alinearForeignKeysEventoNovedades() {
        try {
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = 'public' AND table_name = 'evento_novedades'",
                    Integer.class);
            if (existe == null || existe == 0) {
                log.debug("Tabla evento_novedades aún no existe; Hibernate la creará con ddl-auto=update.");
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE evento_novedades DROP CONSTRAINT IF EXISTS fk_evento_novedad_evento");
            jdbcTemplate.execute(
                    "ALTER TABLE evento_novedades ADD CONSTRAINT fk_evento_novedad_evento "
                            + "FOREIGN KEY (evento_id) REFERENCES eventos(id) ON DELETE CASCADE");
            jdbcTemplate.execute(
                    "ALTER TABLE evento_novedades DROP CONSTRAINT IF EXISTS fk_evento_novedad_usuario");
            jdbcTemplate.execute(
                    "ALTER TABLE evento_novedades ADD CONSTRAINT fk_evento_novedad_usuario "
                            + "FOREIGN KEY (usuario_solicitante_id) REFERENCES usuarios(id)");
            log.info("FK evento_novedades → eventos y usuarios alineadas.");
        } catch (Exception e) {
            log.warn("No se pudieron crear FK de evento_novedades: {}", e.getMessage());
        }
    }
}
