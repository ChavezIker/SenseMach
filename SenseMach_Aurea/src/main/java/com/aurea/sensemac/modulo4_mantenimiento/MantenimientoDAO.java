package com.aurea.sensemac.modulo4_mantenimiento;

import com.aurea.sensemac.config.ConexionDB;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

// dao encargado de ordenes de mantenimiento preventivo, correctivo y alertas por empresa
public class MantenimientoDAO {

    public static String calcularNivelAlerta(String fechaProgramada, String estadoActual) {
        if ("Completado".equalsIgnoreCase(estadoActual)) {
            return "Completado";
        }
        try {
            LocalDate fecha = LocalDate.parse(fechaProgramada);
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), fecha);
            if (dias < 0) {
                return "Vencido";
            } else if (dias <= 2) {
                return "Próximo (< 48h)";
            } else {
                return "Programado";
            }
        } catch (Exception e) {
            return (estadoActual != null && !estadoActual.isEmpty()) ? estadoActual : "Programado";
        }
    }

    public void registrarMantenimiento(String codigoEmpresa, String idMaq, String tipo, String act, String ref, String fecha, String tecnico) {
        String alertaInicial = calcularNivelAlerta(fecha, "Programado");
        String sql = "INSERT INTO mantenimientos (codigo_empresa, codigo_maq, tipo, actividades, refacciones, fecha_programada, tecnico_responsable, alerta_proxima) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoEmpresa);
            ps.setString(2, idMaq);
            ps.setString(3, tipo);
            ps.setString(4, act);
            ps.setString(5, ref);
            ps.setString(6, fecha);
            ps.setString(7, tecnico);
            ps.setString(8, alertaInicial);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean completarMantenimiento(int idMant, String codigoEmpresa) {
        String sql = "UPDATE mantenimientos SET alerta_proxima = 'Completado' WHERE id_mant = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMant);
            ps.setString(2, codigoEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, String>> listarMantenimientosPorEmpresa(String codigoEmpresa) {
        List<Map<String, String>> lista = new ArrayList<>();
        String sql = "SELECT * FROM mantenimientos WHERE codigo_empresa = ? ORDER BY id_mant DESC";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> mt = new HashMap<>();
                    String fechaProg = rs.getString("fecha_programada");
                    String alertaGuardada = rs.getString("alerta_proxima");
                    mt.put("idMant", String.valueOf(rs.getInt("id_mant")));
                    mt.put("idMaq", rs.getString("codigo_maq"));
                    mt.put("tipo", rs.getString("tipo"));
                    mt.put("act", rs.getString("actividades"));
                    mt.put("ref", rs.getString("refacciones"));
                    mt.put("fecha", fechaProg);
                    mt.put("tec", rs.getString("tecnico_responsable"));
                    mt.put("alerta", calcularNivelAlerta(fechaProg, alertaGuardada));
                    lista.add(mt);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}
