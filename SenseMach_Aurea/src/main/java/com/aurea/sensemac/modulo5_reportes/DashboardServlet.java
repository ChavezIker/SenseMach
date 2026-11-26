package com.aurea.sensemac.modulo5_reportes;

import com.aurea.sensemac.modulo1_usuarios.UsuarioDAO;
import com.aurea.sensemac.modulo2_maquinaria.MaquinariaDAO;
import com.aurea.sensemac.modulo3_monitoreo.MonitoreoDAO;
import com.aurea.sensemac.modulo4_mantenimiento.MantenimientoDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

// servlet que concentra los datos exclusivos de la empresa activa para la vista dashboard.jsp
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final MaquinariaDAO maquinariaDAO = new MaquinariaDAO();
    private final MonitoreoDAO monitoreoDAO = new MonitoreoDAO();
    private final MantenimientoDAO mantenimientoDAO = new MantenimientoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession sesion = req.getSession();

        String codigoEmpresa = (String) sesion.getAttribute("codigoEmpresa");
        if (codigoEmpresa == null || codigoEmpresa.isEmpty()) {
            codigoEmpresa = "AUREA-2026";
        }

        if (sesion.getAttribute("alertaExito") != null) {
            req.setAttribute("alertaExito", sesion.getAttribute("alertaExito"));
            sesion.removeAttribute("alertaExito");
        }
        if (sesion.getAttribute("alertaError") != null) {
            req.setAttribute("alertaError", sesion.getAttribute("alertaError"));
            sesion.removeAttribute("alertaError");
        }

        req.setAttribute("usuarios", usuarioDAO.listarUsuariosPorEmpresa(codigoEmpresa));
        req.setAttribute("maquinaria", maquinariaDAO.listarMaquinariaPorEmpresa(codigoEmpresa));
        req.setAttribute("bitacora", monitoreoDAO.listarBitacoraPorEmpresa(codigoEmpresa));
        req.setAttribute("mantenimientos", mantenimientoDAO.listarMantenimientosPorEmpresa(codigoEmpresa));

        req.getRequestDispatcher("dashboard.jsp").forward(req, resp);
    }
}
