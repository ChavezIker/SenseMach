package com.aurea.sensemac.modulo4_mantenimiento;

import com.aurea.sensemac.modulo2_maquinaria.MaquinariaDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// controlador para gestion de ordenes de mantenimiento preventivo y correctivo por empresa
@WebServlet(name = "MantenimientoServlet", urlPatterns = {"/mantenimiento"})
public class MantenimientoServlet extends HttpServlet {

    private final MantenimientoDAO mantenimientoDAO = new MantenimientoDAO();
    private final MaquinariaDAO maquinariaDAO = new MaquinariaDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession sesion = req.getSession();
        String codigoEmpresa = (String) sesion.getAttribute("codigoEmpresa");
        if (codigoEmpresa == null || codigoEmpresa.isEmpty()) {
            codigoEmpresa = "AUREA-2026";
        }
        String accion = req.getParameter("accion");
        String ahora = new SimpleDateFormat("dd MMM HH:mm", new Locale("es", "MX")).format(new Date());

        if ("completar".equals(accion)) {
            try {
                int idMant = Integer.parseInt(req.getParameter("idMant"));
                String idMaq = req.getParameter("idMaq");
                boolean ok = mantenimientoDAO.completarMantenimiento(idMant, codigoEmpresa);
                if (ok && idMaq != null && !idMaq.isEmpty()) {
                    maquinariaDAO.actualizarEstado(codigoEmpresa, idMaq, "Óptimo", ahora);
                    sesion.setAttribute("alertaExito", "Intervención concluida. El equipo " + idMaq + " ha sido restablecido a estado Óptimo.");
                } else {
                    sesion.setAttribute("alertaError", "No fue posible actualizar el estado de la orden de mantenimiento.");
                }
            } catch (Exception e) {
                sesion.setAttribute("alertaError", "Solicitud inválida al concluir el mantenimiento.");
            }
            resp.sendRedirect("dashboard?tab=mantenimiento");
            return;
        }

        String idMaq = req.getParameter("idMaq");
        String tipo = req.getParameter("tipo");
        String act = req.getParameter("actividades");
        String ref = req.getParameter("refacciones");
        String fecha = req.getParameter("fecha");
        double costoIntervencion = 0.0;
        try {
            if (req.getParameter("costoIntervencion") != null && !req.getParameter("costoIntervencion").trim().isEmpty()) {
                costoIntervencion = Double.parseDouble(req.getParameter("costoIntervencion").trim());
            }
        } catch (Exception ignored) {}
        String rolActual = (String) sesion.getAttribute("usuarioRol");

        if (act == null || act.trim().isEmpty() || ref == null || ref.trim().isEmpty() || fecha == null || fecha.trim().isEmpty()) {
            sesion.setAttribute("alertaError", "Por favor complete todos los campos obligatorios del reporte de mantenimiento.");
            resp.sendRedirect("dashboard?tab=mantenimiento");
            return;
        }

        mantenimientoDAO.registrarMantenimiento(codigoEmpresa, idMaq, tipo, act.trim(), ref.trim(), fecha.trim(), rolActual != null ? rolActual : "Técnico");
        if (costoIntervencion > 0) {
            maquinariaDAO.sumarCostoMantenimiento(codigoEmpresa, idMaq, costoIntervencion, ahora);
        }
        if ("Correctivo".equalsIgnoreCase(tipo)) {
            maquinariaDAO.actualizarEstado(codigoEmpresa, idMaq, "En Mantenimiento", ahora);
        }
        sesion.setAttribute("alertaExito", "Mantenimiento registrado con éxito para el equipo " + idMaq + ".");
        resp.sendRedirect("dashboard?tab=mantenimiento");
    }
}
