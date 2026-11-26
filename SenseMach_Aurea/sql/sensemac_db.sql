CREATE DATABASE IF NOT EXISTS sensemac_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sensemac_db;

CREATE TABLE IF NOT EXISTS empresas (
    codigo_empresa VARCHAR(25) PRIMARY KEY,
    nombre_empresa VARCHAR(120) NOT NULL UNIQUE,
    rfc_empresa VARCHAR(30) NOT NULL UNIQUE,
    sector_industrial VARCHAR(80) NOT NULL,
    fecha_registro VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    rol VARCHAR(60) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Activa',
    codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026',
    FOREIGN KEY (codigo_empresa) REFERENCES empresas(codigo_empresa) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS maquinaria (
    codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026',
    codigo_maq VARCHAR(20) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    modelo VARCHAR(60) NOT NULL,
    anio INT NOT NULL,
    horas_uso INT NOT NULL DEFAULT 0,
    fallas_previas VARCHAR(255) DEFAULT 'Ninguna',
    costo_adquisicion DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    costo_mantenimiento DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    estado_operativo VARCHAR(30) NOT NULL DEFAULT 'Óptimo',
    ultimo_registro VARCHAR(40) NOT NULL,
    PRIMARY KEY (codigo_empresa, codigo_maq),
    FOREIGN KEY (codigo_empresa) REFERENCES empresas(codigo_empresa) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS monitoreo_bitacora (
    id_lectura INT AUTO_INCREMENT PRIMARY KEY,
    codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026',
    codigo_maq VARCHAR(20) NOT NULL,
    temperatura DECIMAL(6,2) NOT NULL,
    presion DECIMAL(6,2) NOT NULL,
    vibracion DECIMAL(6,2) NOT NULL,
    observaciones VARCHAR(255) NOT NULL,
    responsable VARCHAR(100) NOT NULL,
    fecha_hora VARCHAR(40) NOT NULL,
    estado_detectado VARCHAR(30) NOT NULL,
    FOREIGN KEY (codigo_empresa, codigo_maq) REFERENCES maquinaria(codigo_empresa, codigo_maq) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mantenimientos (
    id_mant INT AUTO_INCREMENT PRIMARY KEY,
    codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026',
    codigo_maq VARCHAR(20) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    actividades VARCHAR(255) NOT NULL,
    refacciones VARCHAR(150) NOT NULL,
    fecha_programada VARCHAR(30) NOT NULL,
    tecnico_responsable VARCHAR(100) NOT NULL,
    alerta_proxima VARCHAR(40) NOT NULL DEFAULT 'Programado',
    FOREIGN KEY (codigo_empresa, codigo_maq) REFERENCES maquinaria(codigo_empresa, codigo_maq) ON DELETE CASCADE
);

INSERT IGNORE INTO empresas (codigo_empresa, nombre_empresa, rfc_empresa, sector_industrial, fecha_registro) VALUES
('AUREA-2026', 'Manufactura Industrial del Centro', 'MIC260828IP9', 'Metalurgia y Maquinado CNC', '28 Ago 2026'),
('BEST-RACK26', 'Best Ingeniería y Logística', 'BIL210415RK8', 'Fabricación de Racks y Sistemas de Almacenamiento', '05 Sep 2026'),
('NVIDIA-2026', 'NVIDIA Corporation', 'NVD930405GP1', 'Semiconductores y Ensamble de Servidores IA', '12 Sep 2026'),
('BIMBO-PL26', 'Grupo Bimbo', 'GBI451202PA4', 'Industria Alimentaria y Panificación Automatizada', '18 Sep 2026');

