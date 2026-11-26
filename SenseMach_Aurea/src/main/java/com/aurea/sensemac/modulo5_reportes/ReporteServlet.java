package com.aurea.sensemac.modulo5_reportes;

import com.aurea.sensemac.modulo2_maquinaria.MaquinariaDAO;
import com.aurea.sensemac.modulo4_mantenimiento.MantenimientoDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// controlador para exportacion ejecutiva de reportes filtrados por empresa en formato csv/excel
@WebServlet(name = "ReporteServlet", urlPatterns = {"/reportes"})
public class ReporteServlet extends HttpServlet {

    private final MaquinariaDAO maquinariaDAO = new MaquinariaDAO();
    private final MantenimientoDAO mantenimientoDAO = new MantenimientoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/csv; charset=UTF-8");

        HttpSession sesion = req.getSession();
        String codigoEmpresa = (String) sesion.getAttribute("codigoEmpresa");
        String nombreEmpresa = (String) sesion.getAttribute("nombreEmpresa");
        if (codigoEmpresa == null || codigoEmpresa.isEmpty()) {
            codigoEmpresa = "AUREA-2026";
        }
        if (nombreEmpresa == null || nombreEmpresa.isEmpty()) {
            nombreEmpresa = "Manufactura Industrial del Centro";
        }

        String tipoReporte = req.getParameter("tipo") != null ? req.getParameter("tipo").trim() : "general";
        String filtroEstado = req.getParameter("estado") != null ? req.getParameter("estado").trim() : "";
        String filtroEquipo = req.getParameter("equipo") != null ? req.getParameter("equipo").trim() : "";

        String nombreArchivo = "mantenimientos".equalsIgnoreCase(tipoReporte)
                ? "Reporte_Mantenimientos_SenseMach.csv"
                : "Reporte_Ejecutivo_SenseMach.csv";
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");

        PrintWriter out = resp.getWriter();

        out.write('\uFEFF');

        if ("mantenimientos".equalsIgnoreCase(tipoReporte)) {
            out.println("Empresa,Folio,Equipo,Tipo_Intervencion,Actividades_Realizadas,Refacciones_Utilizadas,Fecha_Programada,Responsable,Estado_Orden");
            List<Map<String, String>> mantenimientos = mantenimientoDAO.listarMantenimientosPorEmpresa(codigoEmpresa);
            for (Map<String, String> mt : mantenimientos) {
                if (!filtroEquipo.isEmpty() && !filtroEquipo.equalsIgnoreCase(mt.get("idMaq"))) {
                    continue;
                }
                out.println("\"" + nombreEmpresa.replace("\"", "\"\"") + "\"," +
                        mt.get("idMant") + "," + mt.get("idMaq") + "," + mt.get("tipo") + ",\"" +
                        mt.get("act").replace("\"", "\"\"") + "\",\"" + mt.get("ref").replace("\"", "\"\"") + "\"," +
                        mt.get("fecha") + ",\"" + mt.get("tec") + "\"," + mt.get("alerta"));
            }
            out.flush();
            return;
        }

        out.println("Empresa,ID,Equipo,Modelo,Anio,Horas_Uso,Estado_General,Fallas_Historicas,Costo_Adquisicion_MXN,Costo_Mantenimiento_MXN,Costo_Total_Activo_MXN,Ultimo_Registro");

        List<Map<String, String>> lista = maquinariaDAO.listarMaquinariaPorEmpresa(codigoEmpresa);
        int totalHoras = 0;
        double sumaAdq = 0.0;
        double sumaMant = 0.0;

        for (Map<String, String> m : lista) {
            if (!filtroEstado.isEmpty() && !filtroEstado.equalsIgnoreCase(m.get("estado"))) {
                continue;
            }
            if (!filtroEquipo.isEmpty() && !filtroEquipo.equalsIgnoreCase(m.get("id"))) {
                continue;
            }
            int horas = Integer.parseInt(m.get("horas"));
            double cAdq = Double.parseDouble(m.get("costoAdq"));
            double cMant = Double.parseDouble(m.get("costoMant"));
            double cTotal = cAdq + cMant;

            totalHoras += horas;
            sumaAdq += cAdq;
            sumaMant += cMant;

            out.println("\"" + nombreEmpresa.replace("\"", "\"\"") + "\"," +
                    m.get("id") + ",\"" + m.get("nombre").replace("\"", "\"\"") + "\"," + m.get("modelo") + "," +
                    m.get("anio") + "," + horas + "," + m.get("estado") + ",\"" +
                    m.get("fallas").replace("\"", "\"\"") + "\"," +
                    String.format(Locale.US, "%.2f", cAdq) + "," +
                    String.format(Locale.US, "%.2f", cMant) + "," +
                    String.format(Locale.US, "%.2f", cTotal) + ",\"" +
                    m.get("fecha") + "\"");
        }

        out.println("\"" + nombreEmpresa.replace("\"", "\"\"") + "\",TOTALES,CONSOLIDADO DE PLANTA,,," + totalHoras + ",,," +
                String.format(Locale.US, "%.2f", sumaAdq) + "," +
                String.format(Locale.US, "%.2f", sumaMant) + "," +
                String.format(Locale.US, "%.2f", (sumaAdq + sumaMant)) + ",");
        out.flush();
    }
}
