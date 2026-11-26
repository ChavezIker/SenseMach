package com.aurea.sensemac.modulo3_monitoreo;

import com.aurea.sensemac.modulo2_maquinaria.MaquinariaDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// controlador para monitoreo de parametros fisicos en tiempo real por empresa
@WebServlet(name = "MonitoreoServlet", urlPatterns = {"/monitoreo"})
public class MonitoreoServlet extends HttpServlet {

    private final MonitoreoDAO monitoreoDAO = new MonitoreoDAO();
    private final MaquinariaDAO maquinariaDAO = new MaquinariaDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession sesion = req.getSession();
        String codigoEmpresa = (String) sesion.getAttribute("codigoEmpresa");
        if (codigoEmpresa == null || codigoEmpresa.isEmpty()) {
            codigoEmpresa = "AUREA-2026";
        }
        String ahora = new SimpleDateFormat("dd MMM HH:mm", new Locale("es", "MX")).format(new Date());

        String idMaq = req.getParameter("idMaq");
        double temp = Double.parseDouble(req.getParameter("temperatura"));
        double pres = Double.parseDouble(req.getParameter("presion"));
        double vib = Double.parseDouble(req.getParameter("vibracion"));
        int horasTurno = 0;
        try {
            if (req.getParameter("horasTurno") != null && !req.getParameter("horasTurno").trim().isEmpty()) {
                horasTurno = Integer.parseInt(req.getParameter("horasTurno").trim());
            }
        } catch (Exception ignored) {}
        String obs = req.getParameter("observaciones");
        String rolActual = (String) sesion.getAttribute("usuarioRol");

        List<String> anomalias = new ArrayList<>();
        if (temp > 80.0) {
            anomalias.add(String.format(Locale.US, "Sobrecalentamiento (%.1f °C)", temp));
        }
        if (pres < 60.0 || pres > 150.0) {
            anomalias.add(String.format(Locale.US, "Presión fuera de norma (%.1f psi)", pres));
        }
        if (vib > 6.5) {
            anomalias.add(String.format(Locale.US, "Vibración excesiva (%.1f mm/s)", vib));
        }

        String nuevoEstado = anomalias.isEmpty() ? "Óptimo" : "Alerta";
        String resumenAnomalias = String.join(", ", anomalias);
        String notaFinal = (obs == null || obs.trim().isEmpty())
                ? (anomalias.isEmpty() ? "Inspección instrumental en planta dentro de norma" : "Anomalía: " + resumenAnomalias)
                : obs.trim();

        monitoreoDAO.registrarLectura(codigoEmpresa, idMaq, temp, pres, vib, notaFinal, rolActual != null ? rolActual : "Técnico", ahora, nuevoEstado);
        if (horasTurno > 0) {
            maquinariaDAO.incrementarHorasUso(codigoEmpresa, idMaq, horasTurno, ahora);
        }

        if ("Alerta".equals(nuevoEstado)) {
            maquinariaDAO.registrarAnomalia(codigoEmpresa, idMaq, resumenAnomalias, ahora);
            sesion.setAttribute("alertaError", "Anomalía detectada en " + idMaq + ": " + resumenAnomalias + ". Estado actualizado a Alerta.");
        } else {
            maquinariaDAO.actualizarEstado(codigoEmpresa, idMaq, "Óptimo", ahora);
            sesion.setAttribute("alertaExito", "Lectura operativa registrada correctamente. Estado del equipo " + idMaq + ": Óptimo.");
        }
        resp.sendRedirect("dashboard?tab=monitoreo");
    }
}
