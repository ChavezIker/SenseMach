package com.aurea.sensemac.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

// clase que administra la conexion a mysql y el esquema multiempresa con datos de demostracion
public class ConexionDB {

    private static final String HOST = obtenerEnv("DB_HOST", "localhost");
    private static final String PUERTO = obtenerEnv("DB_PORT", "3306");
    private static final String NOMBRE_BD = obtenerEnv("DB_NAME", "sensemac_db");
    private static final String USUARIO = obtenerEnv("DB_USER", "root");
    private static final String PASSWORD = obtenerEnv("DB_PASSWORD", "angel735");
    private static final String USAR_SSL = obtenerEnv("DB_SSL", "localhost".equalsIgnoreCase(HOST) ? "false" : "true");

    private static final String PARAMS_JDBC = "?useSSL=" + USAR_SSL + "&requireSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectTimeout=15000";
    private static final String URL_SERVIDOR = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + PARAMS_JDBC;
    private static final String URL_BD = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + NOMBRE_BD + PARAMS_JDBC;
    private static volatile boolean inicializado = false;

    private static String obtenerEnv(String clave, String valorPorDefecto) {
        String val = System.getenv(clave);
        if (val == null || val.trim().isEmpty()) {
            val = System.getProperty(clave);
        }
        return (val != null && !val.trim().isEmpty()) ? val.trim() : valorPorDefecto;
    }

    public static Connection obtenerConexion() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        if (!inicializado) {
            synchronized (ConexionDB.class) {
                if (!inicializado) {
                    inicializado = inicializarBaseDeDatos();
                }
            }
        }
        return DriverManager.getConnection(URL_BD, USUARIO, PASSWORD);
    }

    private static boolean existeColumna(Connection con, String tabla, String columna) {
        try (ResultSet rs = con.getMetaData().getColumns(NOMBRE_BD, null, tabla, columna)) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean inicializarBaseDeDatos() {
        try (Connection conServer = DriverManager.getConnection(URL_SERVIDOR, USUARIO, PASSWORD);
             Statement stmt = conServer.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + NOMBRE_BD + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (Connection con = DriverManager.getConnection(URL_BD, USUARIO, PASSWORD);
             Statement st = con.createStatement()) {

            st.executeUpdate("CREATE TABLE IF NOT EXISTS empresas (" +
                    "codigo_empresa VARCHAR(25) PRIMARY KEY," +
                    "nombre_empresa VARCHAR(120) NOT NULL UNIQUE," +
                    "rfc_empresa VARCHAR(30) NOT NULL UNIQUE," +
                    "sector_industrial VARCHAR(80) NOT NULL," +
                    "fecha_registro VARCHAR(40) NOT NULL)");

            st.executeUpdate("INSERT IGNORE INTO empresas (codigo_empresa, nombre_empresa, rfc_empresa, sector_industrial, fecha_registro) VALUES " +
                    "('AUREA-2026', 'Manufactura Industrial del Centro', 'MIC260828IP9', 'Metalurgia y Maquinado CNC', '28 Ago 2026')," +
                    "('BEST-RACK26', 'Best Ingeniería y Logística', 'BIL210415RK8', 'Fabricación de Racks y Sistemas de Almacenamiento', '05 Sep 2026')," +
                    "('NVIDIA-2026', 'NVIDIA Corporation', 'NVD930405GP1', 'Semiconductores y Ensamble de Servidores IA', '12 Sep 2026')," +
                    "('BIMBO-PL26', 'Grupo Bimbo', 'GBI451202PA4', 'Industria Alimentaria y Panificación Automatizada', '18 Sep 2026')");

            st.executeUpdate("UPDATE empresas SET nombre_empresa = 'Manufactura Industrial del Centro' WHERE codigo_empresa = 'AUREA-2026' AND nombre_empresa LIKE '%urea%'");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS usuarios (" +
                    "id_usuario INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(100) NOT NULL," +
                    "correo VARCHAR(100) NOT NULL UNIQUE," +
                    "password VARCHAR(100) NOT NULL," +
                    "rol VARCHAR(60) NOT NULL," +
                    "estado VARCHAR(20) NOT NULL DEFAULT 'Activa'," +
                    "codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026')");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS maquinaria (" +
                    "codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026'," +
                    "codigo_maq VARCHAR(20) NOT NULL," +
                    "nombre VARCHAR(100) NOT NULL," +
                    "modelo VARCHAR(60) NOT NULL," +
                    "anio INT NOT NULL," +
                    "horas_uso INT NOT NULL DEFAULT 0," +
                    "fallas_previas VARCHAR(255) DEFAULT 'Ninguna'," +
                    "costo_adquisicion DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
                    "costo_mantenimiento DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
                    "estado_operativo VARCHAR(30) NOT NULL DEFAULT 'Óptimo'," +
                    "ultimo_registro VARCHAR(40) NOT NULL," +
                    "PRIMARY KEY (codigo_empresa, codigo_maq))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS monitoreo_bitacora (" +
                    "id_lectura INT AUTO_INCREMENT PRIMARY KEY," +
                    "codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026'," +
                    "codigo_maq VARCHAR(20) NOT NULL," +
                    "temperatura DECIMAL(6,2) NOT NULL," +
                    "presion DECIMAL(6,2) NOT NULL," +
                    "vibracion DECIMAL(6,2) NOT NULL," +
                    "observaciones VARCHAR(255) NOT NULL," +
                    "responsable VARCHAR(100) NOT NULL," +
                    "fecha_hora VARCHAR(40) NOT NULL," +
                    "estado_detectado VARCHAR(30) NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS mantenimientos (" +
                    "id_mant INT AUTO_INCREMENT PRIMARY KEY," +
                    "codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026'," +
                    "codigo_maq VARCHAR(20) NOT NULL," +
                    "tipo VARCHAR(30) NOT NULL," +
                    "actividades VARCHAR(255) NOT NULL," +
                    "refacciones VARCHAR(150) NOT NULL," +
                    "fecha_programada VARCHAR(30) NOT NULL," +
                    "tecnico_responsable VARCHAR(100) NOT NULL," +
                    "alerta_proxima VARCHAR(40) NOT NULL DEFAULT 'Programado')");

            if (!existeColumna(con, "usuarios", "codigo_empresa")) {
                st.executeUpdate("ALTER TABLE usuarios ADD COLUMN codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026'");
            }
            if (!existeColumna(con, "maquinaria", "codigo_empresa")) {
                st.executeUpdate("ALTER TABLE maquinaria ADD COLUMN codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026' FIRST");
                try {
                    st.executeUpdate("ALTER TABLE maquinaria DROP PRIMARY KEY, ADD PRIMARY KEY (codigo_empresa, codigo_maq)");
                } catch (Exception ignored) {}
            }
            if (!existeColumna(con, "monitoreo_bitacora", "codigo_empresa")) {
                st.executeUpdate("ALTER TABLE monitoreo_bitacora ADD COLUMN codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026' AFTER id_lectura");
            }
            if (!existeColumna(con, "mantenimientos", "codigo_empresa")) {
                st.executeUpdate("ALTER TABLE mantenimientos ADD COLUMN codigo_empresa VARCHAR(25) NOT NULL DEFAULT 'AUREA-2026' AFTER id_mant");
            }

            st.executeUpdate("INSERT IGNORE INTO usuarios (nombre, correo, password, rol, estado, codigo_empresa) VALUES " +

                    "('Iker Donovan Chavez Ramos', 'sup.general@aurea.mx', 'Aurea2026*', 'Supervisor General', 'Activa', 'AUREA-2026')," +
                    "('Ailyn Marquez Chaverria', 'sup.prod@aurea.mx', 'Aurea2026*', 'Supervisor de Producción', 'Activa', 'AUREA-2026')," +
                    "('Miguel Angel Velazquez Arroyo', 'sup.finanzas@aurea.mx', 'Aurea2026*', 'Supervisor Financiero', 'Activa', 'AUREA-2026')," +
                    "('Andressa Vega Martinez', 'tec.senior@aurea.mx', 'Aurea2026*', 'Técnico Senior', 'Activa', 'AUREA-2026')," +
                    "('Carlos Eduardo Méndez Ríos', 'tec.lineab@aurea.mx', 'Aurea2026*', 'Técnico de Línea B', 'Activa', 'AUREA-2026')," +
                    "('Fernando Silva Castellanos', 'tec.diagnostico@aurea.mx', 'Aurea2026*', 'Técnico de Diagnóstico y Soporte', 'Activa', 'AUREA-2026')," +
                    "('Roberto Sánchez Morales', 'inactivo@aurea.mx', 'Aurea2026*', 'Técnico de Línea B', 'Inactiva', 'AUREA-2026')," +
                    "('Javier Ramírez Ortiz', 'j.ramirez@aurea.mx', 'Aurea2026*', 'Técnico de Línea B', 'Pendiente', 'AUREA-2026')," +

                    "('Ing. Ricardo Morales Vega', 'sup.general@bestlogistica.mx', 'Aurea2026*', 'Supervisor General', 'Activa', 'BEST-RACK26')," +
                    "('Ing. Sofía Hernández Luna', 'sup.prod@bestlogistica.mx', 'Aurea2026*', 'Supervisor de Producción', 'Activa', 'BEST-RACK26')," +
                    "('Lic. Daniel Ortega Ruiz', 'sup.finanzas@bestlogistica.mx', 'Aurea2026*', 'Supervisor Financiero', 'Activa', 'BEST-RACK26')," +
                    "('Marco Antonio Solís Peña', 'tec.senior@bestlogistica.mx', 'Aurea2026*', 'Técnico Senior', 'Activa', 'BEST-RACK26')," +
                    "('Héctor Iván Domínguez', 'tec.lineab@bestlogistica.mx', 'Aurea2026*', 'Técnico de Línea B', 'Activa', 'BEST-RACK26')," +
                    "('Luis Ángel Torres', 'solicitud@bestlogistica.mx', 'Aurea2026*', 'Técnico de Diagnóstico y Soporte', 'Pendiente', 'BEST-RACK26')," +

                    "('Ing. Alejandro Mendoza', 'sup.general@nvidia.com', 'Aurea2026*', 'Supervisor General', 'Activa', 'NVIDIA-2026')," +
                    "('Ing. Elena Vázquez', 'sup.prod@nvidia.com', 'Aurea2026*', 'Supervisor de Producción', 'Activa', 'NVIDIA-2026')," +
                    "('Lic. Roberto Chang', 'sup.finanzas@nvidia.com', 'Aurea2026*', 'Supervisor Financiero', 'Activa', 'NVIDIA-2026')," +
                    "('David Chen', 'tec.senior@nvidia.com', 'Aurea2026*', 'Técnico Senior', 'Activa', 'NVIDIA-2026')," +
                    "('Valeria Castro', 'solicitud@nvidia.com', 'Aurea2026*', 'Técnico de Línea B', 'Pendiente', 'NVIDIA-2026')," +

                    "('Ing. Gabriel Servitje', 'sup.general@bimbo.com', 'Aurea2026*', 'Supervisor General', 'Activa', 'BIMBO-PL26')," +
                    "('Jorge Luis Pineda', 'tec.senior@bimbo.com', 'Aurea2026*', 'Técnico Senior', 'Activa', 'BIMBO-PL26')");

            st.executeUpdate("INSERT IGNORE INTO maquinaria (codigo_empresa, codigo_maq, nombre, modelo, anio, horas_uso, fallas_previas, costo_adquisicion, costo_mantenimiento, estado_operativo, ultimo_registro) VALUES " +

                    "('AUREA-2026', 'TR-104', 'TR-104 - Torno Industrial', 'CNC-TX200', 2024, 1420, 'Ninguna', 85000.00, 4200.00, 'Óptimo', '19 Nov 08:30')," +
                    "('AUREA-2026', 'CO-202', 'CO-202 - Compresor Neumático', 'ATLAS-500', 2023, 2150, 'Válvula de alivio', 42000.00, 3100.00, 'Óptimo', '19 Nov 09:15')," +
                    "('AUREA-2026', 'BA-301', 'BA-301 - Banda Transportadora', 'FLEX-B3', 2022, 3890, 'Desgaste rodamiento', 29500.00, 6800.00, 'Alerta', '19 Nov 10:05')," +
                    "('AUREA-2026', 'PR-405', 'PR-405 - Prensa Hidráulica 200T', 'SCHULER-H2', 2024, 980, 'Ninguna', 135000.00, 2900.00, 'Óptimo', '19 Nov 10:40')," +
                    "('AUREA-2026', 'FR-510', 'FR-510 - Fresadora Vertical 5 Ejes', 'HAAS-VF4', 2025, 640, 'Ninguna', 210000.00, 1800.00, 'Óptimo', '19 Nov 11:10')," +
                    "('AUREA-2026', 'SO-612', 'SO-612 - Celda de Soldadura Robótica', 'FANUC-ARC', 2023, 2740, 'Calibración de servomotor', 178000.00, 9400.00, 'En Mantenimiento', '19 Nov 11:50')," +

                    "('BEST-RACK26', 'PF-101', 'PF-101 - Perfiladora de Poste para Rack Selectivo', 'ROLL-FORM-800', 2024, 1850, 'Ninguna', 145000.00, 6400.00, 'Óptimo', '19 Nov 08:15')," +
                    "('BEST-RACK26', 'PN-204', 'PN-204 - Punzonadora CNC de Vigas y Conectores', 'TRUMPF-T3000', 2023, 2410, 'Ninguna', 190000.00, 8200.00, 'Óptimo', '19 Nov 09:00')," +
                    "('BEST-RACK26', 'SR-308', 'SR-308 - Robot de Soldadura MIG para Bastidores', 'KUKA-KR16', 2024, 1620, 'Sobrecalentamiento (83.2 °C)', 210000.00, 11500.00, 'Alerta', '19 Nov 09:45')," +
                    "('BEST-RACK26', 'CB-412', 'CB-412 - Cabina de Pintura Electrostática Epóxica', 'GEMA-OPTIFLEX', 2022, 3320, 'Limpieza de boquillas', 118000.00, 9100.00, 'Óptimo', '19 Nov 10:20')," +
                    "('BEST-RACK26', 'CT-501', 'CT-501 - Cortadora Láser de Fibra para Placa Base', 'BYSTRONIC-6KW', 2025, 790, 'Cambio de lente óptico', 265000.00, 4300.00, 'En Mantenimiento', '19 Nov 11:00')," +

                    "('NVIDIA-2026', 'SM-01', 'SM-01 - Línea SMT de Montaje Superficial GPU Blackwell', 'ASMPT-SIPLACE-SX', 2025, 1120, 'Ninguna', 480000.00, 12000.00, 'Óptimo', '19 Nov 07:50')," +
                    "('NVIDIA-2026', 'RF-02', 'RF-02 - Horno de Reflujo por Nitrógeno 12 Zonas', 'HELLER-1936-MK7', 2024, 2290, 'Sobrecalentamiento (86.4 °C)', 320000.00, 18500.00, 'Alerta', '19 Nov 08:40')," +
                    "('NVIDIA-2026', 'AO-03', 'AO-03 - Inspección Óptica y Rayos X 3D para Wafers', 'KOH-YOUNG-ZENITH', 2025, 840, 'Ninguna', 395000.00, 5400.00, 'Óptimo', '19 Nov 09:30')," +
                    "('NVIDIA-2026', 'CH-04', 'CH-04 - Chiller de Enfriamiento Líquido para Burn-In DGX', 'BOYD-LIQUID-X800', 2024, 3100, 'Ninguna', 275000.00, 9800.00, 'Óptimo', '19 Nov 10:15')," +
                    "('NVIDIA-2026', 'PK-05', 'PK-05 - Estación Automatizada de Encapsulado CoWoS', 'BESI-8800-ULTRA', 2025, 610, 'Ninguna', 540000.00, 7200.00, 'Óptimo', '19 Nov 11:05')," +

                    "('BIMBO-PL26', 'AM-10', 'AM-10 - Amasadora Industrial Continua de Alto Volumen', 'DIOSNA-W240', 2024, 2600, 'Ninguna', 110000.00, 5200.00, 'Óptimo', '19 Nov 06:30')," +
                    "('BIMBO-PL26', 'HT-20', 'HT-20 - Horno de Túnel Continuo de Gas Natural', 'BAKER-PERKINS-90', 2023, 4120, 'Sobrecalentamiento (85.0 °C)', 195000.00, 14300.00, 'Alerta', '19 Nov 07:45')," +
                    "('BIMBO-PL26', 'EM-30', 'EM-30 - Empacadora Flow-Pack de Alta Velocidad', 'BOSCH-PACK-401', 2024, 1950, 'Ninguna', 98000.00, 3900.00, 'Óptimo', '19 Nov 08:20')");

            try (ResultSet rsBit = st.executeQuery("SELECT COUNT(*) FROM monitoreo_bitacora WHERE codigo_empresa = 'BEST-RACK26'")) {
                if (rsBit.next() && rsBit.getInt(1) == 0) {
                    st.executeUpdate("INSERT INTO monitoreo_bitacora (codigo_empresa, codigo_maq, temperatura, presion, vibracion, observaciones, responsable, fecha_hora, estado_detectado) VALUES " +

                            "('AUREA-2026', 'PR-405', 61.0, 128.0, 1.9, 'Presión hidráulica estable en ciclo de estampado', 'Técnico Senior', '19 Nov 10:40', 'Óptimo')," +
                            "('AUREA-2026', 'FR-510', 58.5, 105.0, 1.4, 'Husillo operando sin vibración anormal', 'Técnico Senior', '19 Nov 11:10', 'Óptimo')," +

                            "('BEST-RACK26', 'PF-101', 64.0, 115.0, 2.3, 'Perfilado continuo de postes calibre 12 en rango nominal', 'Técnico Senior', '19 Nov 08:15', 'Óptimo')," +
                            "('BEST-RACK26', 'PN-204', 66.5, 122.0, 2.8, 'Punzonado de conectores de viga sin desviación', 'Técnico de Línea B', '19 Nov 09:00', 'Óptimo')," +
                            "('BEST-RACK26', 'SR-308', 83.2, 110.0, 6.9, 'Incremento térmico en antorcha MIG y vibración en eje 3', 'Técnico Senior', '19 Nov 09:45', 'Alerta')," +
                            "('BEST-RACK26', 'CB-412', 71.0, 95.0, 1.8, 'Curado de pintura epóxica naranja/azul para racks', 'Técnico de Línea B', '19 Nov 10:20', 'Óptimo')," +

                            "('NVIDIA-2026', 'SM-01', 54.0, 98.0, 0.9, 'Colocación de componentes SMD en PCB de alta densidad', 'Técnico Senior', '19 Nov 07:50', 'Óptimo')," +
                            "('NVIDIA-2026', 'RF-02', 86.4, 102.0, 2.4, 'Desviación térmica en zona 8 de reflujo por nitrógeno', 'Técnico Senior', '19 Nov 08:40', 'Alerta')," +
                            "('NVIDIA-2026', 'AO-03', 49.5, 90.0, 0.6, 'Inspección óptica de soldadura BGA al 99.98% de precisión', 'Técnico Senior', '19 Nov 09:30', 'Óptimo')," +
                            "('NVIDIA-2026', 'CH-04', 52.0, 118.0, 1.7, 'Caudal de refrigerante dieléctrico estable en racks DGX', 'Técnico Senior', '19 Nov 10:15', 'Óptimo')," +

                            "('BIMBO-PL26', 'AM-10', 59.0, 100.0, 2.5, 'Torque de amasado continuo dentro de especificación', 'Técnico Senior', '19 Nov 06:30', 'Óptimo')," +
                            "('BIMBO-PL26', 'HT-20', 85.0, 112.0, 4.1, 'Temperatura elevada en rodamiento de banda de horneado', 'Técnico Senior', '19 Nov 07:45', 'Alerta')");
                }
            }

            try (ResultSet rsMant = st.executeQuery("SELECT COUNT(*) FROM mantenimientos WHERE codigo_empresa = 'BEST-RACK26'")) {
                if (rsMant.next() && rsMant.getInt(1) == 0) {
                    st.executeUpdate("INSERT INTO mantenimientos (codigo_empresa, codigo_maq, tipo, actividades, refacciones, fecha_programada, tecnico_responsable, alerta_proxima) VALUES " +

                            "('AUREA-2026', 'SO-612', 'Correctivo', 'Reemplazo de encoder y calibración de brazo soldador', 'Encoder Fanuc A860-2000', '2026-11-20', 'Técnico Senior', 'Próximo (< 48h)')," +

                            "('BEST-RACK26', 'SR-308', 'Correctivo', 'Cambio de difusor de gas, tobera MIG y revisión de enfriador', 'Kit Antorcha Fronius 500A', '2026-11-20', 'Técnico Senior', 'Próximo (< 48h)')," +
                            "('BEST-RACK26', 'CT-501', 'Preventivo', 'Reemplazo de ventana protectora de cuarzo y alineación óptica', 'Lente Colimador Bystronic 6kW', '2026-11-21', 'Técnico Senior', 'Próximo (< 48h)')," +
                            "('BEST-RACK26', 'PF-101', 'Preventivo', 'Ajuste y lubricación de rodillos conformadores de poste', 'Grasa Extrema Presión EP-2', '2026-11-28', 'Técnico de Línea B', 'Programado')," +

                            "('NVIDIA-2026', 'RF-02', 'Correctivo', 'Calibración de termopares y reemplazo de ventilador de convección Z8', 'Módulo Térmico Heller Z8-SSR', '2026-11-20', 'Técnico Senior', 'Próximo (< 48h)')," +
                            "('NVIDIA-2026', 'SM-01', 'Preventivo', 'Limpieza ultrasónica de boquillas de vacío pick-and-place', 'Kit Boquillas Cerámicas Siplace', '2026-11-27', 'Técnico Senior', 'Programado')," +
                            "('NVIDIA-2026', 'CH-04', 'Preventivo', 'Sustitución de filtros de micro-partículas en circuito primario', 'Filtro Hidráulico 5 Micras', '2026-11-14', 'Técnico Senior', 'Completado')," +

                            "('BIMBO-PL26', 'HT-20', 'Correctivo', 'Lubricación de alta temperatura en chumaceras del horno túnel', 'Grasa Grado Alimenticio NSF-H1', '2026-11-20', 'Técnico Senior', 'Próximo (< 48h)')");
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
