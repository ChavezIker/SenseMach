<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%
    String usuarioId = (String) session.getAttribute("usuarioId");
    String usuarioNombre = (String) session.getAttribute("usuarioNombre");
    String usuarioRol = (String) session.getAttribute("usuarioRol");
    String codigoEmpresa = (String) session.getAttribute("codigoEmpresa");
    String nombreEmpresa = (String) session.getAttribute("nombreEmpresa");
    String sectorEmpresa = (String) session.getAttribute("sectorEmpresa");

    if (usuarioRol == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    if (codigoEmpresa == null || codigoEmpresa.isEmpty()) {
        codigoEmpresa = "AUREA-2026";
    }
    if (nombreEmpresa == null || nombreEmpresa.isEmpty()) {
        nombreEmpresa = "Manufactura Industrial del Centro";
    }
    if (sectorEmpresa == null || sectorEmpresa.isEmpty()) {
        sectorEmpresa = "Manufactura Industrial";
    }

    List<Map<String, String>> usuarios = (List<Map<String, String>>) request.getAttribute("usuarios");
    List<Map<String, String>> maquinaria = (List<Map<String, String>>) request.getAttribute("maquinaria");
    List<Map<String, String>> bitacora = (List<Map<String, String>>) request.getAttribute("bitacora");
    List<Map<String, String>> mantenimientos = (List<Map<String, String>>) request.getAttribute("mantenimientos");

    if (maquinaria == null) {
        response.sendRedirect("dashboard");
        return;
    }

    boolean esSupGeneral = "Supervisor General".equals(usuarioRol);
    boolean esSupProduccion = "Supervisor de Producción".equals(usuarioRol);
    boolean esSupFinanciero = "Supervisor Financiero".equals(usuarioRol);
    boolean esTecSenior = "Técnico Senior".equals(usuarioRol);
    boolean esTecLineaB = "Técnico de Línea B".equals(usuarioRol);
    boolean esTecDiagnostico = "Técnico de Diagnóstico y Soporte".equals(usuarioRol);

    boolean puedeMonitorear = esSupGeneral || esSupProduccion || esTecSenior || esTecLineaB || esTecDiagnostico;
    boolean puedeMantenimiento = esSupGeneral || esTecSenior || esTecLineaB || esTecDiagnostico;
    boolean puedeMaquinaria = esSupGeneral || esSupProduccion || esSupFinanciero;
    boolean puedeReportes = esSupGeneral || esSupProduccion || esSupFinanciero;

    // Separa las solicitudes pendientes de aprobacion del directorio activo de la empresa
    List<Map<String, String>> solicitudesPendientes = new ArrayList<>();
    List<Map<String, String>> personalEmpresa = new ArrayList<>();
    if (usuarios != null) {
        for (Map<String, String> u : usuarios) {
            if ("Pendiente".equalsIgnoreCase(u.get("estado"))) {
                solicitudesPendientes.add(u);
            } else {
                personalEmpresa.add(u);
            }
        }
    }

    // Mapa con la ultima lectura de telemetria por cada maquina de la empresa
    Map<String, Map<String, String>> ultimaLecturaPorMaq = new HashMap<>();
    if (bitacora != null) {
        for (Map<String, String> b : bitacora) {
            String codigo = b.get("idMaq");
            if (codigo != null && !ultimaLecturaPorMaq.containsKey(codigo)) {
                ultimaLecturaPorMaq.put(codigo, b);
            }
        }
    }

    // Calculo dinamico de indicadores operativos y financieros de la empresa activa
    int totalEquipos = maquinaria.size();
    int equiposOptimos = 0;
    int equiposAlerta = 0;
    int equiposEnMant = 0;
    int equiposInactivos = 0;
    int totalHorasPlanta = 0;
    double totalCostoAdq = 0.0;
    double totalCostoMant = 0.0;

    for (Map<String, String> m : maquinaria) {
        String est = m.get("estado") != null ? m.get("estado") : "";
        if ("Óptimo".equalsIgnoreCase(est) || "Activa".equalsIgnoreCase(est)) {
            equiposOptimos++;
        } else if ("Alerta".equalsIgnoreCase(est)) {
            equiposAlerta++;
        } else if ("En Mantenimiento".equalsIgnoreCase(est)) {
            equiposEnMant++;
        } else {
            equiposInactivos++;
        }
        try { totalHorasPlanta += Integer.parseInt(m.get("horas")); } catch (Exception ignored) {}
        try { totalCostoAdq += Double.parseDouble(m.get("costoAdq")); } catch (Exception ignored) {}
        try { totalCostoMant += Double.parseDouble(m.get("costoMant")); } catch (Exception ignored) {}
    }

    double eficienciaCalculada = totalEquipos > 0
            ? ((equiposOptimos * 100.0) + (equiposEnMant * 65.0) + (equiposAlerta * 35.0)) / totalEquipos
            : 100.0;
    String eficienciaTexto = String.format(Locale.US, "%.1f", eficienciaCalculada);
    int barraEficiencia = (int) Math.round(eficienciaCalculada);

    int ordenesPendientes = 0;
    int ordenesUrgentes = 0;
    int ordenesCompletadas = 0;
    if (mantenimientos != null) {
        for (Map<String, String> mt : mantenimientos) {
            String al = mt.get("alerta") != null ? mt.get("alerta") : "";
            if ("Completado".equalsIgnoreCase(al)) {
                ordenesCompletadas++;
            } else {
                ordenesPendientes++;
                if ("Vencido".equalsIgnoreCase(al) || al.contains("Próximo") || "Urgente".equalsIgnoreCase(al)) {
                    ordenesUrgentes++;
                }
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= nombreEmpresa %> | SenseMach</title>
    <link rel="icon" type="image/png" href="img/logo-aurea.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="css/aurea.css" rel="stylesheet">
</head>
<body>

<!-- BARRA SUPERIOR CON NOMBRE LIMPIO DE LA EMPRESA ACTIVA (SIN MOSTRAR CLAVE ÚNICA) -->
<header class="topbar-aurea">
    <div class="d-flex align-items-center">
        <div class="logo-badge-aurea me-2">
            <img src="img/logo-aurea.png" alt="Logo">
        </div>
        <div class="me-3">
            <div class="d-flex align-items-center">
                <span class="brand-font fw-bold" style="color:#263540;font-size:1.08rem;letter-spacing:0.4px;"><%= nombreEmpresa %></span>
            </div>
            <div class="text-muted" style="font-size:0.74rem;">
                SenseMach &middot; <span id="tituloSeccionActiva" class="fw-semibold" style="color:#263540;">Panel General</span>
            </div>
        </div>
    </div>

    <div class="d-flex align-items-center gap-3">
        <% if (esSupGeneral && !solicitudesPendientes.isEmpty()) { %>
            <button type="button" class="badge-alerta border-0 d-none d-md-inline-flex align-items-center gap-1" style="cursor:pointer;" onclick="seleccionarPestana('usuarios')" title="Aprobar solicitudes de acceso a su empresa">
                <span>&#128100; <%= solicitudesPendientes.size() %> <%= solicitudesPendientes.size() == 1 ? "Solicitud Pendiente" : "Solicitudes Pendientes" %></span>
            </button>
        <% } %>

        <% if (equiposAlerta > 0) { %>
            <button type="button" class="badge-alerta border-0 d-none d-lg-inline-flex align-items-center gap-1" style="cursor:pointer;" onclick="seleccionarPestana('<%= puedeMonitorear ? "monitoreo" : "resumen" %>')" title="Ver equipos con alerta operativa">
                <span>&#9888; <%= equiposAlerta %> <%= equiposAlerta == 1 ? "Equipo en Alerta" : "Equipos en Alerta" %></span>
            </button>
        <% } else { %>
            <span class="badge-optimo d-none d-lg-inline-flex align-items-center">
                &#10003; Planta en Rango Nominal
            </span>
        <% } %>

        <div class="text-end border-start ps-3">
            <div class="fw-bold small" style="color:#263540;"><%= usuarioNombre %></div>
            <span class="badge" style="background:#263540;color:#fff;font-size:0.7rem;"><%= usuarioRol %></span>
        </div>
        <a href="usuarios" class="btn btn-outline-secondary btn-sm fw-semibold">Salir</a>
    </div>
</header>

<div class="layout-wrapper">
    <!-- BARRA LATERAL ORGANIZADA POR CATEGORÍAS OPERATIVAS -->
    <aside id="sidebarAurea" class="sidebar-aurea">
        <div class="sidebar-header">
            <span class="sidebar-title-text">Navegación</span>
            <button type="button" class="btn-toggle-sidebar" onclick="alternarSidebar()" title="Minimizar / Expandir menú lateral">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                    <line x1="9" y1="3" x2="9" y2="21"></line>
                </svg>
            </button>
        </div>

        <div class="sidebar-section-label">Operación en Planta</div>

        <!-- PANEL GENERAL -->
        <button type="button" class="nav-item-aurea" data-tab="resumen" data-title="Panel General" onclick="seleccionarPestana('resumen')" title="Panel General de Operaciones">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            <span class="nav-label">Panel General</span>
        </button>

        <!-- MONITOREO EN PLANTA -->
        <% if (puedeMonitorear) { %>
        <button type="button" class="nav-item-aurea" data-tab="monitoreo" data-title="Monitoreo en Planta" onclick="seleccionarPestana('monitoreo')" title="Monitoreo de Parámetros en Tiempo Real">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
            </svg>
            <span class="nav-label">Monitoreo en Planta</span>
            <span class="nav-badge-count"><%= totalEquipos %></span>
        </button>
        <% } %>

        <!-- MANTENIMIENTO -->
        <% if (puedeMantenimiento) { %>
        <button type="button" class="nav-item-aurea" data-tab="mantenimiento" data-title="Gestión de Mantenimiento" onclick="seleccionarPestana('mantenimiento')" title="Órdenes de Mantenimiento y Alertas">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
            </svg>
            <span class="nav-label">Mantenimiento</span>
            <% if (ordenesPendientes > 0) { %>
                <span class="nav-badge-count"><%= ordenesPendientes %></span>
            <% } %>
        </button>
        <% } %>

        <% if (puedeMaquinaria || puedeReportes || esSupGeneral) { %>
        <div class="sidebar-section-label mt-2">Gestión y Estrategia</div>
        <% } %>

        <!-- CATÁLOGO DE MAQUINARIA Y COSTOS -->
        <% if (puedeMaquinaria) { %>
        <button type="button" class="nav-item-aurea" data-tab="maquinaria" data-title="Catálogo de Maquinaria" onclick="seleccionarPestana('maquinaria')" title="Catálogo de Maquinaria y Costos">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <ellipse cx="12" cy="5" rx="9" ry="3"></ellipse>
                <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path>
                <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path>
            </svg>
            <span class="nav-label">Catálogo de Maquinaria</span>
        </button>
        <% } %>

        <!-- REPORTES EJECUTIVOS -->
        <% if (puedeReportes) { %>
        <button type="button" class="nav-item-aurea" data-tab="reportes" data-title="Reportes Ejecutivos" onclick="seleccionarPestana('reportes')" title="Reportes Financieros y de Estado">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
            </svg>
            <span class="nav-label">Reportes Ejecutivos</span>
        </button>
        <% } %>

        <!-- PERSONAL Y ACCESOS -->
        <% if (esSupGeneral) { %>
        <button type="button" class="nav-item-aurea" data-tab="usuarios" data-title="Personal y Accesos" onclick="seleccionarPestana('usuarios')" title="Aprobación de Solicitudes y Personal de la Empresa">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            <span class="nav-label">Personal y Accesos</span>
            <% if (!solicitudesPendientes.isEmpty()) { %>
                <span class="nav-badge-count" style="background-color:#FEF3C7;color:#92400E;"><%= solicitudesPendientes.size() %></span>
            <% } %>
        </button>
        <% } %>

        <div class="mt-auto pt-3 border-top sidebar-footer-info">
            <div class="d-flex align-items-center gap-2">
                <div class="logo-badge-aurea" style="width:26px;height:26px;border-radius:6px;">
                    <img src="img/logo-aurea.png" alt="A">
                </div>
                <div style="overflow:hidden;">
                    <div class="small fw-bold text-truncate" style="color:#263540;line-height:1.1;"><%= nombreEmpresa %></div>
                    <div class="text-muted d-flex align-items-center" style="font-size:0.71rem;">
                        <span class="status-dot-online"></span>Planta en Línea
                    </div>
                </div>
            </div>
        </div>
    </aside>

    <!-- ÁREA DE CONTENIDO PRINCIPAL -->
    <main class="main-content-aurea">

        <% if (request.getAttribute("alertaExito") != null) { %>
            <div class="alert alert-success py-2 small fw-semibold mb-3 no-print"><%= request.getAttribute("alertaExito") %></div>
        <% } %>
        <% if (request.getAttribute("alertaError") != null) { %>
            <div class="alert alert-danger py-2 small fw-semibold mb-3 no-print"><%= request.getAttribute("alertaError") %></div>
        <% } %>

        <% if (esSupGeneral && !solicitudesPendientes.isEmpty()) { %>
            <div class="p-3 mb-3 rounded d-flex flex-wrap justify-content-between align-items-center gap-2 no-print" style="background-color:#FFFBEB;border:1px solid #FDE68A;">
                <div>
                    <span class="fw-bold small" style="color:#92400E;">&#128100; Solicitudes de Acceso a su Empresa:</span>
                    <span class="small text-dark ms-1">
                        Hay <b><%= solicitudesPendientes.size() %></b> colaborador(es) esperando su aprobación para acceder a las máquinas de <b><%= nombreEmpresa %></b>.
                    </span>
                </div>
                <button type="button" class="btn btn-warning btn-sm fw-bold py-1 px-3" style="font-size:0.78rem;" onclick="seleccionarPestana('usuarios')">
                    Revisar y Aprobar Solicitudes
                </button>
            </div>
        <% } %>

        <!-- ==================== PESTAÑA: PANEL GENERAL ==================== -->
        <section id="tab-resumen" class="tab-pane-aurea">
            <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                <div>
                    <h5 class="fw-bold m-0" style="color:#263540;">PANEL OPERATIVO — <%= nombreEmpresa.toUpperCase() %></h5>
                    <small class="text-secondary">
                        Sector: <%= sectorEmpresa %> &middot; Supervisión de confiabilidad y estado de maquinaria de la organización
                    </small>
                </div>
                <div class="d-flex gap-2">
                    <% if (puedeMaquinaria && totalEquipos == 0) { %>
                        <button type="button" class="btn-aurea btn-sm" onclick="seleccionarPestana('maquinaria')">+ Registrar Primera Máquina</button>
                    <% } %>
                    <% if (puedeMonitorear && totalEquipos > 0) { %>
                        <button type="button" class="btn btn-outline-dark btn-sm fw-semibold" onclick="seleccionarPestana('monitoreo')">+ Capturar Lectura</button>
                    <% } %>
                    <% if (puedeMantenimiento && totalEquipos > 0) { %>
                        <button type="button" class="btn-aurea btn-sm" onclick="seleccionarPestana('mantenimiento')">Órdenes de Trabajo (<%= ordenesPendientes %>)</button>
                    <% } %>
                </div>
            </div>

            <!-- KPIs INDUSTRIALES EN TIEMPO REAL -->
            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <div class="card-aurea h-100 d-flex align-items-center justify-content-between">
                        <div>
                            <div class="text-secondary fw-bold small mb-1">ÍNDICE DE CONFIABILIDAD (OEE)</div>
                            <div class="d-flex align-items-center gap-3">
                                <span class="brand-font fw-bold" style="font-size: 2.1rem; color:#263540;"><%= eficienciaTexto %>%</span>
                                <div style="width:105px;height:8px;background:#D1D5DB;border-radius:4px;overflow:hidden;">
                                    <div style="width:<%= barraEficiencia %>%;height:100%;background:<%= barraEficiencia >= 80 ? "#263540" : "#D97706" %>;"></div>
                                </div>
                            </div>
                            <small class="text-muted d-block mt-1">
                                <%= equiposOptimos %> de <%= totalEquipos %> unidades en operación nominal
                            </small>
                        </div>
                        <svg width="115" height="48" viewBox="0 0 150 52" class="d-none d-xl-block">
                            <path d="M5,42 L30,30 L55,36 L85,18 L115,25 L145,10 L145,48 L5,48 Z" fill="#E2E8F0"/>
                            <polyline fill="none" stroke="#263540" stroke-width="2.5" points="5,42 30,30 55,36 85,18 115,25 145,10"/>
                        </svg>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="card-aurea h-100 d-flex align-items-center justify-content-between">
                        <div>
                            <div class="text-secondary fw-bold small mb-1">SEGURIDAD Y DIAGNÓSTICO</div>
                            <% if (equiposAlerta == 0) { %>
                                <div class="brand-font fw-bold" style="font-size: 1.2rem; color:#166534;">SEGURO / SIN INCIDENTES</div>
                                <small class="text-muted">Temperatura, presión y vibración bajo norma</small>
                            <% } else { %>
                                <div class="brand-font fw-bold" style="font-size: 1.2rem; color:#92400E;">ATENCIÓN REQUERIDA (<%= equiposAlerta %>)</div>
                                <small class="text-muted"><%= equiposAlerta %> <%= equiposAlerta == 1 ? "equipo presenta" : "equipos presentan" %> anomalía física</small>
                            <% } %>
                        </div>
                        <% if (equiposAlerta == 0) { %>
                            <div style="width:44px;height:44px;border-radius:50%;background:#DCFCE7;color:#166534;display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:1.25rem;flex-shrink:0;">&#10003;</div>
                        <% } else { %>
                            <div style="width:44px;height:44px;border-radius:50%;background:#FEF3C7;color:#92400E;display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:1.25rem;flex-shrink:0;">!</div>
                        <% } %>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="card-aurea h-100 d-flex justify-content-between align-items-center">
                        <div>
                            <div class="text-secondary fw-bold small mb-1">CUMPLIMIENTO DE MANTENIMIENTO</div>
                            <div class="d-flex align-items-baseline gap-2">
                                <span class="brand-font fw-bold" style="font-size: 1.85rem; color:#263540;"><%= ordenesPendientes %></span>
                                <span class="small text-muted">activas / <%= ordenesCompletadas %> cerradas</span>
                            </div>
                            <small class="<%= ordenesUrgentes > 0 ? "text-danger fw-semibold" : "text-muted" %>">
                                <%= ordenesUrgentes > 0 ? (ordenesUrgentes + " con prioridad inmediata (< 48h)") : "Agenda preventiva al día" %>
                            </small>
                        </div>
                        <div class="text-end">
                            <span class="badge-programado"><%= String.format(Locale.US, "%,d", totalHorasPlanta) %> h uso</span>
                        </div>
                    </div>
                </div>
            </div>

            <% if (totalEquipos == 0) { %>
                <div class="card-aurea text-center py-5">
                    <h6 class="fw-bold" style="color:#263540;">Planta de «<%= nombreEmpresa %>» lista para configurar</h6>
                    <p class="small text-secondary mb-3">
                        Su entorno empresarial es privado e independiente. Registre la primera máquina de su planta para iniciar el monitoreo en tiempo real.
                    </p>
                    <% if (puedeMaquinaria) { %>
                        <button type="button" class="btn-aurea px-4" onclick="seleccionarPestana('maquinaria')">Ir a Catálogo de Maquinaria</button>
                    <% } %>
                </div>
            <% } else { %>
                <!-- BARRA DE HERRAMIENTAS DE PLANTA -->
                <div class="card-aurea mb-3 py-2 px-3">
                    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
                        <div class="d-flex flex-wrap align-items-center gap-2">
                            <span class="small fw-bold text-secondary me-1">Filtrar Planta:</span>
                            <button type="button" class="filter-pill-aurea active" data-est="" onclick="seleccionarFiltroEstadoResumen('', this)">Todos (<%= totalEquipos %>)</button>
                            <button type="button" class="filter-pill-aurea" data-est="Óptimo" onclick="seleccionarFiltroEstadoResumen('Óptimo', this)">Óptimos (<%= equiposOptimos %>)</button>
                            <button type="button" class="filter-pill-aurea" data-est="Alerta" onclick="seleccionarFiltroEstadoResumen('Alerta', this)">En Alerta (<%= equiposAlerta %>)</button>
                            <button type="button" class="filter-pill-aurea" data-est="En Mantenimiento" onclick="seleccionarFiltroEstadoResumen('En Mantenimiento', this)">En Mantenimiento (<%= equiposEnMant %>)</button>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <input type="text" id="buscadorResumen" class="form-control form-control-sm" style="width: 200px;" placeholder="Buscar código o máquina..." onkeyup="filtrarPlantaGeneral()">
                            <div class="segmented-control-aurea">
                                <button type="button" id="btnVistaTarjetas" class="segmented-btn-aurea active py-1 px-2" onclick="cambiarVistaPlanta('tarjetas')">Tarjetas</button>
                                <button type="button" id="btnVistaTabla" class="segmented-btn-aurea py-1 px-2" onclick="cambiarVistaPlanta('tabla')">Tabla</button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- VISTA 1: TARJETAS DE MONITOREO DE ACTIVOS -->
                <div class="row g-3 mb-3" id="contenedorTarjetasPlanta">
                    <% for (Map<String, String> m : maquinaria) {
                        String idM = m.get("id");
                        String est = m.get("estado") != null ? m.get("estado") : "Óptimo";
                        String claseBadge = "Óptimo".equalsIgnoreCase(est) || "Activa".equalsIgnoreCase(est) ? "badge-optimo" :
                                            ("Alerta".equalsIgnoreCase(est) || "En Mantenimiento".equalsIgnoreCase(est) ? "badge-alerta" : "badge-inactiva");
                        String claseBorde = "Óptimo".equalsIgnoreCase(est) || "Activa".equalsIgnoreCase(est) ? "status-border-optimo" :
                                            ("Alerta".equalsIgnoreCase(est) || "En Mantenimiento".equalsIgnoreCase(est) ? "status-border-alerta" : "status-border-inactiva");

                        Map<String, String> ultLect = ultimaLecturaPorMaq.get(idM);
                        String tempStr = ultLect != null ? ultLect.get("temp") : "64.0";
                        String presStr = ultLect != null ? ultLect.get("pres") : "112.0";
                        String vibStr  = ultLect != null ? ultLect.get("vib")  : "2.2";

                        double tempVal = 64.0, presVal = 112.0, vibVal = 2.2;
                        try { tempVal = Double.parseDouble(tempStr); } catch (Exception ignored) {}
                        try { presVal = Double.parseDouble(presStr); } catch (Exception ignored) {}
                        try { vibVal  = Double.parseDouble(vibStr);  } catch (Exception ignored) {}

                        int pctTemp = Math.min(100, (int) Math.round((tempVal / 100.0) * 100));
                        int pctPres = Math.min(100, (int) Math.round((presVal / 160.0) * 100));
                        int pctVib  = Math.min(100, (int) Math.round((vibVal / 10.0) * 100));

                        int horasUso = 0;
                        try { horasUso = Integer.parseInt(m.get("horas")); } catch (Exception ignored) {}
                        int horasEnCiclo = horasUso % 500;
                        int horasRestantesPreventivo = 500 - horasEnCiclo;
                        int pctCiclo = (int) Math.round((horasEnCiclo / 500.0) * 100);
                    %>
                    <div class="col-md-6 col-xl-4 item-planta-filtrable" data-estado="<%= est %>" data-texto="<%= (idM + " " + m.get("nombre") + " " + m.get("modelo") + " " + m.get("fallas")).toLowerCase() %>">
                        <div class="asset-card-aurea <%= claseBorde %>">
                            <div>
                                <div class="d-flex justify-content-between align-items-start mb-2">
                                    <div>
                                        <span class="badge bg-light text-dark border fw-bold me-1"><%= idM %></span>
                                        <small class="text-muted"><%= m.get("modelo") %> (<%= m.get("anio") %>)</small>
                                        <h6 class="fw-bold mt-1 mb-0" style="color:#263540;"><%= m.get("nombre") %></h6>
                                    </div>
                                    <span class="<%= claseBadge %>"><%= est %></span>
                                </div>

                                <div class="row g-2 my-2">
                                    <div class="col-4">
                                        <div class="telemetry-metric-box">
                                            <div class="d-flex justify-content-between align-items-center">
                                                <small class="text-muted" style="font-size:0.68rem;">TEMP</small>
                                                <span class="fw-bold" style="font-size:0.78rem;color:<%= tempVal > 80.0 ? "#DC2626" : "#263540" %>;"><%= tempStr %>°C</span>
                                            </div>
                                            <div class="telemetry-bar-track">
                                                <div class="telemetry-bar-fill" style="width:<%= pctTemp %>%;background:<%= tempVal > 80.0 ? "#DC2626" : "#263540" %>;"></div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="col-4">
                                        <div class="telemetry-metric-box">
                                            <div class="d-flex justify-content-between align-items-center">
                                                <small class="text-muted" style="font-size:0.68rem;">PRES</small>
                                                <span class="fw-bold" style="font-size:0.78rem;color:<%= (presVal < 60.0 || presVal > 150.0) ? "#DC2626" : "#263540" %>;"><%= presStr %> psi</span>
                                            </div>
                                            <div class="telemetry-bar-track">
                                                <div class="telemetry-bar-fill" style="width:<%= pctPres %>%;background:<%= (presVal < 60.0 || presVal > 150.0) ? "#DC2626" : "#263540" %>;"></div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="col-4">
                                        <div class="telemetry-metric-box">
                                            <div class="d-flex justify-content-between align-items-center">
                                                <small class="text-muted" style="font-size:0.68rem;">VIBR</small>
                                                <span class="fw-bold" style="font-size:0.78rem;color:<%= vibVal > 6.5 ? "#DC2626" : "#263540" %>;"><%= vibStr %> mm/s</span>
                                            </div>
                                            <div class="telemetry-bar-track">
                                                <div class="telemetry-bar-fill" style="width:<%= pctVib %>%;background:<%= vibVal > 6.5 ? "#DC2626" : "#263540" %>;"></div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="mt-2 pt-2 border-top">
                                    <div class="d-flex justify-content-between small mb-1" style="font-size:0.75rem;">
                                        <span class="text-secondary">Horómetro: <b><%= horasUso %> h</b></span>
                                        <span class="text-muted">Próx. servicio en <b><%= horasRestantesPreventivo %> h</b></span>
                                    </div>
                                    <div class="telemetry-bar-track">
                                        <div class="telemetry-bar-fill" style="width:<%= pctCiclo %>%;background:<%= pctCiclo > 85 ? "#D97706" : "#64748B" %>;"></div>
                                    </div>
                                    <div class="d-flex justify-content-between align-items-center mt-2" style="font-size:0.74rem;">
                                        <span class="text-muted">Diagnóstico: <b><%= m.get("fallas") %></b></span>
                                        <span class="text-muted"><%= m.get("fecha") %></span>
                                    </div>
                                </div>
                            </div>

                            <div class="d-flex gap-2 mt-3 pt-2 border-top">
                                <% if (puedeMonitorear) { %>
                                    <button type="button" class="btn btn-outline-dark btn-sm flex-fill py-1 fw-semibold" style="font-size:0.76rem;" onclick="irAMonitoreoConEquipo('<%= idM %>')">
                                        Inspeccionar
                                    </button>
                                <% } %>
                                <% if (puedeMantenimiento) { %>
                                    <button type="button" class="btn-aurea btn-sm flex-fill py-1" style="font-size:0.76rem;" onclick="irAMantenimientoConEquipo('<%= idM %>')">
                                        Orden de Trabajo
                                    </button>
                                <% } %>
                                <% if (!puedeMonitorear && !puedeMantenimiento && puedeReportes) { %>
                                    <button type="button" class="btn btn-outline-dark btn-sm flex-fill py-1 fw-semibold" style="font-size:0.76rem;" onclick="seleccionarPestana('reportes')">
                                        Ver Costos ($<%= m.get("costoMant") %>)
                                    </button>
                                <% } %>
                            </div>
                        </div>
                    </div>
                    <% } %>
                </div>

                <!-- VISTA 2: TABLA DETALLADA DE MAQUINARIA EN PLANTA -->
                <div class="card-aurea" id="contenedorTablaPlanta" style="display: none;">
                    <h6 class="fw-bold mb-3" style="color:#263540;">LISTADO TÉCNICO DE MAQUINARIA — <%= nombreEmpresa.toUpperCase() %></h6>
                    <div class="table-responsive">
                        <table class="table table-sm align-middle small mb-0" id="tablaResumenMaquinaria">
                            <thead class="table-light">
                                <tr>
                                    <th>Código</th>
                                    <th>Equipo Industrial</th>
                                    <th>Modelo / Año</th>
                                    <th>Horómetro</th>
                                    <th>Diagnóstico / Antecedente</th>
                                    <th>Última Actualización</th>
                                    <th>Estado</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (Map<String, String> m : maquinaria) {
                                    String idM = m.get("id");
                                    String est = m.get("estado");
                                    String claseBadge = "Óptimo".equalsIgnoreCase(est) || "Activa".equalsIgnoreCase(est) ? "badge-optimo" :
                                                        ("Alerta".equalsIgnoreCase(est) || "En Mantenimiento".equalsIgnoreCase(est) ? "badge-alerta" : "badge-inactiva");
                                %>
                                <tr class="item-planta-filtrable-tabla" data-estado="<%= est %>" data-texto="<%= (idM + " " + m.get("nombre") + " " + m.get("modelo") + " " + m.get("fallas")).toLowerCase() %>">
                                    <td class="fw-bold"><%= idM %></td>
                                    <td><%= m.get("nombre") %></td>
                                    <td><%= m.get("modelo") %> (<%= m.get("anio") %>)</td>
                                    <td><%= m.get("horas") %> h</td>
                                    <td><%= m.get("fallas") %></td>
                                    <td><%= m.get("fecha") %></td>
                                    <td><span class="<%= claseBadge %>"><%= est %></span></td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
            <% } %>
        </section>

        <!-- ==================== PESTAÑA: MONITOREO EN PLANTA ==================== -->
        <% if (puedeMonitorear) { %>
        <section id="tab-monitoreo" class="tab-pane-aurea">
            <div class="mb-3">
                <h5 class="fw-bold m-0" style="color:#263540;">MONITOREO INSTRUMENTAL EN PLANTA — <%= nombreEmpresa.toUpperCase() %></h5>
                <small class="text-secondary">Evaluación en tiempo real de parámetros físicos, horómetro de turno y bitácora exclusiva de su organización</small>
            </div>

            <div class="row g-3">
                <div class="col-lg-4">
                    <div class="card-aurea h-100">
                        <h6 class="fw-bold mb-2" style="color:#263540;">CAPTURA DE LECTURA OPERATIVA</h6>

                        <div class="p-2 mb-3 rounded" style="background-color:#F8FAFC;border:1px solid #E2E8F0;font-size:0.78rem;">
                            <div class="d-flex justify-content-between">
                                <span class="text-muted">Modelo: <b id="infoMonModelo" class="text-dark">-</b></span>
                                <span class="text-muted">Horómetro: <b id="infoMonHoras" class="text-dark">-</b></span>
                            </div>
                            <div class="mt-1 text-muted">Condición actual: <b id="infoMonFallas" class="text-dark">-</b></div>
                        </div>

                        <form action="monitoreo" method="post">
                            <div class="mb-2">
                                <label class="form-label small fw-semibold">Equipo a Inspeccionar</label>
                                <select id="selectEquipoMonitoreo" name="idMaq" class="form-select form-select-sm" onchange="actualizarFichaMonitoreo()" required>
                                    <% for (Map<String, String> m : maquinaria) { %>
                                        <option value="<%= m.get("id") %>"
                                                data-modelo="<%= m.get("modelo") %> (<%= m.get("anio") %>)"
                                                data-horas="<%= m.get("horas") %> h"
                                                data-fallas="<%= m.get("fallas") %>">
                                            <%= m.get("nombre") %> [<%= m.get("estado") %>]
                                        </option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="row g-2 mb-2">
                                <div class="col-6">
                                    <label class="form-label small fw-semibold mb-1">Temperatura (°C)</label>
                                    <input type="number" step="0.1" id="inputMonTemp" name="temperatura" class="form-control form-control-sm" placeholder="Máx 80 °C" oninput="evaluarUmbralesEnVivo()" required>
                                </div>
                                <div class="col-6">
                                    <label class="form-label small fw-semibold mb-1">Presión (psi)</label>
                                    <input type="number" step="0.1" id="inputMonPres" name="presion" class="form-control form-control-sm" placeholder="60 – 150 psi" oninput="evaluarUmbralesEnVivo()" required>
                                </div>
                            </div>
                            <div class="row g-2 mb-2">
                                <div class="col-6">
                                    <label class="form-label small fw-semibold mb-1">Vibración (mm/s)</label>
                                    <input type="number" step="0.1" id="inputMonVib" name="vibracion" class="form-control form-control-sm" placeholder="Máx 6.5 mm/s" oninput="evaluarUmbralesEnVivo()" required>
                                </div>
                                <div class="col-6">
                                    <label class="form-label small fw-semibold mb-1">Horas del Turno (+h)</label>
                                    <input type="number" min="0" max="24" name="horasTurno" class="form-control form-control-sm" value="0">
                                </div>
                            </div>

                            <div id="bannerDiagnosticoVivo" class="p-2 mb-2 rounded small fw-semibold" style="background-color:#F1F5F9;color:#475569;font-size:0.76rem;">
                                Ingrese las lecturas instrumentales para previsualizar el dictamen automático.
                            </div>

                            <div class="mb-3">
                                <label class="form-label small fw-semibold">Observaciones en Bitácora</label>
                                <input type="text" name="observaciones" class="form-control form-control-sm" placeholder="Notas técnicas de la inspección">
                            </div>
                            <button type="submit" class="btn-aurea w-100" <%= totalEquipos == 0 ? "disabled" : "" %>>GUARDAR LECTURA EN BITÁCORA</button>
                        </form>
                    </div>
                </div>

                <div class="col-lg-8">
                    <div class="card-aurea h-100">
                        <h6 class="fw-bold mb-3" style="color:#263540;">ESTADO DE CONFIABILIDAD POR UNIDAD</h6>
                        <div class="table-responsive">
                            <table class="table table-sm align-middle small">
                                <thead class="table-light">
                                    <tr>
                                        <th>Código</th>
                                        <th>Equipo</th>
                                        <th>Horómetro</th>
                                        <th>Diagnóstico Reciente</th>
                                        <th>Último Registro</th>
                                        <th>Estado</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% for (Map<String, String> m : maquinaria) {
                                        String est = m.get("estado");
                                        String claseBadge = "Óptimo".equalsIgnoreCase(est) || "Activa".equalsIgnoreCase(est) ? "badge-optimo" :
                                                            ("Alerta".equalsIgnoreCase(est) || "En Mantenimiento".equalsIgnoreCase(est) ? "badge-alerta" : "badge-inactiva");
                                    %>
                                    <tr>
                                        <td class="fw-bold"><%= m.get("id") %></td>
                                        <td><%= m.get("nombre") %></td>
                                        <td><%= m.get("horas") %> h</td>
                                        <td><%= m.get("fallas") %></td>
                                        <td><%= m.get("fecha") %></td>
                                        <td><span class="<%= claseBadge %>"><%= est %></span></td>
                                    </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>

                        <div class="d-flex justify-content-between align-items-center mt-3 mb-2">
                            <h6 class="fw-bold m-0 small text-secondary">HISTORIAL DE TELEMETRÍA EN TIEMPO REAL</h6>
                            <select id="filtroBitacoraEquipo" class="form-select form-select-sm" style="width: 190px;" onchange="filtrarBitacora()">
                                <option value="">Todos los equipos</option>
                                <% for (Map<String, String> m : maquinaria) { %>
                                    <option value="<%= m.get("id") %>"><%= m.get("nombre") %></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-sm align-middle small mb-0" id="tablaBitacora">
                                <thead class="table-light">
                                    <tr>
                                        <th>Equipo</th>
                                        <th>Temp.</th>
                                        <th>Presión</th>
                                        <th>Vibración</th>
                                        <th>Observaciones</th>
                                        <th>Fecha / Hora</th>
                                        <th>Dictamen</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (bitacora != null) {
                                        for (Map<String, String> b : bitacora) {
                                            String estBit = b.get("estado");
                                            String badgeBit = "Óptimo".equalsIgnoreCase(estBit) ? "badge-optimo" : "badge-alerta";
                                    %>
                                    <tr data-equipo="<%= b.get("idMaq") %>">
                                        <td class="fw-bold"><%= b.get("idMaq") %></td>
                                        <td><%= b.get("temp") %> °C</td>
                                        <td><%= b.get("pres") %> psi</td>
                                        <td><%= b.get("vib") %> mm/s</td>
                                        <td><%= b.get("obs") %></td>
                                        <td class="text-muted"><%= b.get("fecha") %></td>
                                        <td><span class="<%= badgeBit %>"><%= estBit %></span></td>
                                    </tr>
                                    <%  }
                                       } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </section>
        <% } %>

        <!-- ==================== PESTAÑA: CATÁLOGO DE MAQUINARIA Y COSTOS ==================== -->
        <% if (puedeMaquinaria) { %>
        <section id="tab-maquinaria" class="tab-pane-aurea">
            <div class="mb-3">
                <h5 class="fw-bold m-0" style="color:#263540;">CATÁLOGO DE MAQUINARIA — <%= nombreEmpresa.toUpperCase() %></h5>
                <small class="text-secondary">Administración técnica de activos industriales, control de horómetro, estados operativos y costos</small>
            </div>

            <% if (esSupProduccion || esSupGeneral) { %>
            <div class="row g-3 mb-4">
                <div class="col-lg-7">
                    <div class="card-aurea h-100">
                        <h6 class="fw-bold mb-3" style="color:#263540;">ALTA DE NUEVA MAQUINARIA EN SU EMPRESA</h6>
                        <form action="maquinaria" method="post" class="row g-2">
                            <input type="hidden" name="accion" value="registrarMaquina">
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold">Código ID</label>
                                <input type="text" name="id" class="form-control form-control-sm" placeholder="Ej. PR-405" required>
                            </div>
                            <div class="col-md-5">
                                <label class="form-label small fw-semibold">Nombre del Equipo</label>
                                <input type="text" name="nombre" class="form-control form-control-sm" placeholder="Prensa Hidráulica" required>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label small fw-semibold">Modelo</label>
                                <input type="text" name="modelo" class="form-control form-control-sm" placeholder="PH-900" required>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold">Año</label>
                                <input type="number" name="anio" class="form-control form-control-sm" value="2025" min="1990" max="2035" required>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label small fw-semibold">Horas de Uso</label>
                                <input type="number" name="horas" class="form-control form-control-sm" value="0" min="0" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-semibold">Estado Operativo Inicial</label>
                                <select name="estado" class="form-select form-select-sm">
                                    <option value="Óptimo">Activa / Óptimo</option>
                                    <option value="En Mantenimiento">En Mantenimiento</option>
                                    <option value="Inactiva">Inactiva</option>
                                </select>
                            </div>
                            <div class="col-md-8">
                                <label class="form-label small fw-semibold">Antecedente de Fallas</label>
                                <input type="text" name="fallas" class="form-control form-control-sm" value="Ninguna" required>
                            </div>
                            <div class="col-md-4 d-flex align-items-end">
                                <button type="submit" class="btn-aurea w-100">REGISTRAR EQUIPO</button>
                            </div>
                        </form>
                    </div>
                </div>

                <div class="col-lg-5">
                    <div class="card-aurea h-100">
                        <h6 class="fw-bold mb-3" style="color:#263540;">ACTUALIZAR ESTADO Y HORÓMETRO</h6>
                        <form action="maquinaria" method="post" class="row g-2">
                            <input type="hidden" name="accion" value="actualizarOperacion">
                            <div class="col-md-7">
                                <label class="form-label small fw-semibold">Equipo Registrado</label>
                                <select id="selectMaqOperacion" name="idMaq" class="form-select form-select-sm" onchange="cargarDatosMaquinaOperacion()">
                                    <% for (Map<String, String> m : maquinaria) { %>
                                        <option value="<%= m.get("id") %>"
                                                data-horas="<%= m.get("horas") %>"
                                                data-estado="<%= m.get("estado") %>"
                                                data-fallas="<%= m.get("fallas") %>">
                                            <%= m.get("nombre") %>
                                        </option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="col-md-5">
                                <label class="form-label small fw-semibold">Horas Acumuladas</label>
                                <input type="number" id="inputHorasOperacion" name="horas" class="form-control form-control-sm" min="0" required>
                            </div>
                            <div class="col-md-12">
                                <label class="form-label small fw-semibold">Estado Operativo</label>
                                <select id="selectEstadoOperacion" name="estado" class="form-select form-select-sm">
                                    <option value="Óptimo">Activa / Óptimo</option>
                                    <option value="Alerta">En Alerta Operativa</option>
                                    <option value="En Mantenimiento">En Mantenimiento</option>
                                    <option value="Inactiva">Inactiva / Fuera de Línea</option>
                                </select>
                            </div>
                            <div class="col-md-8">
                                <label class="form-label small fw-semibold">Historial / Fallas Registradas</label>
                                <input type="text" id="inputFallasOperacion" name="fallas" class="form-control form-control-sm" required>
                            </div>
                            <div class="col-md-4 d-flex align-items-end">
                                <button type="submit" class="btn-aurea w-100" <%= totalEquipos == 0 ? "disabled" : "" %>>GUARDAR</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
            <% } %>

            <% if (esSupFinanciero || esSupGeneral) { %>
            <div class="card-aurea mb-4">
                <h6 class="fw-bold mb-3" style="color:#263540;">ACTUALIZACIÓN DE COSTOS DE ADQUISICIÓN Y MANTENIMIENTO</h6>
                <form action="maquinaria" method="post" class="row g-3 align-items-end">
                    <input type="hidden" name="accion" value="actualizarCostos">
                    <div class="col-md-4">
                        <label class="form-label small fw-semibold">Seleccionar Equipo</label>
                        <select id="selectMaqCostos" name="idMaq" class="form-select form-select-sm" onchange="cargarDatosMaquinaCostos()">
                            <% for (Map<String, String> m : maquinaria) { %>
                                <option value="<%= m.get("id") %>"
                                        data-adq="<%= m.get("costoAdq") %>"
                                        data-mant="<%= m.get("costoMant") %>">
                                    <%= m.get("nombre") %>
                                </option>
                            <% } %>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-semibold">Costo de Adquisición ($ MXN)</label>
                        <input type="number" step="0.01" min="0" id="inputCostoAdq" name="costoAdq" class="form-control form-control-sm" required>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-semibold">Costo de Mantenimiento ($ MXN)</label>
                        <input type="number" step="0.01" min="0" id="inputCostoMant" name="costoMant" class="form-control form-control-sm" required>
                    </div>
                    <div class="col-md-2">
                        <button type="submit" class="btn-aurea w-100" <%= totalEquipos == 0 ? "disabled" : "" %>>GUARDAR COSTOS</button>
                    </div>
                </form>
            </div>
            <% } %>

            <div class="card-aurea">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                    <h6 class="fw-bold m-0" style="color:#263540;">INVENTARIO DE EQUIPOS EN PLANTA (<%= nombreEmpresa %>)</h6>
                    <div class="small text-secondary fw-semibold">
                        Inversión Adquisición: <span class="text-dark">$<%= String.format(Locale.US, "%,.2f", totalCostoAdq) %> MXN</span>
                        &nbsp;|&nbsp;
                        Mantenimiento Acumulado: <span class="text-dark">$<%= String.format(Locale.US, "%,.2f", totalCostoMant) %> MXN</span>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table table-sm align-middle small mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Código</th>
                                <th>Nombre</th>
                                <th>Modelo</th>
                                <th>Año</th>
                                <th>Horómetro</th>
                                <th>Fallas Previas</th>
                                <th>Costo Adquisición</th>
                                <th>Costo Mant.</th>
                                <th>Estado</th>
                                <% if (esSupProduccion || esSupGeneral) { %>
                                <th class="text-end">Baja</th>
                                <% } %>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Map<String, String> m : maquinaria) {
                                String est = m.get("estado");
                                String claseBadge = "Óptimo".equalsIgnoreCase(est) || "Activa".equalsIgnoreCase(est) ? "badge-optimo" :
                                                    ("Alerta".equalsIgnoreCase(est) || "En Mantenimiento".equalsIgnoreCase(est) ? "badge-alerta" : "badge-inactiva");
                            %>
                            <tr>
                                <td class="fw-bold"><%= m.get("id") %></td>
                                <td><%= m.get("nombre") %></td>
                                <td><%= m.get("modelo") %></td>
                                <td><%= m.get("anio") %></td>
                                <td><%= m.get("horas") %> h</td>
                                <td><%= m.get("fallas") %></td>
                                <td>$<%= m.get("costoAdq") %></td>
                                <td>$<%= m.get("costoMant") %></td>
                                <td><span class="<%= claseBadge %>"><%= est %></span></td>
                                <% if (esSupProduccion || esSupGeneral) { %>
                                <td class="text-end">
                                    <form action="maquinaria" method="post" class="d-inline" onsubmit="return confirm('¿Confirmar baja definitiva del equipo <%= m.get("id") %>?');">
                                        <input type="hidden" name="accion" value="eliminarMaquina">
                                        <input type="hidden" name="idMaq" value="<%= m.get("id") %>">
                                        <button type="submit" class="btn btn-outline-danger btn-sm py-0 px-2" style="font-size:0.74rem;">Baja</button>
                                    </form>
                                </td>
                                <% } %>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>
        <% } %>

        <!-- ==================== PESTAÑA: MANTENIMIENTO Y ALERTAS (CMMS) ==================== -->
        <% if (puedeMantenimiento) { %>
        <section id="tab-mantenimiento" class="tab-pane-aurea">
            <div class="mb-3">
                <h5 class="fw-bold m-0" style="color:#263540;">GESTIÓN DE ÓRDENES DE MANTENIMIENTO — <%= nombreEmpresa.toUpperCase() %></h5>
                <small class="text-secondary">Programación preventiva y correctiva, plantillas rápidas de intervención y control de vencimientos</small>
            </div>

            <div class="card-aurea mb-4">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                    <h6 class="fw-bold m-0" style="color:#263540;">NUEVA ORDEN DE TRABAJO</h6>
                    <div class="d-flex align-items-center gap-2">
                        <small class="text-secondary fw-semibold">Plantilla Rápida:</small>
                        <select id="selectPlantillaMant" class="form-select form-select-sm" style="width: 260px;" onchange="aplicarPlantillaMantenimiento()">
                            <option value="">-- Seleccionar tarea estándar (opcional) --</option>
                            <option value="Preventivo|Lubricación de husillo principal y calibración de ejes|Aceite Sintético ISO VG 68|1200">Lubricación y calibración de husillo</option>
                            <option value="Correctivo|Reemplazo de kit de rodamientos y alineación láser|Kit Rodamientos SKF-20|3400">Cambio de rodamientos y alineación</option>
                            <option value="Preventivo|Purga de línea neumática y ajuste de válvula de alivio|Sello Viton y Filtro Neumático AF-30|950">Servicio neumático y válvulas</option>
                            <option value="Correctivo|Ajuste de tensión de banda y reemplazo de rodillo guía|Rodillo Guía Industrial RG-12|2100">Tensión de banda y rodillo guía</option>
                        </select>
                    </div>
                </div>

                <form action="mantenimiento" method="post" class="row g-3">
                    <input type="hidden" name="accion" value="registrar">
                    <div class="col-md-2">
                        <label class="form-label small fw-semibold">Equipo</label>
                        <select id="selectEquipoMantenimiento" name="idMaq" class="form-select form-select-sm">
                            <% for (Map<String, String> m : maquinaria) { %>
                                <option value="<%= m.get("id") %>"><%= m.get("id") %> (<%= m.get("estado") %>)</option>
                            <% } %>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label small fw-semibold">Tipo de Orden</label>
                        <select id="selectTipoMant" name="tipo" class="form-select form-select-sm">
                            <option value="Preventivo">Preventivo</option>
                            <option value="Correctivo">Correctivo</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-semibold">Actividades a Ejecutar</label>
                        <input type="text" id="inputActMant" name="actividades" class="form-control form-control-sm" placeholder="Descripción técnica" required>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label small fw-semibold">Refacciones / Insumos</label>
                        <input type="text" id="inputRefMant" name="refacciones" class="form-control form-control-sm" placeholder="Ej. Kit SKF-20" required>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label small fw-semibold">Fecha Programada</label>
                        <input type="date" name="fecha" class="form-control form-control-sm" value="2026-11-22" required>
                    </div>
                    <div class="col-md-1">
                        <label class="form-label small fw-semibold" title="Se suma automáticamente al costo de mantenimiento del equipo">Costo ($)</label>
                        <input type="number" step="0.01" min="0" id="inputCostoIntervencion" name="costoIntervencion" class="form-control form-control-sm" value="0">
                    </div>
                    <div class="col-12 d-flex justify-content-end">
                        <button type="submit" class="btn-aurea px-4" <%= totalEquipos == 0 ? "disabled" : "" %>>REGISTRAR ORDEN DE TRABAJO</button>
                    </div>
                </form>
            </div>

            <div class="card-aurea">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                    <h6 class="fw-bold m-0" style="color:#263540;">TABLERO DE INTERVENCIONES Y ALERTAS</h6>
                    <div class="d-flex flex-wrap gap-1">
                        <button type="button" class="filter-pill-aurea active" onclick="filtrarOrdenesMantenimiento('', this)">Todas</button>
                        <button type="button" class="filter-pill-aurea" onclick="filtrarOrdenesMantenimiento('urgente', this)">Prioritarias (<%= ordenesUrgentes %>)</button>
                        <button type="button" class="filter-pill-aurea" onclick="filtrarOrdenesMantenimiento('Programado', this)">Programadas</button>
                        <button type="button" class="filter-pill-aurea" onclick="filtrarOrdenesMantenimiento('Completado', this)">Completadas (<%= ordenesCompletadas %>)</button>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table table-sm align-middle small mb-0" id="tablaOrdenesMantenimiento">
                        <thead class="table-light">
                            <tr>
                                <th>Equipo</th>
                                <th>Tipo</th>
                                <th>Actividades</th>
                                <th>Refacciones Utilizadas</th>
                                <th>Fecha Programada</th>
                                <th>Responsable</th>
                                <th>Estado / Alerta</th>
                                <th class="text-end">Acción</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (mantenimientos != null) {
                                for (Map<String, String> mt : mantenimientos) {
                                    String alerta = mt.get("alerta") != null ? mt.get("alerta") : "Programado";
                                    boolean completado = "Completado".equalsIgnoreCase(alerta);
                                    boolean esUrgente = "Vencido".equalsIgnoreCase(alerta) || alerta.contains("Próximo") || "Urgente".equalsIgnoreCase(alerta);
                                    String claseAlerta = completado ? "badge-optimo" :
                                            ("Vencido".equalsIgnoreCase(alerta) ? "badge-inactiva" :
                                            (esUrgente ? "badge-alerta" : "badge-programado"));
                            %>
                            <tr data-alerta="<%= alerta %>" data-urgente="<%= esUrgente ? "1" : "0" %>">
                                <td class="fw-bold"><%= mt.get("idMaq") %></td>
                                <td><%= mt.get("tipo") %></td>
                                <td><%= mt.get("act") %></td>
                                <td><%= mt.get("ref") %></td>
                                <td><%= mt.get("fecha") %></td>
                                <td><%= mt.get("tec") %></td>
                                <td><span class="<%= claseAlerta %>"><%= alerta %></span></td>
                                <td class="text-end">
                                    <% if (!completado) { %>
                                        <form action="mantenimiento" method="post" class="d-inline">
                                            <input type="hidden" name="accion" value="completar">
                                            <input type="hidden" name="idMant" value="<%= mt.get("idMant") %>">
                                            <input type="hidden" name="idMaq" value="<%= mt.get("idMaq") %>">
                                            <button type="submit" class="btn btn-outline-success btn-sm py-0 px-2 fw-semibold" style="font-size:0.75rem;">
                                                Concluir Orden
                                            </button>
                                        </form>
                                    <% } else { %>
                                        <span class="text-muted" style="font-size:0.75rem;">&#10003; Cerrada</span>
                                    <% } %>
                                </td>
                            </tr>
                            <%  }
                               } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>
        <% } %>

        <!-- ==================== PESTAÑA: REPORTES EJECUTIVOS ==================== -->
        <% if (puedeReportes) { %>
        <section id="tab-reportes" class="tab-pane-aurea">
            <div class="d-none d-print-block mb-3 border-bottom pb-2">
                <div class="d-flex align-items-center gap-2">
                    <img src="img/logo-aurea.png" alt="Logo" style="width:36px;height:36px;border-radius:6px;">
                    <div>
                        <h4 class="fw-bold m-0" style="color:#263540;"><%= nombreEmpresa.toUpperCase() %> — REPORTE EJECUTIVO DE PLANTA</h4>
                        <small class="text-muted">Sector: <%= sectorEmpresa %> &middot; Generado por: <%= usuarioNombre %> (<%= usuarioRol %>)</small>
                    </div>
                </div>
            </div>

            <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2 no-print">
                <div>
                    <h5 class="fw-bold m-0" style="color:#263540;">REPORTES EJECUTIVOS — <%= nombreEmpresa.toUpperCase() %></h5>
                    <small class="text-secondary">Consolidación financiera (TCO), intensidad de mantenimiento por activo y exportación a PDF y Excel (CSV)</small>
                </div>
                <div class="d-flex gap-2">
                    <button type="button" onclick="window.print()" class="btn btn-outline-dark btn-sm fw-semibold">Imprimir / Guardar PDF</button>
                    <a id="btnExportarCsv" href="reportes?tipo=general" class="btn-aurea btn-sm text-decoration-none">Exportar a Excel (CSV)</a>
                </div>
            </div>

            <div class="card-aurea mb-3 no-print">
                <div class="row g-3 align-items-end">
                    <div class="col-md-4">
                        <label class="form-label small fw-semibold">Tipo de Reporte</label>
                        <select id="filtroTipoReporte" class="form-select form-select-sm" onchange="aplicarFiltrosReporte()">
                            <option value="general">Consolidado General de Maquinaria y Costos</option>
                            <option value="fallas">Equipos con Incidencias / Historial de Fallas</option>
                            <option value="mantenimientos">Historial de Órdenes de Mantenimiento</option>
                        </select>
                    </div>
                    <div class="col-md-4" id="contenedorFiltroEstado">
                        <label class="form-label small fw-semibold">Filtrar por Estado Operativo</label>
                        <select id="filtroEstadoReporte" class="form-select form-select-sm" onchange="aplicarFiltrosReporte()">
                            <option value="">Todos los estados</option>
                            <option value="Óptimo">Óptimo</option>
                            <option value="Alerta">Alerta</option>
                            <option value="En Mantenimiento">En Mantenimiento</option>
                            <option value="Inactiva">Inactiva</option>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label small fw-semibold">Filtrar por Equipo</label>
                        <select id="filtroEquipoReporte" class="form-select form-select-sm" onchange="aplicarFiltrosReporte()">
                            <option value="">Todos los equipos registrados</option>
                            <% for (Map<String, String> m : maquinaria) { %>
                                <option value="<%= m.get("id") %>"><%= m.get("nombre") %></option>
                            <% } %>
                        </select>
                    </div>
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-md-3">
                    <div class="card-aurea py-2 px-3">
                        <small class="text-secondary fw-bold" style="font-size:0.72rem;">HORAS DE USO ACUMULADAS</small>
                        <div class="fw-bold fs-5" style="color:#263540;"><%= String.format(Locale.US, "%,d", totalHorasPlanta) %> h</div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card-aurea py-2 px-3">
                        <small class="text-secondary fw-bold" style="font-size:0.72rem;">INVERSIÓN EN ADQUISICIÓN</small>
                        <div class="fw-bold fs-5" style="color:#263540;">$<%= String.format(Locale.US, "%,.2f", totalCostoAdq) %></div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card-aurea py-2 px-3">
                        <small class="text-secondary fw-bold" style="font-size:0.72rem;">COSTO EN MANTENIMIENTO</small>
                        <div class="fw-bold fs-5" style="color:#263540;">$<%= String.format(Locale.US, "%,.2f", totalCostoMant) %></div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card-aurea py-2 px-3">
                        <small class="text-secondary fw-bold" style="font-size:0.72rem;">VALOR TOTAL TCO EN PLANTA</small>
                        <div class="fw-bold fs-5" style="color:#166534;">$<%= String.format(Locale.US, "%,.2f", (totalCostoAdq + totalCostoMant)) %></div>
                    </div>
                </div>
            </div>

            <div class="card-aurea" id="bloqueTablaReporteActivos">
                <h6 class="fw-bold mb-3" style="color:#263540;">DETALLE EJECUTIVO DE ACTIVOS, COSTO TOTAL (TCO) E INTENSIDAD DE MANTENIMIENTO</h6>
                <div class="table-responsive">
                    <table class="table table-sm align-middle small mb-0" id="tablaReporteActivos">
                        <thead class="table-light">
                            <tr>
                                <th>Código</th>
                                <th>Activo Industrial</th>
                                <th>Horómetro</th>
                                <th>Fallas Históricas</th>
                                <th>Estado</th>
                                <th>Costo Adquisición</th>
                                <th>Costo Mant.</th>
                                <th>Ratio Mant./Adq.</th>
                                <th>Costo Total (TCO)</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Map<String, String> m : maquinaria) {
                                double cAdq = Double.parseDouble(m.get("costoAdq"));
                                double cMant = Double.parseDouble(m.get("costoMant"));
                                double ratioMant = cAdq > 0 ? (cMant / cAdq) * 100.0 : 0.0;
                                String fallas = m.get("fallas") != null ? m.get("fallas") : "Ninguna";
                                String est = m.get("estado");
                                String claseBadge = "Óptimo".equalsIgnoreCase(est) || "Activa".equalsIgnoreCase(est) ? "badge-optimo" :
                                                    ("Alerta".equalsIgnoreCase(est) || "En Mantenimiento".equalsIgnoreCase(est) ? "badge-alerta" : "badge-inactiva");
                            %>
                            <tr data-equipo="<%= m.get("id") %>"
                                data-estado="<%= est %>"
                                data-con-falla="<%= (!"Ninguna".equalsIgnoreCase(fallas.trim()) || "Alerta".equalsIgnoreCase(est)) ? "1" : "0" %>">
                                <td class="fw-bold"><%= m.get("id") %></td>
                                <td><%= m.get("nombre") %> <small class="text-muted">(<%= m.get("modelo") %>)</small></td>
                                <td><%= m.get("horas") %> h</td>
                                <td><%= fallas %></td>
                                <td><span class="<%= claseBadge %>"><%= est %></span></td>
                                <td>$<%= m.get("costoAdq") %></td>
                                <td>$<%= m.get("costoMant") %></td>
                                <td>
                                    <span class="fw-semibold" style="color:<%= ratioMant > 20.0 ? "#92400E" : "#263540" %>;">
                                        <%= String.format(Locale.US, "%.1f%%", ratioMant) %>
                                    </span>
                                </td>
                                <td class="fw-bold">$<%= String.format(Locale.US, "%.2f", (cAdq + cMant)) %> MXN</td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card-aurea" id="bloqueTablaReporteMantenimientos" style="display: none;">
                <h6 class="fw-bold mb-3" style="color:#263540;">REPORTE HISTÓRICO DE INTERVENCIONES DE MANTENIMIENTO</h6>
                <div class="table-responsive">
                    <table class="table table-sm align-middle small mb-0" id="tablaReporteMantenimientos">
                        <thead class="table-light">
                            <tr>
                                <th>Equipo</th>
                                <th>Tipo</th>
                                <th>Actividades</th>
                                <th>Refacciones / Insumos</th>
                                <th>Fecha Programada</th>
                                <th>Responsable</th>
                                <th>Estado de Orden</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (mantenimientos != null) {
                                for (Map<String, String> mt : mantenimientos) { %>
                            <tr data-equipo="<%= mt.get("idMaq") %>">
                                <td class="fw-bold"><%= mt.get("idMaq") %></td>
                                <td><%= mt.get("tipo") %></td>
                                <td><%= mt.get("act") %></td>
                                <td><%= mt.get("ref") %></td>
                                <td><%= mt.get("fecha") %></td>
                                <td><%= mt.get("tec") %></td>
                                <td><span class="fw-semibold"><%= mt.get("alerta") %></span></td>
                            </tr>
                            <%  }
                               } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>
        <% } %>

        <!-- ==================== PESTAÑA: PERSONAL Y ACCESOS (APROBACIÓN MULTIEMPRESA) ==================== -->
        <% if (esSupGeneral) { %>
        <section id="tab-usuarios" class="tab-pane-aurea">
            <div class="mb-3">
                <h5 class="fw-bold m-0" style="color:#263540;">CONTROL DE ACCESOS Y PERSONAL — <%= nombreEmpresa.toUpperCase() %></h5>
                <small class="text-secondary">Aprobación de solicitudes vinculadas por Código Único de Empresa y gestión de permisos internos</small>
            </div>

            <!-- TARJETA DE IDENTIDAD DE LA EMPRESA Y CÓDIGO ÚNICO PARA COMPARTIR -->
            <div class="card-aurea mb-4" style="border-left: 4px solid #263540;">
                <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
                    <div>
                        <small class="text-secondary fw-bold">ORGANIZACIÓN REGISTRADA EN SENSEMACH</small>
                        <h6 class="fw-bold m-0 mt-1" style="color:#263540;"><%= nombreEmpresa %> (<%= sectorEmpresa %>)</h6>
                        <small class="text-muted">Comparta el Código Único con sus técnicos y supervisores para que soliciten unirse desde la pantalla de registro.</small>
                    </div>
                    <div class="d-flex align-items-center gap-2 p-2 rounded" style="background-color:#F8FAFC;border:1px solid #CBD5E1;">
                        <div>
                            <small class="text-muted d-block" style="font-size:0.68rem;">CÓDIGO ÚNICO DE EMPRESA</small>
                            <span class="brand-font fw-bold" style="font-size:1.2rem;color:#263540;letter-spacing:1px;"><%= codigoEmpresa %></span>
                        </div>
                        <button type="button" class="btn-aurea btn-sm ms-2" onclick="navigator.clipboard.writeText('<%= codigoEmpresa %>');this.innerText='¡Copiado!';">
                            Copiar Código
                        </button>
                    </div>
                </div>
            </div>

            <!-- SECCIÓN 1: SOLICITUDES DE ACCESO PENDIENTES DE APROBACIÓN -->
            <div class="card-aurea mb-4" style="border-top: 3px solid #D97706;">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <div>
                        <h6 class="fw-bold m-0" style="color:#92400E;">SOLICITUDES DE ACCESO PENDIENTES DE APROBACIÓN</h6>
                        <small class="text-secondary">Usuarios que ingresaron el código <b><%= codigoEmpresa %></b> al crear su cuenta y esperan autorización</small>
                    </div>
                    <span class="badge-alerta"><%= solicitudesPendientes.size() %> pendientes</span>
                </div>

                <% if (solicitudesPendientes.isEmpty()) { %>
                    <div class="p-3 rounded text-center small text-muted" style="background-color:#F8FAFC;">
                        No hay solicitudes de acceso pendientes para <b><%= nombreEmpresa %></b> en este momento.
                    </div>
                <% } else { %>
                    <div class="table-responsive">
                        <table class="table table-sm align-middle small mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Solicitante</th>
                                    <th>Correo Corporativo</th>
                                    <th>Rol Solicitado (Ajustable antes de aprobar)</th>
                                    <th class="text-end">Decisión del Supervisor General</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (Map<String, String> sol : solicitudesPendientes) {
                                    String rolSol = sol.get("rol");
                                %>
                                <tr>
                                    <td class="fw-bold"><%= sol.get("nombre") %></td>
                                    <td><%= sol.get("correo") %></td>
                                    <td colspan="2">
                                        <div class="d-flex flex-wrap align-items-center justify-content-between gap-2">
                                            <form action="usuarios" method="post" class="d-flex align-items-center gap-2 flex-grow-1 m-0">
                                                <input type="hidden" name="accion" value="aprobarSolicitud">
                                                <input type="hidden" name="idUsuario" value="<%= sol.get("id") %>">
                                                <select name="rol" class="form-select form-select-sm" style="max-width: 260px;">
                                                    <option value="Técnico de Diagnóstico y Soporte" <%= "Técnico de Diagnóstico y Soporte".equals(rolSol) ? "selected" : "" %>>Técnico de Diagnóstico y Soporte</option>
                                                    <option value="Técnico de Línea B" <%= "Técnico de Línea B".equals(rolSol) ? "selected" : "" %>>Técnico de Línea B</option>
                                                    <option value="Técnico Senior" <%= "Técnico Senior".equals(rolSol) ? "selected" : "" %>>Técnico Senior</option>
                                                    <option value="Supervisor de Producción" <%= "Supervisor de Producción".equals(rolSol) ? "selected" : "" %>>Supervisor de Producción</option>
                                                    <option value="Supervisor Financiero" <%= "Supervisor Financiero".equals(rolSol) ? "selected" : "" %>>Supervisor Financiero</option>
                                                    <option value="Supervisor General" <%= "Supervisor General".equals(rolSol) ? "selected" : "" %>>Supervisor General</option>
                                                </select>
                                                <button type="submit" class="btn btn-success btn-sm py-1 px-3 fw-semibold" style="font-size:0.76rem;">
                                                    &#10003; Aprobar Acceso
                                                </button>
                                            </form>
                                            <form action="usuarios" method="post" class="m-0" onsubmit="return confirm('¿Rechazar la solicitud de <%= sol.get("nombre") %>?');">
                                                <input type="hidden" name="accion" value="rechazarSolicitud">
                                                <input type="hidden" name="idUsuario" value="<%= sol.get("id") %>">
                                                <button type="submit" class="btn btn-outline-danger btn-sm py-1 px-2 fw-semibold" style="font-size:0.76rem;">
                                                    Rechazar
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                <% } %>
            </div>

            <!-- SECCIÓN 2: ALTA DIRECTA POR EL SUPERVISOR GENERAL -->
            <div class="card-aurea mb-4">
                <h6 class="fw-bold mb-3" style="color:#263540;">ALTA DIRECTA DE COLABORADOR EN <%= nombreEmpresa.toUpperCase() %></h6>
                <form action="usuarios" method="post" class="row g-3">
                    <input type="hidden" name="accion" value="registrar">
                    <div class="col-md-3">
                        <label class="form-label small fw-semibold">Nombre Completo</label>
                        <input type="text" name="nombre" class="form-control form-control-sm" placeholder="Nombre y apellidos" required>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-semibold">Correo Corporativo</label>
                        <input type="email" name="correo" class="form-control form-control-sm" placeholder="colaborador@empresa.mx" required>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label small fw-semibold">Contraseña Inicial</label>
                        <input type="password" name="pass" class="form-control form-control-sm" placeholder="Contraseña" required>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label small fw-semibold">Perfil Operativo</label>
                        <select name="rol" class="form-select form-select-sm">
                            <option value="Supervisor General">Supervisor General</option>
                            <option value="Supervisor de Producción">Supervisor de Producción</option>
                            <option value="Supervisor Financiero">Supervisor Financiero</option>
                            <option value="Técnico Senior">Técnico Senior</option>
                            <option value="Técnico de Línea B">Técnico de Línea B</option>
                            <option value="Técnico de Diagnóstico y Soporte">Técnico de Diagnóstico y Soporte</option>
                        </select>
                    </div>
                    <div class="col-md-1">
                        <label class="form-label small fw-semibold">Estado</label>
                        <select name="estado" class="form-select form-select-sm">
                            <option value="Activa">Activa</option>
                            <option value="Inactiva">Inactiva</option>
                        </select>
                    </div>
                    <div class="col-md-1 d-flex align-items-end">
                        <button type="submit" class="btn-aurea w-100">ALTA</button>
                    </div>
                </form>
            </div>

            <!-- SECCIÓN 3: DIRECTORIO DE PERSONAL DE LA EMPRESA -->
            <div class="card-aurea">
                <h6 class="fw-bold mb-3" style="color:#263540;">DIRECTORIO DE PERSONAL AUTORIZADO (<%= nombreEmpresa %>)</h6>
                <div class="table-responsive">
                    <table class="table table-sm align-middle small mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Colaborador</th>
                                <th>Correo Corporativo</th>
                                <th>Perfil Asignado</th>
                                <th>Estado de Acceso</th>
                                <th class="text-end">Guardar Cambios</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Map<String, String> u : personalEmpresa) {
                                String rolU = u.get("rol");
                                String estU = u.get("estado");
                            %>
                            <tr>
                                <td class="fw-semibold"><%= u.get("nombre") %></td>
                                <td><%= u.get("correo") %></td>
                                <td colspan="3">
                                    <form action="usuarios" method="post" class="row g-2 align-items-center m-0">
                                        <input type="hidden" name="accion" value="actualizar">
                                        <input type="hidden" name="idUsuario" value="<%= u.get("id") %>">
                                        <div class="col-md-6">
                                            <select name="rol" class="form-select form-select-sm">
                                                <option value="Supervisor General" <%= "Supervisor General".equals(rolU) ? "selected" : "" %>>Supervisor General</option>
                                                <option value="Supervisor de Producción" <%= "Supervisor de Producción".equals(rolU) ? "selected" : "" %>>Supervisor de Producción</option>
                                                <option value="Supervisor Financiero" <%= "Supervisor Financiero".equals(rolU) ? "selected" : "" %>>Supervisor Financiero</option>
                                                <option value="Técnico Senior" <%= "Técnico Senior".equals(rolU) ? "selected" : "" %>>Técnico Senior</option>
                                                <option value="Técnico de Línea B" <%= "Técnico de Línea B".equals(rolU) ? "selected" : "" %>>Técnico de Línea B</option>
                                                <option value="Técnico de Diagnóstico y Soporte" <%= "Técnico de Diagnóstico y Soporte".equals(rolU) ? "selected" : "" %>>Técnico de Diagnóstico y Soporte</option>
                                            </select>
                                        </div>
                                        <div class="col-md-3">
                                            <select name="estado" class="form-select form-select-sm">
                                                <option value="Activa" <%= "Activa".equalsIgnoreCase(estU) ? "selected" : "" %>>Activa</option>
                                                <option value="Inactiva" <%= "Inactiva".equalsIgnoreCase(estU) ? "selected" : "" %>>Inactiva</option>
                                            </select>
                                        </div>
                                        <div class="col-md-3 text-end">
                                            <button type="submit" class="btn btn-outline-dark btn-sm py-1 px-2 fw-semibold" style="font-size:0.75rem;">
                                                Actualizar
                                            </button>
                                        </div>
                                    </form>
                                </td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>
        <% } %>

    </main>
</div>

<script>
    let estadoFiltroResumenActual = '';

    function seleccionarPestana(tabId) {
        const paneles = document.querySelectorAll('.tab-pane-aurea');
        const botones = document.querySelectorAll('.nav-item-aurea');

        let existePanel = document.getElementById('tab-' + tabId);
        if (!existePanel) {
            tabId = 'resumen';
            existePanel = document.getElementById('tab-resumen');
        }

        paneles.forEach(p => p.classList.remove('active'));
        botones.forEach(b => b.classList.remove('active'));

        if (existePanel) {
            existePanel.classList.add('active');
        }
        const botonActivo = document.querySelector('.nav-item-aurea[data-tab="' + tabId + '"]');
        if (botonActivo) {
            botonActivo.classList.add('active');
            const tituloTop = document.getElementById('tituloSeccionActiva');
            if (tituloTop) {
                tituloTop.innerText = botonActivo.getAttribute('data-title') || 'Panel General';
            }
        }
        localStorage.setItem('aurea_active_tab', tabId);
    }

    function alternarSidebar() {
        const sidebar = document.getElementById('sidebarAurea');
        sidebar.classList.toggle('collapsed');
        localStorage.setItem('aurea_sidebar_collapsed', sidebar.classList.contains('collapsed') ? '1' : '0');
    }

    function cambiarVistaPlanta(modo) {
        const tarjetas = document.getElementById('contenedorTarjetasPlanta');
        const tabla = document.getElementById('contenedorTablaPlanta');
        const btnTarjetas = document.getElementById('btnVistaTarjetas');
        const btnTabla = document.getElementById('btnVistaTabla');
        if (!tarjetas || !tabla) return;

        if (modo === 'tabla') {
            tarjetas.style.display = 'none';
            tabla.style.display = 'block';
            btnTarjetas.classList.remove('active');
            btnTabla.classList.add('active');
        } else {
            tarjetas.style.display = 'flex';
            tabla.style.display = 'none';
            btnTarjetas.classList.add('active');
            btnTabla.classList.remove('active');
        }
    }

    function seleccionarFiltroEstadoResumen(estado, btn) {
        estadoFiltroResumenActual = estado;
        const botones = btn.parentElement.querySelectorAll('.filter-pill-aurea');
        botones.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        filtrarPlantaGeneral();
    }

    function filtrarPlantaGeneral() {
        const texto = (document.getElementById('buscadorResumen')?.value || '').toLowerCase();

        document.querySelectorAll('.item-planta-filtrable').forEach(card => {
            const t = card.getAttribute('data-texto') || '';
            const e = card.getAttribute('data-estado') || '';
            const okTexto = !texto || t.includes(texto);
            const okEst = !estadoFiltroResumenActual || e.toLowerCase() === estadoFiltroResumenActual.toLowerCase();
            card.style.display = (okTexto && okEst) ? '' : 'none';
        });

        document.querySelectorAll('.item-planta-filtrable-tabla').forEach(fila => {
            const t = fila.getAttribute('data-texto') || '';
            const e = fila.getAttribute('data-estado') || '';
            const okTexto = !texto || t.includes(texto);
            const okEst = !estadoFiltroResumenActual || e.toLowerCase() === estadoFiltroResumenActual.toLowerCase();
            fila.style.display = (okTexto && okEst) ? '' : 'none';
        });
    }

    function irAMonitoreoConEquipo(idMaq) {
        seleccionarPestana('monitoreo');
        const sel = document.getElementById('selectEquipoMonitoreo');
        if (sel) {
            sel.value = idMaq;
            actualizarFichaMonitoreo();
            document.getElementById('inputMonTemp')?.focus();
        }
    }

    function irAMantenimientoConEquipo(idMaq) {
        seleccionarPestana('mantenimiento');
        const sel = document.getElementById('selectEquipoMantenimiento');
        if (sel) {
            sel.value = idMaq;
            document.getElementById('inputActMant')?.focus();
        }
    }

    function actualizarFichaMonitoreo() {
        const sel = document.getElementById('selectEquipoMonitoreo');
        if (!sel || sel.selectedIndex < 0) return;
        const opt = sel.options[sel.selectedIndex];
        document.getElementById('infoMonModelo').innerText = opt.getAttribute('data-modelo') || '-';
        document.getElementById('infoMonHoras').innerText = opt.getAttribute('data-horas') || '-';
        document.getElementById('infoMonFallas').innerText = opt.getAttribute('data-fallas') || '-';
    }

    function evaluarUmbralesEnVivo() {
        const banner = document.getElementById('bannerDiagnosticoVivo');
        if (!banner) return;

        const tStr = document.getElementById('inputMonTemp')?.value;
        const pStr = document.getElementById('inputMonPres')?.value;
        const vStr = document.getElementById('inputMonVib')?.value;

        if (!tStr && !pStr && !vStr) {
            banner.style.backgroundColor = '#F1F5F9';
            banner.style.color = '#475569';
            banner.innerText = 'Ingrese las lecturas instrumentales para previsualizar el dictamen automático.';
            return;
        }

        const alertas = [];
        const t = parseFloat(tStr);
        const p = parseFloat(pStr);
        const v = parseFloat(vStr);

        if (!isNaN(t) && t > 80.0) alertas.push('Sobrecalentamiento (' + t + ' °C)');
        if (!isNaN(p) && (p < 60.0 || p > 150.0)) alertas.push('Presión fuera de rango (' + p + ' psi)');
        if (!isNaN(v) && v > 6.5) alertas.push('Vibración excesiva (' + v + ' mm/s)');

        if (alertas.length > 0) {
            banner.style.backgroundColor = '#FEF3C7';
            banner.style.color = '#92400E';
            banner.innerText = '⚠ Alerta detectada: ' + alertas.join(', ') + '. El equipo pasará a estado Alerta.';
        } else {
            banner.style.backgroundColor = '#DCFCE7';
            banner.style.color = '#166534';
            banner.innerText = '✓ Parámetros dentro de rango normativo. El equipo se registrará como Óptimo.';
        }
    }

    function aplicarPlantillaMantenimiento() {
        const sel = document.getElementById('selectPlantillaMant');
        if (!sel || !sel.value) return;
        const partes = sel.value.split('|');
        if (partes.length === 4) {
            document.getElementById('selectTipoMant').value = partes[0];
            document.getElementById('inputActMant').value = partes[1];
            document.getElementById('inputRefMant').value = partes[2];
            document.getElementById('inputCostoIntervencion').value = partes[3];
        }
    }

    function filtrarOrdenesMantenimiento(criterio, btn) {
        const botones = btn.parentElement.querySelectorAll('.filter-pill-aurea');
        botones.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        const filas = document.querySelectorAll('#tablaOrdenesMantenimiento tbody tr');
        filas.forEach(fila => {
            const alerta = fila.getAttribute('data-alerta') || '';
            const urgente = fila.getAttribute('data-urgente') === '1';
            if (!criterio) {
                fila.style.display = '';
            } else if (criterio === 'urgente') {
                fila.style.display = urgente ? '' : 'none';
            } else {
                fila.style.display = (alerta.toLowerCase() === criterio.toLowerCase()) ? '' : 'none';
            }
        });
    }

    function filtrarBitacora() {
        const equipo = document.getElementById('filtroBitacoraEquipo')?.value || '';
        const filas = document.querySelectorAll('#tablaBitacora tbody tr');
        filas.forEach(fila => {
            const eqFila = fila.getAttribute('data-equipo') || '';
            fila.style.display = (!equipo || eqFila === equipo) ? '' : 'none';
        });
    }

    function cargarDatosMaquinaOperacion() {
        const sel = document.getElementById('selectMaqOperacion');
        if (!sel || sel.selectedIndex < 0) return;
        const opt = sel.options[sel.selectedIndex];
        document.getElementById('inputHorasOperacion').value = opt.getAttribute('data-horas') || '0';
        document.getElementById('inputFallasOperacion').value = opt.getAttribute('data-fallas') || 'Ninguna';
        const estadoActual = opt.getAttribute('data-estado') || 'Óptimo';
        const selEst = document.getElementById('selectEstadoOperacion');
        if (selEst) {
            for (let i = 0; i < selEst.options.length; i++) {
                if (selEst.options[i].value === estadoActual) {
                    selEst.selectedIndex = i;
                    break;
                }
            }
        }
    }

    function cargarDatosMaquinaCostos() {
        const sel = document.getElementById('selectMaqCostos');
        if (!sel || sel.selectedIndex < 0) return;
        const opt = sel.options[sel.selectedIndex];
        document.getElementById('inputCostoAdq').value = opt.getAttribute('data-adq') || '0.00';
        document.getElementById('inputCostoMant').value = opt.getAttribute('data-mant') || '0.00';
    }

    function aplicarFiltrosReporte() {
        const tipo = document.getElementById('filtroTipoReporte')?.value || 'general';
        const estado = document.getElementById('filtroEstadoReporte')?.value || '';
        const equipo = document.getElementById('filtroEquipoReporte')?.value || '';

        const bloqueActivos = document.getElementById('bloqueTablaReporteActivos');
        const bloqueMant = document.getElementById('bloqueTablaReporteMantenimientos');
        const contEstado = document.getElementById('contenedorFiltroEstado');

        if (tipo === 'mantenimientos') {
            if (bloqueActivos) bloqueActivos.style.display = 'none';
            if (bloqueMant) bloqueMant.style.display = 'block';
            if (contEstado) contEstado.style.visibility = 'hidden';

            const filasMant = document.querySelectorAll('#tablaReporteMantenimientos tbody tr');
            filasMant.forEach(fila => {
                const eq = fila.getAttribute('data-equipo') || '';
                fila.style.display = (!equipo || eq === equipo) ? '' : 'none';
            });
        } else {
            if (bloqueActivos) bloqueActivos.style.display = 'block';
            if (bloqueMant) bloqueMant.style.display = 'none';
            if (contEstado) contEstado.style.visibility = 'visible';

            const filasAct = document.querySelectorAll('#tablaReporteActivos tbody tr');
            filasAct.forEach(fila => {
                const eq = fila.getAttribute('data-equipo') || '';
                const est = fila.getAttribute('data-estado') || '';
                const conFalla = fila.getAttribute('data-con-falla') === '1';

                const okEquipo = !equipo || eq === equipo;
                const okEstado = !estado || est.toLowerCase() === estado.toLowerCase();
                const okTipo = (tipo !== 'fallas') || conFalla;

                fila.style.display = (okEquipo && okEstado && okTipo) ? '' : 'none';
            });
        }

        const btnCsv = document.getElementById('btnExportarCsv');
        if (btnCsv) {
            const params = new URLSearchParams();
            params.set('tipo', tipo);
            if (estado && tipo !== 'mantenimientos') params.set('estado', estado);
            if (equipo) params.set('equipo', equipo);
            btnCsv.href = 'reportes?' + params.toString();
        }
    }

    document.addEventListener('DOMContentLoaded', function() {
        const params = new URLSearchParams(window.location.search);
        const tabUrl = params.get('tab');
        const tabGuardada = localStorage.getItem('aurea_active_tab') || 'resumen';
        seleccionarPestana(tabUrl ? tabUrl : tabGuardada);

        if (localStorage.getItem('aurea_sidebar_collapsed') === '1') {
            document.getElementById('sidebarAurea').classList.add('collapsed');
        }

        actualizarFichaMonitoreo();
        cargarDatosMaquinaOperacion();
        cargarDatosMaquinaCostos();
    });
</script>

</body>
</html>
