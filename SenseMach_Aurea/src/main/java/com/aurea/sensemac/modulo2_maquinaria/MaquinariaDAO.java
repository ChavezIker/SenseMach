package com.aurea.sensemac.modulo2_maquinaria;

import com.aurea.sensemac.config.ConexionDB;
import java.sql.*;
import java.util.*;

// dao encargado del catalogo de maquinaria industrial, aislado por codigo unico de empresa
public class MaquinariaDAO {

    public boolean existeId(String codigoMaq, String codigoEmpresa) {
        String sql = "SELECT codigo_maq FROM maquinaria WHERE codigo_maq = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoMaq);
            ps.setString(2, codigoEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean registrarMaquina(String codigoEmpresa, String id, String nombre, String modelo, int anio, int horas, String fallas, String estado, String fecha) {
        String sql = "INSERT INTO maquinaria (codigo_empresa, codigo_maq, nombre, modelo, anio, horas_uso, fallas_previas, costo_adquisicion, costo_mantenimiento, estado_operativo, ultimo_registro) VALUES (?, ?, ?, ?, ?, ?, ?, 0.00, 0.00, ?, ?)";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String nombreFormateado = nombre.startsWith(id) ? nombre : (id + " - " + nombre);
            ps.setString(1, codigoEmpresa);
            ps.setString(2, id);
            ps.setString(3, nombreFormateado);
            ps.setString(4, modelo);
            ps.setInt(5, anio);
            ps.setInt(6, horas);
            ps.setString(7, fallas == null || fallas.trim().isEmpty() ? "Ninguna" : fallas.trim());
            ps.setString(8, estado);
            ps.setString(9, fecha);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarOperacionMaquina(String codigoEmpresa, String idMaq, int horasUso, String fallasPrevias, String estadoOperativo, String fecha) {
        String sql = "UPDATE maquinaria SET horas_uso = ?, fallas_previas = ?, estado_operativo = ?, ultimo_registro = ? WHERE codigo_maq = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, horasUso);
            ps.setString(2, fallasPrevias == null || fallasPrevias.trim().isEmpty() ? "Ninguna" : fallasPrevias.trim());
            ps.setString(3, estadoOperativo);
            ps.setString(4, fecha);
            ps.setString(5, idMaq);
            ps.setString(6, codigoEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarCostos(String codigoEmpresa, String idMaq, double costoAdq, double costoMant, String fecha) {
        String sql = "UPDATE maquinaria SET costo_adquisicion = ?, costo_mantenimiento = ?, ultimo_registro = ? WHERE codigo_maq = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, costoAdq);
            ps.setDouble(2, costoMant);
            ps.setString(3, fecha);
            ps.setString(4, idMaq);
            ps.setString(5, codigoEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void actualizarEstado(String codigoEmpresa, String idMaq, String nuevoEstado, String fecha) {
        String sql = "UPDATE maquinaria SET estado_operativo = ?, ultimo_registro = ? WHERE codigo_maq = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, fecha);
            ps.setString(3, idMaq);
            ps.setString(4, codigoEmpresa);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registrarAnomalia(String codigoEmpresa, String idMaq, String detalleAnomalia, String fecha) {
        String sql = "UPDATE maquinaria SET estado_operativo = 'Alerta', fallas_previas = ?, ultimo_registro = ? WHERE codigo_maq = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, detalleAnomalia);
            ps.setString(2, fecha);
            ps.setString(3, idMaq);
            ps.setString(4, codigoEmpresa);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void incrementarHorasUso(String codigoEmpresa, String idMaq, int horasAdicionales, String fecha) {
        if (horasAdicionales <= 0) return;
        String sql = "UPDATE maquinaria SET horas_uso = horas_uso + ?, ultimo_registro = ? WHERE codigo_maq = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, horasAdicionales);
            ps.setString(2, fecha);
            ps.setString(3, idMaq);
            ps.setString(4, codigoEmpresa);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sumarCostoMantenimiento(String codigoEmpresa, String idMaq, double montoAdicional, String fecha) {
        if (montoAdicional <= 0) return;
        String sql = "UPDATE maquinaria SET costo_mantenimiento = costo_mantenimiento + ?, ultimo_registro = ? WHERE codigo_maq = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, montoAdicional);
            ps.setString(2, fecha);
            ps.setString(3, idMaq);
            ps.setString(4, codigoEmpresa);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean eliminarMaquina(String codigoEmpresa, String idMaq) {
        try (Connection con = ConexionDB.obtenerConexion()) {
            try (PreparedStatement ps1 = con.prepareStatement("DELETE FROM monitoreo_bitacora WHERE codigo_maq = ? AND codigo_empresa = ?")) {
                ps1.setString(1, idMaq);
                ps1.setString(2, codigoEmpresa);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM mantenimientos WHERE codigo_maq = ? AND codigo_empresa = ?")) {
                ps2.setString(1, idMaq);
                ps2.setString(2, codigoEmpresa);
                ps2.executeUpdate();
            }
            try (PreparedStatement ps3 = con.prepareStatement("DELETE FROM maquinaria WHERE codigo_maq = ? AND codigo_empresa = ?")) {
                ps3.setString(1, idMaq);
                ps3.setString(2, codigoEmpresa);
                return ps3.executeUpdate() > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, String>> listarMaquinariaPorEmpresa(String codigoEmpresa) {
        List<Map<String, String>> lista = new ArrayList<>();
        String sql = "SELECT * FROM maquinaria WHERE codigo_empresa = ? ORDER BY codigo_maq ASC";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> m = new HashMap<>();
                    m.put("id", rs.getString("codigo_maq"));
                    m.put("nombre", rs.getString("nombre"));
                    m.put("modelo", rs.getString("modelo"));
                    m.put("anio", String.valueOf(rs.getInt("anio")));
                    m.put("horas", String.valueOf(rs.getInt("horas_uso")));
                    m.put("fallas", rs.getString("fallas_previas"));
                    m.put("costoAdq", String.format(Locale.US, "%.2f", rs.getDouble("costo_adquisicion")));
                    m.put("costoMant", String.format(Locale.US, "%.2f", rs.getDouble("costo_mantenimiento")));
                    m.put("estado", rs.getString("estado_operativo"));
                    m.put("fecha", rs.getString("ultimo_registro"));
                    lista.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}
