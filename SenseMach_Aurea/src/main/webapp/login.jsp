<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String mostrarPanel = (String) request.getAttribute("mostrarPanel");
    if (mostrarPanel == null) {
        mostrarPanel = "login";
    }
    String correoRegistrado = (String) request.getAttribute("correoRegistrado");
    if (correoRegistrado == null) {
        correoRegistrado = "";
    }
    String codigoEmpresaRegistrado = (String) request.getAttribute("codigoEmpresaRegistrado");
    if (codigoEmpresaRegistrado == null) {
        codigoEmpresaRegistrado = "";
    }
    String codigoGeneradoNuevo = (String) request.getAttribute("codigoGeneradoNuevo");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AUREA | SenseMach — Acceso y Registro Multiempresa</title>
    <link rel="icon" type="image/png" href="img/logo-aurea.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="css/aurea.css" rel="stylesheet">
</head>
<body class="d-flex align-items-center justify-content-center py-4" style="min-height: 100vh;">

<div class="card-aurea" style="max-width: 510px; width: 100%; padding: 2.1rem;">

    <div class="d-flex align-items-center mb-2">
        <div class="logo-badge-aurea me-3" style="width:46px;height:46px;border-radius:10px;">
            <img src="img/logo-aurea.png" alt="Logo Áurea">
        </div>
        <div>
            <h4 class="m-0 fw-bold" style="color:#263540;letter-spacing:0.8px;">AUREA | SENSEMACH</h4>
            <small class="text-muted">Plataforma Multiempresa de Monitoreo y Confiabilidad Industrial</small>
        </div>
    </div>
    <p class="small text-secondary mb-3"><i>"Eficiencia que se mide, calidad que se nota"</i></p>

    <div class="segmented-control-aurea mb-3" id="barraSelectorAcceso">
        <button type="button" id="btnTabLogin" class="segmented-btn-aurea active" onclick="cambiarModoAcceso('login')">
            Iniciar Sesión
        </button>
        <button type="button" id="btnTabRegistro" class="segmented-btn-aurea" onclick="cambiarModoAcceso('registro')">
            Crear Cuenta / Empresa
        </button>
    </div>

    <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-danger py-2 small fw-semibold mb-3"><%= request.getAttribute("error") %></div>
    <% } %>
    <% if (request.getAttribute("exito") != null) { %>
        <div class="alert alert-success py-2 small fw-semibold mb-3"><%= request.getAttribute("exito") %></div>
    <% } %>

    <% if (codigoGeneradoNuevo != null) { %>
        <div class="p-3 mb-3 rounded text-center" style="background-color:#F0FDF4;border:1px solid #86EFAC;">
            <div class="small text-secondary fw-bold">CLAVE ÚNICA DE SU EMPRESA</div>
            <div class="brand-font fw-bold my-1" style="font-size:1.45rem;color:#166534;letter-spacing:1.5px;" id="textoCodigoNuevo"><%= codigoGeneradoNuevo %></div>
            <small class="text-muted d-block mb-2" style="font-size:0.75rem;">Guarde esta clave y compártala con sus trabajadores para que puedan solicitar acceso e iniciar sesión en las bitácoras de su planta.</small>
            <button type="button" class="btn btn-outline-success btn-sm py-1 px-3 fw-semibold" onclick="navigator.clipboard.writeText('<%= codigoGeneradoNuevo %>');this.innerText='¡Clave Copiada!';">
                Copiar Clave Única de Empresa
            </button>
        </div>
    <% } %>

    <div id="panelLogin">
        <form action="usuarios" method="post">
            <input type="hidden" name="accion" value="login">
            <div class="mb-3">
                <label class="form-label small fw-bold mb-1" for="correoInput">1. Correo Corporativo</label>
                <input type="email" id="correoInput" name="correo" class="form-control" value="<%= correoRegistrado %>" placeholder="usuario@empresa.mx" autocomplete="username" required>
            </div>
            <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center">
                    <label class="form-label small fw-bold mb-1" for="passInput">2. Contraseña Personal</label>
                    <button type="button" class="btn btn-link btn-sm p-0 text-decoration-none small text-secondary" onclick="cambiarModoAcceso('recuperar')">
                        ¿Olvidó su contraseña?
                    </button>
                </div>
                <div class="input-group">
                    <input type="password" id="passInput" name="password" class="form-control" placeholder="Clave privada de su cuenta" autocomplete="current-password" required>
                    <button class="btn btn-outline-secondary btn-sm px-3" type="button" onclick="alternarVisibilidadPass('passInput')">
                        Ver
                    </button>
                </div>
                <small class="text-muted" style="font-size:0.72rem;">Clave exclusiva del titular de la cuenta.</small>
            </div>
            <div class="mb-3 p-2 rounded" style="background-color:#F8FAFC;border:1px solid #CBD5E1;">
                <label class="form-label small fw-bold mb-1" style="color:#263540;" for="codigoEmpresaLoginInput">
                    3. Clave Única de Empresa
                </label>
                <input type="text" id="codigoEmpresaLoginInput" name="codigoEmpresaLogin" class="form-control text-uppercase fw-bold" value="<%= codigoEmpresaRegistrado %>" placeholder="Ej. XXXX-0000" required>
                <small class="text-muted d-block mt-1" style="font-size:0.72rem;">
                    Identificador de la organización para acceder exclusivamente a sus máquinas y bitácoras.
                </small>
            </div>
            <button type="submit" class="btn-aurea w-100 py-2 mt-1">INGRESAR A LA EMPRESA</button>
        </form>
    </div>

    <div id="panelRegistro" style="display: none;">
        <form action="usuarios" method="post">
            <input type="hidden" name="accion" value="registroAutonomo">

            <div class="mb-2">
                <label class="form-label small fw-bold" for="selectRolRegistro">1. Seleccione su Rol / Tipo de Registro</label>
                <select id="selectRolRegistro" name="rolRegistro" class="form-select form-select-sm fw-semibold" onchange="alternarCamposSegunRol()">
                    <option value="Técnico de Diagnóstico y Soporte">Técnico de Diagnóstico y Soporte (Unirse a Empresa)</option>
                    <option value="Técnico de Línea B">Técnico de Línea B (Unirse a Empresa)</option>
                    <option value="Técnico Senior">Técnico Senior (Unirse a Empresa)</option>
                    <option value="Supervisor de Producción">Supervisor de Producción (Unirse a Empresa)</option>
                    <option value="Supervisor Financiero">Supervisor Financiero (Unirse a Empresa)</option>
                    <option value="Supervisor General">Supervisor General (Registrar Nueva Empresa)</option>
                </select>
            </div>

            <div id="bloqueUnirseEmpresa" class="p-2 mb-2 rounded" style="background-color:#F8FAFC; border:1px solid #CBD5E1;">
                <label class="form-label small fw-bold mb-1" style="color:#263540;" for="inputCodigoEmpresa">
                    Clave Única de Empresa
                </label>
                <input type="text" id="inputCodigoEmpresa" name="codigoEmpresaIngreso" class="form-control form-control-sm text-uppercase fw-bold" placeholder="Ej. XXXX-0000">
                <small class="text-muted d-block mt-1" style="font-size:0.73rem;">
                    Ingrese la clave de su organización para acceder a sus máquinas. Su solicitud requerirá <b>aprobación del Supervisor General</b>.
                </small>
            </div>

            <div id="bloqueNuevaEmpresa" class="p-2 mb-2 rounded" style="display: none; background-color:#F8FAFC; border:1px solid #263540;">
                <div class="small fw-bold mb-2" style="color:#263540;">Datos Oficiales de la Nueva Empresa</div>
                <div class="mb-2">
                    <label class="form-label small fw-semibold mb-1">Nombre o Razón Social de la Empresa (Único)</label>
                    <input type="text" id="inputNombreEmpresa" name="nombreEmpresa" class="form-control form-control-sm" placeholder="Ej. Industrias Metalúrgicas del Norte S.A.">
                </div>
                <div class="row g-2 mb-2">
                    <div class="col-sm-6">
                        <label class="form-label small fw-semibold mb-1">RFC / Registro Fiscal (Único)</label>
                        <input type="text" id="inputRfcEmpresa" name="rfcEmpresa" class="form-control form-control-sm text-uppercase" placeholder="Ej. IMN260512AB9">
                    </div>
                    <div class="col-sm-6">
                        <label class="form-label small fw-semibold mb-1">Sector Industrial</label>
                        <input type="text" id="inputSectorEmpresa" name="sectorEmpresa" class="form-control form-control-sm" placeholder="Ej. Automotriz / Metalurgia">
                    </div>
                </div>
                <div class="pt-1 border-top">
                    <label class="form-label small fw-bold mb-1" style="color:#263540;" for="inputClavePersonalizada">
                        Clave Única de Empresa (Opcional — mín. 8 caracteres)
                    </label>
                    <input type="text" id="inputClavePersonalizada" name="codigoEmpresaPersonalizado" class="form-control form-control-sm text-uppercase fw-bold" minlength="8" placeholder="Déjelo en blanco para generarla automáticamente">
                    <small class="text-muted d-block mt-1" style="font-size:0.71rem;">
                        Si lo deja vacío, el sistema le generará su Clave Única de Empresa automáticamente. Si escribe una propia, debe tener <b>mínimo 8 caracteres</b>.
                    </small>
                </div>
            </div>

            <div class="mb-2">
                <label class="form-label small fw-bold mb-1" for="nombreRegInput">Nombre Completo del Usuario</label>
                <input type="text" id="nombreRegInput" name="nombreRegistro" class="form-control form-control-sm" placeholder="Nombre y apellidos" required>
            </div>
            <div class="mb-2">
                <label class="form-label small fw-bold mb-1" for="correoRegInput">Correo Corporativo</label>
                <input type="email" id="correoRegInput" name="correoRegistro" class="form-control form-control-sm" placeholder="usuario@empresa.mx" required>
            </div>
            <div class="row g-2 mb-3">
                <div class="col-sm-6">
                    <label class="form-label small fw-bold mb-1" for="passRegInput">Contraseña Personal</label>
                    <div class="input-group input-group-sm">
                        <input type="password" id="passRegInput" name="passRegistro" class="form-control" minlength="6" placeholder="Mín. 6 caracteres" required>
                        <button class="btn btn-outline-secondary" type="button" onclick="alternarVisibilidadPass('passRegInput')">Ver</button>
                    </div>
                </div>
                <div class="col-sm-6">
                    <label class="form-label small fw-bold mb-1" for="confPassRegInput">Confirmar Contraseña</label>
                    <input type="password" id="confPassRegInput" name="confirmarPassRegistro" class="form-control form-control-sm" minlength="6" placeholder="Repita su contraseña" required>
                </div>
            </div>
            <button type="submit" id="btnSubmitRegistro" class="btn-aurea w-100 py-2">ENVIAR SOLICITUD DE ACCESO</button>
        </form>
    </div>

    <div id="panelRecuperacion" style="display: none;">
        <div class="border-top pt-2 mb-3">
            <h6 class="fw-bold mb-1" style="color:#263540;">Restablecimiento de Contraseña Personal</h6>
            <p class="small text-muted mb-0">Confirme su correo corporativo y la Clave Única de su Empresa para definir una nueva contraseña personal.</p>
        </div>
        <form action="usuarios" method="post">
            <input type="hidden" name="accion" value="recuperar">
            <div class="mb-2">
                <label class="form-label small fw-bold" for="correoRecInput">Correo Corporativo Registrado</label>
                <input type="email" id="correoRecInput" name="correoRecuperacion" class="form-control form-control-sm" placeholder="usuario@empresa.mx" required>
            </div>
            <div class="mb-2">
                <label class="form-label small fw-bold" for="codigoEmpRecInput">Clave Única de su Empresa</label>
                <input type="text" id="codigoEmpRecInput" name="codigoEmpresaRecuperacion" class="form-control form-control-sm text-uppercase fw-bold" placeholder="Ej. XXXX-0000" required>
            </div>
            <div class="mb-3">
                <label class="form-label small fw-bold" for="nuevaPassInput">Nueva Contraseña Personal</label>
                <div class="input-group input-group-sm">
                    <input type="password" id="nuevaPassInput" name="nuevaPassword" class="form-control" minlength="6" placeholder="Mínimo 6 caracteres" required>
                    <button class="btn btn-outline-secondary px-3" type="button" onclick="alternarVisibilidadPass('nuevaPassInput')">
                        Ver
                    </button>
                </div>
            </div>
            <div class="d-flex gap-2">
                <button type="button" class="btn btn-outline-secondary w-50 py-2 small fw-semibold" onclick="cambiarModoAcceso('login')">Volver</button>
                <button type="submit" class="btn-aurea w-50 py-2">ACTUALIZAR CLAVE</button>
            </div>
        </form>
    </div>

    <div class="mt-4 pt-3 border-top d-flex align-items-center justify-content-between">
        <small class="text-muted" style="font-size: 0.75rem;">Áurea Soluciones Tecnológicas</small>
        <small class="text-muted d-flex align-items-center" style="font-size: 0.75rem;">
            <span class="status-dot-online"></span>SenseMach Multiempresa
        </small>
    </div>
</div>

<script>
    function cambiarModoAcceso(modo) {
        const pLogin = document.getElementById('panelLogin');
        const pRegistro = document.getElementById('panelRegistro');
        const pRecuperar = document.getElementById('panelRecuperacion');
        const btnLogin = document.getElementById('btnTabLogin');
        const btnRegistro = document.getElementById('btnTabRegistro');
        const barraSelector = document.getElementById('barraSelectorAcceso');

        pLogin.style.display = (modo === 'login') ? 'block' : 'none';
        pRegistro.style.display = (modo === 'registro') ? 'block' : 'none';
        pRecuperar.style.display = (modo === 'recuperar') ? 'block' : 'none';

        if (modo === 'recuperar') {
            barraSelector.style.display = 'none';
        } else {
            barraSelector.style.display = 'flex';
            btnLogin.classList.toggle('active', modo === 'login');
            btnRegistro.classList.toggle('active', modo === 'registro');
        }
    }

    function alternarCamposSegunRol() {
        const rol = document.getElementById('selectRolRegistro').value;
        const esSupGeneral = (rol === 'Supervisor General');

        const bloqueUnirse = document.getElementById('bloqueUnirseEmpresa');
        const bloqueNueva = document.getElementById('bloqueNuevaEmpresa');
        const btnSubmit = document.getElementById('btnSubmitRegistro');

        const inputCodigo = document.getElementById('inputCodigoEmpresa');
        const inputNomEmp = document.getElementById('inputNombreEmpresa');
        const inputRfcEmp = document.getElementById('inputRfcEmpresa');
        const inputSecEmp = document.getElementById('inputSectorEmpresa');

        if (esSupGeneral) {
            bloqueUnirse.style.display = 'none';
            bloqueNueva.style.display = 'block';
            inputCodigo.required = false;
            inputNomEmp.required = true;
            inputRfcEmp.required = true;
            inputSecEmp.required = true;
            btnSubmit.innerText = 'REGISTRAR EMPRESA Y GENERAR CLAVE ÚNICA';
        } else {
            bloqueUnirse.style.display = 'block';
            bloqueNueva.style.display = 'none';
            inputCodigo.required = true;
            inputNomEmp.required = false;
            inputRfcEmp.required = false;
            inputSecEmp.required = false;
            btnSubmit.innerText = 'ENVIAR SOLICITUD DE ACCESO A EMPRESA';
        }
    }

    function alternarVisibilidadPass(inputId) {
        const input = document.getElementById(inputId);
        if (input) {
            input.type = input.type === 'password' ? 'text' : 'password';
        }
    }

    document.addEventListener('DOMContentLoaded', function() {
        cambiarModoAcceso('<%= mostrarPanel %>');
        alternarCamposSegunRol();
    });
</script>

</body>
</html>
