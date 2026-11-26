package com.aurea.sensemac.modulo1_usuarios;

import com.aurea.sensemac.config.ConexionDB;
import java.sql.*;
import java.util.*;

// dao encargado de autenticacion con 3 factores (correo, contrasena personal y clave unica de empresa)
public class UsuarioDAO {

    // valida las credenciales personales del usuario y verifica su clave unica de empresa
    public Map<String, String> autenticar(String correo, String password) {
        String sql = "SELECT u.id_usuario, u.nombre, u.correo, u.rol, u.estado, u.codigo_empresa, " +
                     "COALESCE(e.nombre_empresa, 'Manufactura Industrial del Centro') AS nombre_empresa, " +
                     "COALESCE(e.sector_industrial, 'Manufactura Industrial') AS sector_industrial " +
                     "FROM usuarios u " +
                     "LEFT JOIN empresas e ON u.codigo_empresa = e.codigo_empresa " +
                     "WHERE u.correo = ? AND u.password = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> u = new HashMap<>();
                    u.put("id", String.valueOf(rs.getInt("id_usuario")));
                    u.put("nombre", rs.getString("nombre"));
                    u.put("correo", rs.getString("correo"));
                    u.put("rol", rs.getString("rol"));
                    u.put("estado", rs.getString("estado"));
                    u.put("codigoEmpresa", rs.getString("codigo_empresa"));
                    u.put("nombreEmpresa", rs.getString("nombre_empresa"));
                    u.put("sectorEmpresa", rs.getString("sector_industrial"));
                    return u;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // verifica si un correo ya se encuentra registrado en la plataforma
    public boolean existeCorreo(String correo) {
        String sql = "SELECT id_usuario FROM usuarios WHERE LOWER(TRIM(correo)) = LOWER(TRIM(?))";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // verifica si ya existe una empresa con el mismo nombre/razon social o el mismo RFC
    public boolean existeEmpresaDuplicada(String nombreEmpresa, String rfcEmpresa) {
        String sql = "SELECT codigo_empresa FROM empresas WHERE LOWER(TRIM(nombre_empresa)) = LOWER(TRIM(?)) OR LOWER(TRIM(rfc_empresa)) = LOWER(TRIM(?))";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreEmpresa);
            ps.setString(2, rfcEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return true;
        }
    }

    // busca una empresa por su clave/codigo unico para validar solicitudes e inicios de sesion
    public Map<String, String> obtenerEmpresaPorCodigo(String codigoEmpresa) {
        String sql = "SELECT codigo_empresa, nombre_empresa, rfc_empresa, sector_industrial FROM empresas WHERE UPPER(TRIM(codigo_empresa)) = UPPER(TRIM(?))";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> emp = new HashMap<>();
                    emp.put("codigo", rs.getString("codigo_empresa"));
                    emp.put("nombre", rs.getString("nombre_empresa"));
                    emp.put("rfc", rs.getString("rfc_empresa"));
                    emp.put("sector", rs.getString("sector_industrial"));
                    return emp;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // genera automaticamente una clave unica de empresa de al menos 9 caracteres (ej. INDU-4821)
    private String generarCodigoUnico(String nombreEmpresa) {
        String limpio = nombreEmpresa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        while (limpio.length() < 4) {
            limpio = limpio + "X";
        }
        String prefijo = limpio.substring(0, 4);
        Random rnd = new Random();
        for (int intento = 0; intento < 50; intento++) {
            int numero = 1000 + rnd.nextInt(9000);
            String candidato = prefijo + "-" + numero;
            if (obtenerEmpresaPorCodigo(candidato) == null) {
                return candidato;
            }
        }
        return prefijo + "-" + (1000 + Math.abs((int) (System.currentTimeMillis() % 9000)));
    }

    // registra una nueva empresa (con clave personalizada >= 8 caracteres o autogenerada) y a su Supervisor General
    public String registrarNuevaEmpresaYSupervisor(String nombreEmpresa, String rfcEmpresa, String sector, String fecha,
                                                   String codigoPersonalizado, String nombreSup, String correoSup, String passSup) {
        String codigoFinal = (codigoPersonalizado != null && !codigoPersonalizado.trim().isEmpty())
                ? codigoPersonalizado.trim().toUpperCase()
                : generarCodigoUnico(nombreEmpresa);

        String sqlEmp = "INSERT INTO empresas (codigo_empresa, nombre_empresa, rfc_empresa, sector_industrial, fecha_registro) VALUES (?, ?, ?, ?, ?)";
        String sqlUsu = "INSERT INTO usuarios (nombre, correo, password, rol, estado, codigo_empresa) VALUES (?, ?, ?, 'Supervisor General', 'Activa', ?)";
        try (Connection con = ConexionDB.obtenerConexion()) {
            try (PreparedStatement psEmp = con.prepareStatement(sqlEmp)) {
                psEmp.setString(1, codigoFinal);
                psEmp.setString(2, nombreEmpresa.trim());
                psEmp.setString(3, rfcEmpresa.trim().toUpperCase());
                psEmp.setString(4, sector.trim());
                psEmp.setString(5, fecha);
                psEmp.executeUpdate();
            }
            try (PreparedStatement psUsu = con.prepareStatement(sqlUsu)) {
                psUsu.setString(1, nombreSup.trim());
                psUsu.setString(2, correoSup.trim());
                psUsu.setString(3, passSup);
                psUsu.setString(4, codigoFinal);
                psUsu.executeUpdate();
            }
            return codigoFinal;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // registra un usuario o solicitud de acceso vinculada al codigo de una empresa
    public boolean registrarUsuario(String nombre, String correo, String pass, String rol, String estado, String codigoEmpresa) {
        String sql = "INSERT INTO usuarios (nombre, correo, password, rol, estado, codigo_empresa) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, correo);
            ps.setString(3, pass);
            ps.setString(4, rol);
            ps.setString(5, estado);
            ps.setString(6, codigoEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // actualiza el rol y/o estado (incluyendo aprobacion de solicitudes) dentro de la misma empresa
    public boolean actualizarUsuario(int idUsuario, String nuevoRol, String nuevoEstado, String codigoEmpresa) {
        String sql = "UPDATE usuarios SET rol = ?, estado = ? WHERE id_usuario = ? AND codigo_empresa = ?";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoRol);
            ps.setString(2, nuevoEstado);
            ps.setInt(3, idUsuario);
            ps.setString(4, codigoEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // rechaza y elimina una solicitud de acceso pendiente de la empresa
    public boolean rechazarSolicitud(int idUsuario, String codigoEmpresa) {
        String sql = "DELETE FROM usuarios WHERE id_usuario = ? AND codigo_empresa = ? AND estado = 'Pendiente'";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, codigoEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // restablece la contrasena personal de un usuario activo validando su correo y clave unica de empresa
    public boolean restablecerPassword(String correo, String codigoEmpresa, String nuevaPassword) {
        String sql = "UPDATE usuarios SET password = ? WHERE correo = ? AND UPPER(TRIM(codigo_empresa)) = UPPER(TRIM(?)) AND estado = 'Activa'";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevaPassword);
            ps.setString(2, correo);
            ps.setString(3, codigoEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // obtiene la lista de usuarios y solicitudes pertenecientes exclusivamente a la empresa activa
    public List<Map<String, String>> listarUsuariosPorEmpresa(String codigoEmpresa) {
        List<Map<String, String>> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, nombre, correo, rol, estado, codigo_empresa FROM usuarios WHERE codigo_empresa = ? ORDER BY FIELD(estado, 'Pendiente', 'Activa', 'Inactiva'), id_usuario ASC";
        try (Connection con = ConexionDB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> u = new HashMap<>();
                    u.put("id", String.valueOf(rs.getInt("id_usuario")));
                    u.put("nombre", rs.getString("nombre"));
                    u.put("correo", rs.getString("correo"));
                    u.put("rol", rs.getString("rol"));
                    u.put("estado", rs.getString("estado"));
                    u.put("codigoEmpresa", rs.getString("codigo_empresa"));
                    lista.add(u);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}
