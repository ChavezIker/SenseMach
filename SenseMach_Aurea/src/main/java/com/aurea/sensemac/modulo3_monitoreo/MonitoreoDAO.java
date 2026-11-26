package com.aurea.sensemac.modulo3_monitoreo;

import com.aurea.sensemac.config.ConexionDB;
import java.sql.*;
import java.util.*;

// dao encargado de las lecturas instrumentales en tiempo real y bitacora operativa por empresa
public class MonitoreoDAO {

    public void registrarLectura(String codigoEmpresa, String idMaq, double temp, double pres, double vib, String obs, String responsable, String fecha, String estado) {
        String sql = "INSERT INTO monitoreo_bitacora (codigo_empresa, codigo_maq, temperatura, presion, vibracion, observaciones, responsable, fecha_hora, estado_detectado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoEmpresa);
            ps.setString(2, idMaq);
            ps.setDouble(3, temp);
            ps.setDouble(4, pres);
            ps.setDouble(5, vib);
            ps.setString(6, obs);
            ps.setString(7, responsable);
            ps.setString(8, fecha);
            ps.setString(9, estado);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, String>> listarBitacoraPorEmpresa(String codigoEmpresa) {
        List<Map<String, String>> lista = new ArrayList<>();
        String sql = "SELECT * FROM monitoreo_bitacora WHERE codigo_empresa = ? ORDER BY id_lectura DESC LIMIT 25";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> b = new HashMap<>();
                    b.put("idLectura", String.valueOf(rs.getInt("id_lectura")));
                    b.put("idMaq", rs.getString("codigo_maq"));
                    b.put("temp", String.format(Locale.US, "%.1f", rs.getDouble("temperatura")));
                    b.put("pres", String.format(Locale.US, "%.1f", rs.getDouble("presion")));
                    b.put("vib", String.format(Locale.US, "%.1f", rs.getDouble("vibracion")));
                    b.put("obs", rs.getString("observaciones"));
                    b.put("resp", rs.getString("responsable"));
                    b.put("fecha", rs.getString("fecha_hora"));
                    b.put("estado", rs.getString("estado_detectado"));
                    lista.add(b);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}
