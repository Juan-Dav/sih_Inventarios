-- Carga inicial de roles (solo se insertan si no existen)
INSERT IGNORE INTO roles (nombre) VALUES ('ADMINISTRADOR');
INSERT IGNORE INTO roles (nombre) VALUES ('TECNICO');
INSERT IGNORE INTO roles (nombre) VALUES ('DOCENTE');
