-- Relaciones de evento_novedades (tabla que usa la aplicación Spring/JPA).
-- En pgAdmin puede existir otra tabla manual "eventos_novedades" sin uso; no es la misma.

ALTER TABLE evento_novedades DROP CONSTRAINT IF EXISTS fk_evento_novedad_evento;
ALTER TABLE evento_novedades
    ADD CONSTRAINT fk_evento_novedad_evento
    FOREIGN KEY (evento_id) REFERENCES eventos (id) ON DELETE CASCADE;

ALTER TABLE evento_novedades DROP CONSTRAINT IF EXISTS fk_evento_novedad_usuario;
ALTER TABLE evento_novedades
    ADD CONSTRAINT fk_evento_novedad_usuario
    FOREIGN KEY (usuario_solicitante_id) REFERENCES usuarios (id);
