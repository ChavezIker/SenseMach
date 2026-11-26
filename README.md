# ÁUREA — Sistema SenseMach (Web + App Android)

> *"Eficiencia que se mide, calidad que se nota"*
> **IPN — CECyT No. 9 "Juan de Dios Bátiz"** | Proyecto Aula Semestral

Plataforma multiempresa de monitoreo de maquinaria industrial, gestión de órdenes de mantenimiento (CMMS), trazabilidad de fallas y control financiero de activos.

---

## Estructura del Repositorio

```text
├── SenseMach_Aurea/   # Aplicación Web Java EE (Servlets, JSP, MySQL 8.0, Apache Tomcat 9, API JSON /api/*)
└── SensMach-App/      # Aplicación Móvil Android Studio (Java, Material 3, conectada a la misma BD vía API)
```

---

## 1. Proyecto Web (`SenseMach_Aurea`)
* **Stack:** Java 11+, Servlets (`javax.servlet`), JSP, Bootstrap 5, MySQL 8.0 (`sensemac_db`), Apache Tomcat 9.
* **Cómo abrir en IntelliJ IDEA:**
  1. `File -> Open` y selecciona la carpeta `SenseMach_Aurea`.
  2. Configura un servidor **Apache Tomcat 9** (o SmartTomcat) apuntando al proyecto.
  3. Configura MySQL local (`localhost:3306`, usuario `root`, contraseña `angel735`) o define las variables de entorno `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSL` para despliegue en nube (Azure).
  4. Al arrancar, `ConexionDB.java` crea automáticamente el esquema `sensemac_db`, las 5 tablas (`empresas`, `usuarios`, `maquinaria`, `monitoreo_bitacora`, `mantenimientos`) y los datos iniciales.

## 2. Aplicación Móvil (`SensMach-App`)
* **Stack:** Android Studio (Java 11, Material 3, Gradle 9.5, Android SDK 36, minSdk 24).
* **Funcionalidades rápidas adaptadas por rol:**
  * Inicio de sesión con 3 factores (Correo, Contraseña Personal y Clave Única de Empresa).
  * Consulta rápida de eficiencia OEE, alertas y maquinaria de la empresa.
  * Registro rápido de lecturas de sensores con evaluación de umbrales en tiempo real.
  * Gestión rápida de órdenes de mantenimiento (crear y concluir en un toque).
  * Aprobación/rechazo rápido de solicitudes de acceso y consulta de Clave Única *(exclusivo para Supervisor General)*.
* **Cómo abrir en Android Studio:**
  1. `File -> Open` y selecciona la carpeta `SensMach-App`.
  2. Ejecuta en un emulador o dispositivo físico Android. Desde el botón **Servidor** en la pantalla de acceso puedes elegir el backend en Azure o tu servidor local (`http://10.0.2.2:8080/SenseMach`).

---

## Equipo Directivo — ÁUREA
* **Chávez Ramos Iker Donovan** — CEO / Dirección General
* **Vega Martínez Andressa** — Directora en Programación, BD y Seguridad de Roles
* **Márquez Chavarría Ailyn** — Directora en Desarrollo de Maquinaria y Mantenimiento
* **Velázquez Arroyo Miguel Ángel** — Director de Marketing, Documentación y QA
