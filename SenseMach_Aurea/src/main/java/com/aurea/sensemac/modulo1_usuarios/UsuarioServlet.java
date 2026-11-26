package com.aurea.sensemac.modulo1_usuarios;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

// controlador para autenticacion de 3 datos (correo, contrasena personal y clave unica de empresa)
@WebServlet(name = "UsuarioServlet", urlPatterns = {"/usuarios"})
public class UsuarioServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // verifica si el texto ingresado es el formato de ejemplo del placeholder para bloquear su uso
    private boolean esClaveEjemploProhibida(String clave) {
        if (clave == null) return false;
        String c = clave.trim().toUpperCase();
        return "XXXX-0000".equals(c) || "EJ. XXXX-0000".equals(c) || "XXXX0000".equals(c) || "0000-0000".equals(c);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().invalidate();
        resp.sendRedirect("login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");
        HttpSession sesion = req.getSession();

        if ("login".equals(accion)) {
            String correo = req.getParameter("correo") != null ? req.getParameter("correo").trim() : "";
            String pass = req.getParameter("password") != null ? req.getParameter("password") : "";
            String claveEmpresaInput = req.getParameter("codigoEmpresaLogin") != null
                    ? req.getParameter("codigoEmpresaLogin").trim().toUpperCase() : "";

            if (correo.isEmpty() || pass.isEmpty() || claveEmpresaInput.isEmpty()) {
                req.setAttribute("error", "Debe ingresar los tres datos requeridos: Correo, Contraseña Personal y Clave Única de Empresa.");
                req.setAttribute("correoRegistrado", correo);
                req.setAttribute("codigoEmpresaRegistrado", claveEmpresaInput);
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            if (esClaveEjemploProhibida(claveEmpresaInput)) {
                req.setAttribute("error", "Acceso denegado: «" + claveEmpresaInput + "» es únicamente un formato de ejemplo y no corresponde a ninguna empresa.");
                req.setAttribute("correoRegistrado", correo);
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            Map<String, String> usuario = usuarioDAO.autenticar(correo, pass);

            if (usuario == null) {
                req.setAttribute("error", "Correo corporativo o contraseña personal incorrectos.");
                req.setAttribute("correoRegistrado", correo);
                req.setAttribute("codigoEmpresaRegistrado", claveEmpresaInput);
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            // Verifica que la Clave Unica de Empresa coincida exactamente con la empresa de la cuenta
            String claveEmpresaReal = usuario.get("codigoEmpresa") != null ? usuario.get("codigoEmpresa").trim() : "";
            if (!claveEmpresaInput.equalsIgnoreCase(claveEmpresaReal)) {
                req.setAttribute("error", "Acceso denegado: La Clave Única de Empresa «" + claveEmpresaInput + "» no corresponde a la organización de esta cuenta.");
                req.setAttribute("correoRegistrado", correo);
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            String estadoCuenta = usuario.get("estado");
            if ("Pendiente".equalsIgnoreCase(estadoCuenta)) {
                req.setAttribute("error", "Su solicitud para ingresar a «" + usuario.get("nombreEmpresa") + "» está pendiente de aprobación por el Supervisor General.");
                req.setAttribute("correoRegistrado", correo);
                req.setAttribute("codigoEmpresaRegistrado", claveEmpresaInput);
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }
            if (!"Activa".equalsIgnoreCase(estadoCuenta)) {
                req.setAttribute("error", "Cuenta inactiva. Contacte al Supervisor General de su empresa.");
                req.setAttribute("correoRegistrado", correo);
                req.setAttribute("codigoEmpresaRegistrado", claveEmpresaInput);
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            sesion.setAttribute("usuarioId", usuario.get("id"));
            sesion.setAttribute("usuarioNombre", usuario.get("nombre"));
            sesion.setAttribute("usuarioCorreo", usuario.get("correo"));
            sesion.setAttribute("usuarioRol", usuario.get("rol"));
            sesion.setAttribute("codigoEmpresa", usuario.get("codigoEmpresa"));
            sesion.setAttribute("nombreEmpresa", usuario.get("nombreEmpresa"));
            sesion.setAttribute("sectorEmpresa", usuario.get("sectorEmpresa"));
            resp.sendRedirect("dashboard?tab=resumen");
            return;
        }

        if ("recuperar".equals(accion)) {
            String correo = req.getParameter("correoRecuperacion") != null ? req.getParameter("correoRecuperacion").trim() : "";
            String claveEmpresa = req.getParameter("codigoEmpresaRecuperacion") != null
                    ? req.getParameter("codigoEmpresaRecuperacion").trim().toUpperCase() : "";
            String nuevaPass = req.getParameter("nuevaPassword") != null ? req.getParameter("nuevaPassword").trim() : "";

            if (correo.isEmpty() || claveEmpresa.isEmpty() || nuevaPass.length() < 6) {
                req.setAttribute("error", "Ingrese su correo corporativo, la Clave Única de su Empresa y una nueva contraseña personal de al menos 6 caracteres.");
                req.setAttribute("mostrarPanel", "recuperar");
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            boolean actualizado = usuarioDAO.restablecerPassword(correo, claveEmpresa, nuevaPass);
            if (actualizado) {
                req.setAttribute("exito", "Contraseña personal restablecida correctamente para " + correo + ". Ya puede iniciar sesión.");
                req.setAttribute("correoRegistrado", correo);
                req.setAttribute("codigoEmpresaRegistrado", claveEmpresa);
            } else {
                req.setAttribute("error", "No se encontró una cuenta activa con ese correo y Clave Única de Empresa.");
                req.setAttribute("mostrarPanel", "recuperar");
            }
            req.getRequestDispatcher("login.jsp").forward(req, resp);
            return;
        }

        if ("registroAutonomo".equals(accion)) {
            String nombre = req.getParameter("nombreRegistro") != null ? req.getParameter("nombreRegistro").trim() : "";
            String correo = req.getParameter("correoRegistro") != null ? req.getParameter("correoRegistro").trim() : "";
            String pass = req.getParameter("passRegistro") != null ? req.getParameter("passRegistro").trim() : "";
            String confirmar = req.getParameter("confirmarPassRegistro") != null ? req.getParameter("confirmarPassRegistro").trim() : "";
            String rolElegido = req.getParameter("rolRegistro") != null ? req.getParameter("rolRegistro").trim() : "Técnico de Diagnóstico y Soporte";

            if (nombre.isEmpty() || correo.isEmpty() || pass.length() < 6) {
                req.setAttribute("error", "Complete todos los datos personales y asegúrese de que su contraseña personal tenga al menos 6 caracteres.");
                req.setAttribute("mostrarPanel", "registro");
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            if (!pass.equals(confirmar)) {
                req.setAttribute("error", "Las contraseñas personales ingresadas no coinciden. Verifique e intente nuevamente.");
                req.setAttribute("mostrarPanel", "registro");
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            if (usuarioDAO.existeCorreo(correo)) {
                req.setAttribute("error", "El correo corporativo «" + correo + "» ya se encuentra registrado en el sistema.");
                req.setAttribute("mostrarPanel", "registro");
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            // CASO A: Registro como Supervisor General -> Crea una nueva empresa (con clave opcional >= 8 caracteres o autogenerada)
            if ("Supervisor General".equalsIgnoreCase(rolElegido)) {
                String nombreEmpresa = req.getParameter("nombreEmpresa") != null ? req.getParameter("nombreEmpresa").trim() : "";
                String rfcEmpresa = req.getParameter("rfcEmpresa") != null ? req.getParameter("rfcEmpresa").trim().toUpperCase() : "";
                String sectorEmpresa = req.getParameter("sectorEmpresa") != null ? req.getParameter("sectorEmpresa").trim() : "";
                String clavePersonalizada = req.getParameter("codigoEmpresaPersonalizado") != null
                        ? req.getParameter("codigoEmpresaPersonalizado").trim().toUpperCase() : "";

                if (nombreEmpresa.isEmpty() || rfcEmpresa.isEmpty() || sectorEmpresa.isEmpty()) {
                    req.setAttribute("error", "Para registrarse como Supervisor General debe ingresar Nombre/Razón Social, RFC y Sector Industrial de su empresa.");
                    req.setAttribute("mostrarPanel", "registro");
                    req.getRequestDispatcher("login.jsp").forward(req, resp);
                    return;
                }

                // Si el Supervisor General escribio una clave personalizada, valida que tenga minimo 8 caracteres, no sea el ejemplo y no exista
                if (!clavePersonalizada.isEmpty()) {
                    if (esClaveEjemploProhibida(clavePersonalizada)) {
                        req.setAttribute("error", "No puede utilizar «" + clavePersonalizada + "» como Clave Única de Empresa ya que es un formato de ejemplo reservado.");
                        req.setAttribute("mostrarPanel", "registro");
                        req.getRequestDispatcher("login.jsp").forward(req, resp);
                        return;
                    }
                    if (clavePersonalizada.length() < 8) {
                        req.setAttribute("error", "La Clave Única de Empresa personalizada debe tener mínimo 8 caracteres (o deje el campo en blanco para generarla automáticamente).");
                        req.setAttribute("mostrarPanel", "registro");
                        req.getRequestDispatcher("login.jsp").forward(req, resp);
                        return;
                    }
                    if (usuarioDAO.obtenerEmpresaPorCodigo(clavePersonalizada) != null) {
                        req.setAttribute("error", "Acceso denegado: La Clave Única de Empresa «" + clavePersonalizada + "» ya está en uso por otra organización.");
                        req.setAttribute("mostrarPanel", "registro");
                        req.getRequestDispatcher("login.jsp").forward(req, resp);
                        return;
                    }
                }

                if (usuarioDAO.existeEmpresaDuplicada(nombreEmpresa, rfcEmpresa)) {
                    req.setAttribute("error", "Acceso denegado: Ya existe una empresa registrada con esa Razón Social o RFC. Dos empresas no pueden compartir los mismos datos.");
                    req.setAttribute("mostrarPanel", "registro");
                    req.getRequestDispatcher("login.jsp").forward(req, resp);
                    return;
                }

                String fechaHoy = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "MX")).format(new Date());
                String codigoFinal = usuarioDAO.registrarNuevaEmpresaYSupervisor(
                        nombreEmpresa, rfcEmpresa, sectorEmpresa, fechaHoy, clavePersonalizada, nombre, correo, pass
                );

                if (codigoFinal != null) {
                    req.setAttribute("exito", "Empresa «" + nombreEmpresa + "» registrada con éxito. Su Clave Única de Empresa es: " + codigoFinal + ". Ya puede iniciar sesión.");
                    req.setAttribute("codigoGeneradoNuevo", codigoFinal);
                    req.setAttribute("correoRegistrado", correo);
                    req.setAttribute("codigoEmpresaRegistrado", codigoFinal);
                } else {
                    req.setAttribute("error", "No fue posible registrar la empresa. Verifique los datos e intente nuevamente.");
                    req.setAttribute("mostrarPanel", "registro");
                }
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            // CASO B: Cualquier otro rol -> Solicita acceso mediante la Clave Unica de una empresa existente
            String codigoEmpresaInput = req.getParameter("codigoEmpresaIngreso") != null
                    ? req.getParameter("codigoEmpresaIngreso").trim().toUpperCase() : "";

            if (codigoEmpresaInput.isEmpty() || esClaveEjemploProhibida(codigoEmpresaInput)) {
                req.setAttribute("error", "Ingrese una Clave Única de Empresa válida (el formato de ejemplo «XXXX-0000» no es válido).");
                req.setAttribute("mostrarPanel", "registro");
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            Map<String, String> empresaDestino = usuarioDAO.obtenerEmpresaPorCodigo(codigoEmpresaInput);
            if (empresaDestino == null) {
                req.setAttribute("error", "La Clave Única de Empresa «" + codigoEmpresaInput + "» no existe. Solicite la clave correcta a su Supervisor General.");
                req.setAttribute("mostrarPanel", "registro");
                req.getRequestDispatcher("login.jsp").forward(req, resp);
                return;
            }

            boolean enviado = usuarioDAO.registrarUsuario(nombre, correo, pass, rolElegido, "Pendiente", empresaDestino.get("codigo"));
            if (enviado) {
                req.setAttribute("exito", "Solicitud enviada a «" + empresaDestino.get("nombre") + "» (" + empresaDestino.get("codigo") + ") con el rol de " + rolElegido + ". Podrá iniciar sesión en cuanto su Supervisor General la apruebe.");
                req.setAttribute("correoRegistrado", correo);
                req.setAttribute("codigoEmpresaRegistrado", empresaDestino.get("codigo"));
            } else {
                req.setAttribute("error", "No se pudo procesar su solicitud de registro.");
                req.setAttribute("mostrarPanel", "registro");
            }
            req.getRequestDispatcher("login.jsp").forward(req, resp);
            return;
        }

        // Acciones internas del Dashboard
        String codigoEmpresaSesion = (String) sesion.getAttribute("codigoEmpresa");
        if (codigoEmpresaSesion == null || codigoEmpresaSesion.isEmpty()) {
            codigoEmpresaSesion = "AUREA-2026";
        }

        if ("registrar".equals(accion)) {
            String nombre = req.getParameter("nombre") != null ? req.getParameter("nombre").trim() : "";
            String correo = req.getParameter("correo") != null ? req.getParameter("correo").trim() : "";
            String pass = req.getParameter("pass") != null ? req.getParameter("pass").trim() : "";
            String rol = req.getParameter("rol");
            String estado = req.getParameter("estado");

            if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty()) {
                sesion.setAttribute("alertaError", "Complete todos los campos obligatorios para dar de alta al colaborador.");
                resp.sendRedirect("dashboard?tab=usuarios");
                return;
            }

            boolean ok = usuarioDAO.registrarUsuario(nombre, correo, pass, rol, estado, codigoEmpresaSesion);
            if (ok) {
                sesion.setAttribute("alertaExito", "Colaborador registrado directamente en su empresa con el perfil: " + rol + ".");
            } else {
                sesion.setAttribute("alertaError", "No se pudo registrar la cuenta. Verifique que el correo corporativo no esté duplicado.");
            }
            resp.sendRedirect("dashboard?tab=usuarios");
            return;
        }

        if ("aprobarSolicitud".equals(accion)) {
            try {
                int idUsuario = Integer.parseInt(req.getParameter("idUsuario"));
                String rolAprobado = req.getParameter("rol");
                boolean ok = usuarioDAO.actualizarUsuario(idUsuario, rolAprobado, "Activa", codigoEmpresaSesion);
                if (ok) {
                    sesion.setAttribute("alertaExito", "Solicitud aprobada. El colaborador ya tiene acceso a las máquinas de su empresa con el rol: " + rolAprobado + ".");
                } else {
                    sesion.setAttribute("alertaError", "No fue posible aprobar la solicitud seleccionada.");
                }
            } catch (Exception e) {
                sesion.setAttribute("alertaError", "Error al procesar la aprobación.");
            }
            resp.sendRedirect("dashboard?tab=usuarios");
            return;
        }

        if ("rechazarSolicitud".equals(accion)) {
            try {
                int idUsuario = Integer.parseInt(req.getParameter("idUsuario"));
                boolean ok = usuarioDAO.rechazarSolicitud(idUsuario, codigoEmpresaSesion);
                if (ok) {
                    sesion.setAttribute("alertaExito", "Solicitud de acceso rechazada y eliminada.");
                } else {
                    sesion.setAttribute("alertaError", "No fue posible rechazar la solicitud.");
                }
            } catch (Exception e) {
                sesion.setAttribute("alertaError", "Error al rechazar la solicitud.");
            }
            resp.sendRedirect("dashboard?tab=usuarios");
            return;
        }

        if ("actualizar".equals(accion)) {
            try {
                int idUsuario = Integer.parseInt(req.getParameter("idUsuario"));
                String nuevoRol = req.getParameter("rol");
                String nuevoEstado = req.getParameter("estado");
                String idSesionActual = (String) sesion.getAttribute("usuarioId");

                if (idSesionActual != null && idSesionActual.equals(String.valueOf(idUsuario)) && !"Activa".equalsIgnoreCase(nuevoEstado)) {
                    sesion.setAttribute("alertaError", "No puede desactivar su propia cuenta mientras tiene una sesión activa.");
                    resp.sendRedirect("dashboard?tab=usuarios");
                    return;
                }

                boolean ok = usuarioDAO.actualizarUsuario(idUsuario, nuevoRol, nuevoEstado, codigoEmpresaSesion);
                if (ok) {
                    if (idSesionActual != null && idSesionActual.equals(String.valueOf(idUsuario))) {
                        sesion.setAttribute("usuarioRol", nuevoRol);
                    }
                    sesion.setAttribute("alertaExito", "Permisos y estado de la cuenta actualizados correctamente.");
                } else {
                    sesion.setAttribute("alertaError", "No fue posible actualizar los datos de la cuenta seleccionada.");
                }
            } catch (Exception e) {
                sesion.setAttribute("alertaError", "Solicitud inválida al actualizar el usuario.");
            }
            resp.sendRedirect("dashboard?tab=usuarios");
        }
    }
}
