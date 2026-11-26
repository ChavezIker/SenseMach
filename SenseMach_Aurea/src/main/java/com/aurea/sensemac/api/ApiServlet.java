package com.aurea.sensemac.api;

import com.aurea.sensemac.modulo1_usuarios.UsuarioDAO;
import com.aurea.sensemac.modulo2_maquinaria.MaquinariaDAO;
import com.aurea.sensemac.modulo3_monitoreo.MonitoreoDAO;
import com.aurea.sensemac.modulo4_mantenimiento.MantenimientoDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

// api json ligera para la aplicacion movil android (SensMach-App) conectada a la misma base de datos
@WebServlet(name = "ApiServlet", urlPatterns = {"/api/*"})
public class ApiServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final MaquinariaDAO maquinariaDAO = new MaquinariaDAO();
    private final MonitoreoDAO monitoreoDAO = new MonitoreoDAO();
    private final MantenimientoDAO mantenimientoDAO = new MantenimientoDAO();

    private boolean esClaveEjemploProhibida(String clave) {
        if (clave == null) return false;
        String c = clave.trim().toUpperCase();
        return "XXXX-0000".equals(c) || "EJ. XXXX-0000".equals(c) || "XXXX0000".equals(c) || "0000-0000".equals(c);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private void configurarJson(HttpServletResponse resp) {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        configurarJson(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        configurarJson(resp);
        PrintWriter out = resp.getWriter();

        String path = req.getPathInfo() != null ? req.getPathInfo() : "";

        if ("/ping".equals(path)) {
            out.print("{\"ok\":true,\"servicio\":\"SenseMach Mobile API\"}");
            return;
        }

        if ("/resumen".equals(path)) {
            String codigoEmpresa = req.getParameter("codigoEmpresa") != null ? req.getParameter("codigoEmpresa").trim().toUpperCase() : "";
            String rol = req.getParameter("rol") != null ? req.getParameter("rol").trim() : "";

            if (codigoEmpresa.isEmpty()) {
                out.print("{\"ok\":false,\"error\":\"Falta codigoEmpresa\"}");
                return;
            }

            List<Map<String, String>> maquinaria = maquinariaDAO.listarMaquinariaPorEmpresa(codigoEmpresa);
            List<Map<String, String>> bitacora = monitoreoDAO.listarBitacoraPorEmpresa(codigoEmpresa);
            List<Map<String, String>> mantenimientos = mantenimientoDAO.listarMantenimientosPorEmpresa(codigoEmpresa);
            List<Map<String, String>> usuarios = "Supervisor General".equalsIgnoreCase(rol)
                    ? usuarioDAO.listarUsuariosPorEmpresa(codigoEmpresa)
                    : Collections.emptyList();

            Map<String, Map<String, String>> ultLect = new HashMap<>();
            for (Map<String, String> b : bitacora) {
                String cod = b.get("idMaq");
                if (cod != null && !ultLect.containsKey(cod)) {
                    ultLect.put(cod, b);
                }
            }

            int totalEquipos = maquinaria.size();
            int optimos = 0, alertas = 0, enMant = 0;
            double totalAdq = 0.0, totalMant = 0.0;

            for (Map<String, String> m : maquinaria) {
                String est = m.get("estado") != null ? m.get("estado") : "";
                if ("Óptimo".equalsIgnoreCase(est) || "Activa".equalsIgnoreCase(est)) optimos++;
                else if ("Alerta".equalsIgnoreCase(est)) alertas++;
                else if ("En Mantenimiento".equalsIgnoreCase(est)) enMant++;
                try { totalAdq += Double.parseDouble(m.get("costoAdq")); } catch (Exception ignored) {}
                try { totalMant += Double.parseDouble(m.get("costoMant")); } catch (Exception ignored) {}
            }

            double eficiencia = totalEquipos > 0
                    ? ((optimos * 100.0) + (enMant * 65.0) + (alertas * 35.0)) / totalEquipos
                    : 100.0;

            int ordenesPendientes = 0;
            for (Map<String, String> mt : mantenimientos) {
                if (!"Completado".equalsIgnoreCase(mt.get("alerta"))) {
                    ordenesPendientes++;
                }
            }

            List<Map<String, String>> pendientes = new ArrayList<>();
            int usuariosActivos = 0;
            for (Map<String, String> u : usuarios) {
                if ("Pendiente".equalsIgnoreCase(u.get("estado"))) {
                    pendientes.add(u);
                } else if ("Activa".equalsIgnoreCase(u.get("estado"))) {
                    usuariosActivos++;
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\"ok\":true,");
            sb.append("\"kpi\":{")
              .append("\"totalEquipos\":").append(totalEquipos).append(",")
              .append("\"optimos\":").append(optimos).append(",")
              .append("\"alertas\":").append(alertas).append(",")
              .append("\"enMantenimiento\":").append(enMant).append(",")
              .append("\"eficiencia\":").append(String.format(Locale.US, "%.1f", eficiencia)).append(",")
              .append("\"ordenesPendientes\":").append(ordenesPendientes).append(",")
              .append("\"solicitudesPendientes\":").append(pendientes.size()).append(",")
              .append("\"usuariosActivos\":").append(usuariosActivos).append(",")
              .append("\"totalCostoAdq\":").append(String.format(Locale.US, "%.2f", totalAdq)).append(",")
              .append("\"totalCostoMant\":").append(String.format(Locale.US, "%.2f", totalMant))
              .append("},");

            sb.append("\"maquinaria\":[");
            for (int i = 0; i < maquinaria.size(); i++) {
                Map<String, String> m = maquinaria.get(i);
                Map<String, String> lect = ultLect.get(m.get("id"));
                if (i > 0) sb.append(",");
                sb.append("{")
                  .append("\"id\":\"").append(esc(m.get("id"))).append("\",")
                  .append("\"nombre\":\"").append(esc(m.get("nombre"))).append("\",")
                  .append("\"modelo\":\"").append(esc(m.get("modelo"))).append("\",")
                  .append("\"anio\":\"").append(esc(m.get("anio"))).append("\",")
                  .append("\"horas\":\"").append(esc(m.get("horas"))).append("\",")
                  .append("\"fallas\":\"").append(esc(m.get("fallas"))).append("\",")
                  .append("\"costoAdq\":\"").append(esc(m.get("costoAdq"))).append("\",")
                  .append("\"costoMant\":\"").append(esc(m.get("costoMant"))).append("\",")
                  .append("\"estado\":\"").append(esc(m.get("estado"))).append("\",")
                  .append("\"fecha\":\"").append(esc(m.get("fecha"))).append("\",")
                  .append("\"temp\":\"").append(esc(lect != null ? lect.get("temp") : "--")).append("\",")
                  .append("\"pres\":\"").append(esc(lect != null ? lect.get("pres") : "--")).append("\",")
                  .append("\"vib\":\"").append(esc(lect != null ? lect.get("vib") : "--")).append("\"")
                  .append("}");
            }
            sb.append("],");

            sb.append("\"mantenimientos\":[");
            for (int i = 0; i < mantenimientos.size(); i++) {
                Map<String, String> mt = mantenimientos.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                  .append("\"idMant\":\"").append(esc(mt.get("idMant"))).append("\",")
                  .append("\"idMaq\":\"").append(esc(mt.get("idMaq"))).append("\",")
                  .append("\"tipo\":\"").append(esc(mt.get("tipo"))).append("\",")
                  .append("\"act\":\"").append(esc(mt.get("act"))).append("\",")
                  .append("\"ref\":\"").append(esc(mt.get("ref"))).append("\",")
                  .append("\"fecha\":\"").append(esc(mt.get("fecha"))).append("\",")
                  .append("\"tec\":\"").append(esc(mt.get("tec"))).append("\",")
                  .append("\"alerta\":\"").append(esc(mt.get("alerta"))).append("\"")
                  .append("}");
            }
            sb.append("],");

            sb.append("\"solicitudes\":[");
            for (int i = 0; i < pendientes.size(); i++) {
                Map<String, String> sol = pendientes.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                  .append("\"id\":\"").append(esc(sol.get("id"))).append("\",")
                  .append("\"nombre\":\"").append(esc(sol.get("nombre"))).append("\",")
                  .append("\"correo\":\"").append(esc(sol.get("correo"))).append("\",")
                  .append("\"rol\":\"").append(esc(sol.get("rol"))).append("\"")
                  .append("}");
            }
            sb.append("]}");
            out.print(sb.toString());
            return;
        }

        out.print("{\"ok\":false,\"error\":\"Endpoint no encontrado\"}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        configurarJson(resp);
        PrintWriter out = resp.getWriter();

        String path = req.getPathInfo() != null ? req.getPathInfo() : "";
        String ahora = new SimpleDateFormat("dd MMM HH:mm", new Locale("es", "MX")).format(new Date());

        if ("/login".equals(path)) {
            String correo = req.getParameter("correo") != null ? req.getParameter("correo").trim() : "";
            String pass = req.getParameter("password") != null ? req.getParameter("password") : "";
            String claveEmp = req.getParameter("codigoEmpresa") != null ? req.getParameter("codigoEmpresa").trim().toUpperCase() : "";

            if (correo.isEmpty() || pass.isEmpty() || claveEmp.isEmpty()) {
                out.print("{\"ok\":false,\"error\":\"Ingrese correo, contraseña personal y clave única de empresa.\"}");
                return;
            }
            if (esClaveEjemploProhibida(claveEmp)) {
                out.print("{\"ok\":false,\"error\":\"«" + esc(claveEmp) + "» es solo un formato de ejemplo.\"}");
                return;
            }

            Map<String, String> u = usuarioDAO.autenticar(correo, pass);
            if (u == null) {
                out.print("{\"ok\":false,\"error\":\"Correo o contraseña personal incorrectos.\"}");
                return;
            }
            if (!claveEmp.equalsIgnoreCase(u.get("codigoEmpresa"))) {
                out.print("{\"ok\":false,\"error\":\"La Clave Única de Empresa no corresponde a esta cuenta.\"}");
                return;
            }
            if ("Pendiente".equalsIgnoreCase(u.get("estado"))) {
                out.print("{\"ok\":false,\"error\":\"Su solicitud en «" + esc(u.get("nombreEmpresa")) + "» sigue pendiente de aprobación.\"}");
                return;
            }
            if (!"Activa".equalsIgnoreCase(u.get("estado"))) {
                out.print("{\"ok\":false,\"error\":\"Cuenta inactiva. Contacte al Supervisor General.\"}");
                return;
            }

            String json = "{\"ok\":true,\"usuario\":{" +
                    "\"id\":\"" + esc(u.get("id")) + "\"," +
                    "\"nombre\":\"" + esc(u.get("nombre")) + "\"," +
                    "\"correo\":\"" + esc(u.get("correo")) + "\"," +
                    "\"rol\":\"" + esc(u.get("rol")) + "\"," +
                    "\"codigoEmpresa\":\"" + esc(u.get("codigoEmpresa")) + "\"," +
                    "\"nombreEmpresa\":\"" + esc(u.get("nombreEmpresa")) + "\"," +
                    "\"sectorEmpresa\":\"" + esc(u.get("sectorEmpresa")) + "\"}}";
            out.print(json);
            return;
        }

        if ("/monitoreo".equals(path)) {
            try {
                String codigoEmpresa = req.getParameter("codigoEmpresa").trim().toUpperCase();
                String idMaq = req.getParameter("idMaq").trim();
                double temp = Double.parseDouble(req.getParameter("temperatura"));
                double pres = Double.parseDouble(req.getParameter("presion"));
                double vib = Double.parseDouble(req.getParameter("vibracion"));
                int horasTurno = 0;
                try { horasTurno = Integer.parseInt(req.getParameter("horasTurno")); } catch (Exception ignored) {}
                String obs = req.getParameter("observaciones");
                String rol = req.getParameter("rol") != null ? req.getParameter("rol") : "Técnico";

                List<String> anomalias = new ArrayList<>();
                if (temp > 80.0) anomalias.add(String.format(Locale.US, "Sobrecalentamiento (%.1f °C)", temp));
                if (pres < 60.0 || pres > 150.0) anomalias.add(String.format(Locale.US, "Presión fuera de norma (%.1f psi)", pres));
                if (vib > 6.5) anomalias.add(String.format(Locale.US, "Vibración excesiva (%.1f mm/s)", vib));

                String nuevoEstado = anomalias.isEmpty() ? "Óptimo" : "Alerta";
                String resumen = String.join(", ", anomalias);
                String notaFinal = (obs == null || obs.trim().isEmpty())
                        ? (anomalias.isEmpty() ? "Inspección rápida desde app móvil" : "Anomalía: " + resumen)
                        : obs.trim();

                monitoreoDAO.registrarLectura(codigoEmpresa, idMaq, temp, pres, vib, notaFinal, rol, ahora, nuevoEstado);
                if (horasTurno > 0) {
                    maquinariaDAO.incrementarHorasUso(codigoEmpresa, idMaq, horasTurno, ahora);
                }
                if ("Alerta".equals(nuevoEstado)) {
                    maquinariaDAO.registrarAnomalia(codigoEmpresa, idMaq, resumen, ahora);
                    out.print("{\"ok\":true,\"estado\":\"Alerta\",\"mensaje\":\"Anomalía detectada en " + esc(idMaq) + ": " + esc(resumen) + "\"}");
                } else {
                    maquinariaDAO.actualizarEstado(codigoEmpresa, idMaq, "Óptimo", ahora);
                    out.print("{\"ok\":true,\"estado\":\"Óptimo\",\"mensaje\":\"Lectura guardada. Equipo " + esc(idMaq) + " en rango Óptimo.\"}");
                }
            } catch (Exception e) {
                out.print("{\"ok\":false,\"error\":\"Verifique que los valores numéricos sean válidos.\"}");
            }
            return;
        }

        if ("/mantenimiento".equals(path)) {
            String accion = req.getParameter("accion") != null ? req.getParameter("accion") : "registrar";
            String codigoEmpresa = req.getParameter("codigoEmpresa") != null ? req.getParameter("codigoEmpresa").trim().toUpperCase() : "";

            if ("completar".equals(accion)) {
                try {
                    int idMant = Integer.parseInt(req.getParameter("idMant"));
                    String idMaq = req.getParameter("idMaq");
                    boolean ok = mantenimientoDAO.completarMantenimiento(idMant, codigoEmpresa);
                    if (ok && idMaq != null && !idMaq.isEmpty()) {
                        maquinariaDAO.actualizarEstado(codigoEmpresa, idMaq, "Óptimo", ahora);
                        out.print("{\"ok\":true,\"mensaje\":\"Orden concluida. Equipo " + esc(idMaq) + " en estado Óptimo.\"}");
                    } else {
                        out.print("{\"ok\":false,\"error\":\"No se pudo completar la orden.\"}");
                    }
                } catch (Exception e) {
                    out.print("{\"ok\":false,\"error\":\"Parámetros inválidos.\"}");
                }
                return;
            }

            String idMaq = req.getParameter("idMaq");
            String tipo = req.getParameter("tipo") != null ? req.getParameter("tipo") : "Preventivo";
            String act = req.getParameter("actividades");
            String ref = req.getParameter("refacciones");
            String fecha = req.getParameter("fecha");
            String rol = req.getParameter("rol") != null ? req.getParameter("rol") : "Técnico";
            double costo = 0.0;
            try { costo = Double.parseDouble(req.getParameter("costoIntervencion")); } catch (Exception ignored) {}

            if (act == null || act.trim().isEmpty() || ref == null || ref.trim().isEmpty() || fecha == null || fecha.trim().isEmpty()) {
                out.print("{\"ok\":false,\"error\":\"Complete actividades, refacciones y fecha programada.\"}");
                return;
            }
            mantenimientoDAO.registrarMantenimiento(codigoEmpresa, idMaq, tipo, act.trim(), ref.trim(), fecha.trim(), rol);
            if (costo > 0) maquinariaDAO.sumarCostoMantenimiento(codigoEmpresa, idMaq, costo, ahora);
            if ("Correctivo".equalsIgnoreCase(tipo)) maquinariaDAO.actualizarEstado(codigoEmpresa, idMaq, "En Mantenimiento", ahora);

            out.print("{\"ok\":true,\"mensaje\":\"Orden de mantenimiento registrada para " + esc(idMaq) + ".\"}");
            return;
        }

        if ("/maquinaria".equals(path)) {
            String accion = req.getParameter("accion") != null ? req.getParameter("accion") : "registrar";
            String codigoEmpresa = req.getParameter("codigoEmpresa") != null ? req.getParameter("codigoEmpresa").trim().toUpperCase() : "";

            if ("cambiarEstado".equals(accion)) {
                String idMaq = req.getParameter("idMaq");
                String estado = req.getParameter("estado");
                maquinariaDAO.actualizarEstado(codigoEmpresa, idMaq, estado, ahora);
                out.print("{\"ok\":true,\"mensaje\":\"Estado de " + esc(idMaq) + " actualizado a " + esc(estado) + ".\"}");
                return;
            }

            if ("actualizarCostos".equals(accion)) {
                try {
                    String idMaq = req.getParameter("idMaq");
                    double cAdq = Double.parseDouble(req.getParameter("costoAdq"));
                    double cMant = Double.parseDouble(req.getParameter("costoMant"));
                    maquinariaDAO.actualizarCostos(codigoEmpresa, idMaq, cAdq, cMant, ahora);
                    out.print("{\"ok\":true,\"mensaje\":\"Costos actualizados para " + esc(idMaq) + ".\"}");
                } catch (Exception e) {
                    out.print("{\"ok\":false,\"error\":\"Montos inválidos.\"}");
                }
                return;
            }

            try {
                String id = req.getParameter("id").trim().toUpperCase();
                if (maquinariaDAO.existeId(id, codigoEmpresa)) {
                    out.print("{\"ok\":false,\"error\":\"El código " + esc(id) + " ya existe en su empresa.\"}");
                    return;
                }
                String nombre = req.getParameter("nombre").trim();
                String modelo = req.getParameter("modelo").trim();
                int anio = Integer.parseInt(req.getParameter("anio"));
                int horas = Integer.parseInt(req.getParameter("horas"));
                String estado = req.getParameter("estado") != null ? req.getParameter("estado") : "Óptimo";
                maquinariaDAO.registrarMaquina(codigoEmpresa, id, nombre, modelo, anio, horas, "Ninguna", estado, ahora);
                out.print("{\"ok\":true,\"mensaje\":\"Máquina " + esc(id) + " registrada correctamente.\"}");
            } catch (Exception e) {
                out.print("{\"ok\":false,\"error\":\"Verifique todos los datos de la máquina.\"}");
            }
            return;
        }

        if ("/solicitudes".equals(path)) {
            String accion = req.getParameter("accion");
            String codigoEmpresa = req.getParameter("codigoEmpresa") != null ? req.getParameter("codigoEmpresa").trim().toUpperCase() : "";
            try {
                int idUsuario = Integer.parseInt(req.getParameter("idUsuario"));
                if ("aprobar".equals(accion)) {
                    String rol = req.getParameter("rol");
                    boolean ok = usuarioDAO.actualizarUsuario(idUsuario, rol, "Activa", codigoEmpresa);
                    out.print(ok ? "{\"ok\":true,\"mensaje\":\"Acceso aprobado correctamente.\"}"
                                 : "{\"ok\":false,\"error\":\"No se pudo aprobar la solicitud.\"}");
                } else if ("rechazar".equals(accion)) {
                    boolean ok = usuarioDAO.rechazarSolicitud(idUsuario, codigoEmpresa);
                    out.print(ok ? "{\"ok\":true,\"mensaje\":\"Solicitud rechazada.\"}"
                                 : "{\"ok\":false,\"error\":\"No se pudo rechazar la solicitud.\"}");
                } else {
                    out.print("{\"ok\":false,\"error\":\"Acción desconocida.\"}");
                }
            } catch (Exception e) {
                out.print("{\"ok\":false,\"error\":\"Parámetros inválidos.\"}");
            }
            return;
        }

        out.print("{\"ok\":false,\"error\":\"Endpoint no soportado\"}");
    }
}
