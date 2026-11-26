package com.aurea.sensemac.modulo2_maquinaria;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// controlador para alta de maquinaria, control de estados operativos y costos por empresa
@WebServlet(name = "MaquinariaServlet", urlPatterns = {"/maquinaria"})
public class MaquinariaServlet extends HttpServlet {

    private final MaquinariaDAO maquinariaDAO = new MaquinariaDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");
        HttpSession sesion = req.getSession();
        String codigoEmpresa = (String) sesion.getAttribute("codigoEmpresa");
        if (codigoEmpresa == null || codigoEmpresa.isEmpty()) {
            codigoEmpresa = "AUREA-2026";
        }
        String ahora = new SimpleDateFormat("dd MMM HH:mm", new Locale("es", "MX")).format(new Date());

        if ("registrarMaquina".equals(accion)) {
            String id = req.getParameter("id") != null ? req.getParameter("id").trim().toUpperCase() : "";
            if (id.isEmpty()) {
                sesion.setAttribute("alertaError", "Debe especificar un código identificador para la máquina.");
                resp.sendRedirect("dashboard?tab=maquinaria");
                return;
            }
            if (maquinariaDAO.existeId(id, codigoEmpresa)) {
                sesion.setAttribute("alertaError", "El ID de la maquinaria ya existe en el catálogo de su empresa. Verifique la información.");
                resp.sendRedirect("dashboard?tab=maquinaria");
                return;
            }
            String nombre = req.getParameter("nombre") != null ? req.getParameter("nombre").trim() : "";
            String modelo = req.getParameter("modelo") != null ? req.getParameter("modelo").trim() : "";
            int anio = Integer.parseInt(req.getParameter("anio"));
            int horas = Integer.parseInt(req.getParameter("horas"));
            String fallas = req.getParameter("fallas");
            String estado = req.getParameter("estado");

            maquinariaDAO.registrarMaquina(codigoEmpresa, id, nombre, modelo, anio, horas, fallas, estado, ahora);
            sesion.setAttribute("alertaExito", "Máquina " + id + " registrada con éxito en el catálogo de su empresa.");
        }

        if ("actualizarOperacion".equals(accion)) {
            String idMaq = req.getParameter("idMaq");
            int horas = Integer.parseInt(req.getParameter("horas"));
            String fallas = req.getParameter("fallas");
            String estado = req.getParameter("estado");

            boolean ok = maquinariaDAO.actualizarOperacionMaquina(codigoEmpresa, idMaq, horas, fallas, estado, ahora);
            if (ok) {
                sesion.setAttribute("alertaExito", "Estado operativo y trazabilidad del equipo " + idMaq + " actualizados.");
            } else {
                sesion.setAttribute("alertaError", "No se pudo actualizar la información del equipo " + idMaq + ".");
            }
        }

        if ("actualizarCostos".equals(accion)) {
            String idMaq = req.getParameter("idMaq");
            double costoAdq = Double.parseDouble(req.getParameter("costoAdq"));
            double costoMant = Double.parseDouble(req.getParameter("costoMant"));
            maquinariaDAO.actualizarCostos(codigoEmpresa, idMaq, costoAdq, costoMant, ahora);
            sesion.setAttribute("alertaExito", "Costos financieros actualizados para el equipo " + idMaq + ".");
        }

        if ("eliminarMaquina".equals(accion)) {
            String idMaq = req.getParameter("idMaq");
            boolean ok = maquinariaDAO.eliminarMaquina(codigoEmpresa, idMaq);
            if (ok) {
                sesion.setAttribute("alertaExito", "Equipo " + idMaq + " dado de baja del catálogo de su empresa.");
            } else {
                sesion.setAttribute("alertaError", "No se pudo dar de baja el equipo seleccionado.");
            }
        }

        resp.sendRedirect("dashboard?tab=maquinaria");
    }
}
