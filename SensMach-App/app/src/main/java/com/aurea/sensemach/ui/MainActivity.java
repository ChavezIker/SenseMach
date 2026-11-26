package com.aurea.sensemach.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.aurea.sensemach.R;
import com.aurea.sensemach.data.ApiClient;
import com.aurea.sensemach.data.SessionManager;
import com.aurea.sensemach.model.Mantenimiento;
import com.aurea.sensemach.model.Maquina;
import com.aurea.sensemach.model.SolicitudAcceso;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private SessionManager session;
    private String rol;
    private boolean esSupGeneral, esSupProd, esSupFinanciero, puedeMonitorear, puedeOrdenes;

    private ProgressBar progressTop;
    private View viewResumen, viewMonitoreo, viewOrdenes, viewAccesos, viewCostos;
    private BottomNavigationView bottomNav;

    private TextView tvKpiEficiencia, tvKpiOptimos, tvKpiSeguridad, tvKpiAlertasDetalle, tvBannerSolicitudes;
    private View bannerSolicitudesPendientes;
    private LinearLayout listaMaquinasContainer;
    private MaterialButton btnAltaRapidaMaquina;

    private Spinner spinnerEquipoMon;
    private TextInputEditText etTemp, etPresion, etVibracion, etHorasTurno, etObservaciones;
    private TextView tvDictamenVivo;
    private MaterialButton btnGuardarLectura;

    private LinearLayout listaOrdenesContainer;
    private MaterialButton btnNuevaOrden;

    private TextView tvClaveUnicaEmpresa;
    private MaterialButton btnCopiarClave;
    private LinearLayout listaSolicitudesContainer;

    private TextView tvTotalTco, tvDetalleFinanciero;
    private LinearLayout listaCostosContainer;

    private final List<Maquina> listaMaquinas = new ArrayList<>();
    private final List<Mantenimiento> listaOrdenes = new ArrayList<>();
    private final List<SolicitudAcceso> listaSolicitudes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        if (!session.estaLogueado()) {
            volverAlLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        rol = session.getRol();
        esSupGeneral = "Supervisor General".equalsIgnoreCase(rol);
        esSupProd = "Supervisor de Producción".equalsIgnoreCase(rol);
        esSupFinanciero = "Supervisor Financiero".equalsIgnoreCase(rol);
        puedeMonitorear = !esSupFinanciero;
        puedeOrdenes = esSupGeneral || rol.toLowerCase(Locale.ROOT).contains("técnico");

        enlazarVistas();
        configurarHeader();
        configurarMenuPorRol();
        configurarEventos();

        cargarDatosEmpresa();
    }

    private void enlazarVistas() {
        progressTop = findViewById(R.id.progressTop);
        viewResumen = findViewById(R.id.viewResumen);
        viewMonitoreo = findViewById(R.id.viewMonitoreo);
        viewOrdenes = findViewById(R.id.viewOrdenes);
        viewAccesos = findViewById(R.id.viewAccesos);
        viewCostos = findViewById(R.id.viewCostos);
        bottomNav = findViewById(R.id.bottomNav);

        tvKpiEficiencia = findViewById(R.id.tvKpiEficiencia);
        tvKpiOptimos = findViewById(R.id.tvKpiOptimos);
        tvKpiSeguridad = findViewById(R.id.tvKpiSeguridad);
        tvKpiAlertasDetalle = findViewById(R.id.tvKpiAlertasDetalle);
        bannerSolicitudesPendientes = findViewById(R.id.bannerSolicitudesPendientes);
        tvBannerSolicitudes = findViewById(R.id.tvBannerSolicitudes);
        listaMaquinasContainer = findViewById(R.id.listaMaquinasContainer);
        btnAltaRapidaMaquina = findViewById(R.id.btnAltaRapidaMaquina);

        spinnerEquipoMon = findViewById(R.id.spinnerEquipoMon);
        etTemp = findViewById(R.id.etTemp);
        etPresion = findViewById(R.id.etPresion);
        etVibracion = findViewById(R.id.etVibracion);
        etHorasTurno = findViewById(R.id.etHorasTurno);
        etObservaciones = findViewById(R.id.etObservaciones);
        tvDictamenVivo = findViewById(R.id.tvDictamenVivo);
        btnGuardarLectura = findViewById(R.id.btnGuardarLectura);

        listaOrdenesContainer = findViewById(R.id.listaOrdenesContainer);
        btnNuevaOrden = findViewById(R.id.btnNuevaOrden);

        tvClaveUnicaEmpresa = findViewById(R.id.tvClaveUnicaEmpresa);
        btnCopiarClave = findViewById(R.id.btnCopiarClave);
        listaSolicitudesContainer = findViewById(R.id.listaSolicitudesContainer);

        tvTotalTco = findViewById(R.id.tvTotalTco);
        tvDetalleFinanciero = findViewById(R.id.tvDetalleFinanciero);
        listaCostosContainer = findViewById(R.id.listaCostosContainer);
    }

    private void configurarHeader() {
        TextView tvEmpresa = findViewById(R.id.tvNombreEmpresaHeader);
        TextView tvUsuarioRol = findViewById(R.id.tvUsuarioRolHeader);
        tvEmpresa.setText(session.getNombreEmpresa());
        tvUsuarioRol.setText(session.getNombre() + " · " + rol);

        findViewById(R.id.btnRefrescar).setOnClickListener(v -> cargarDatosEmpresa());
        findViewById(R.id.btnSalir).setOnClickListener(v -> {
            session.cerrarSesion();
            volverAlLogin();
        });
    }

    private void configurarMenuPorRol() {
        Menu menu = bottomNav.getMenu();
        menu.findItem(R.id.nav_resumen).setVisible(true);
        menu.findItem(R.id.nav_monitoreo).setVisible(puedeMonitorear);
        menu.findItem(R.id.nav_ordenes).setVisible(puedeOrdenes);
        menu.findItem(R.id.nav_accesos).setVisible(esSupGeneral);
        menu.findItem(R.id.nav_costos).setVisible(esSupFinanciero);

        if (esSupProd || esSupGeneral) {
            btnAltaRapidaMaquina.setVisibility(View.VISIBLE);
        }

        if (esSupGeneral) {
            tvClaveUnicaEmpresa.setText(session.getCodigoEmpresa());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            mostrarVista(id);
            return true;
        });
    }

    private void mostrarVista(int menuId) {
        viewResumen.setVisibility(menuId == R.id.nav_resumen ? View.VISIBLE : View.GONE);
        viewMonitoreo.setVisibility(menuId == R.id.nav_monitoreo ? View.VISIBLE : View.GONE);
        viewOrdenes.setVisibility(menuId == R.id.nav_ordenes ? View.VISIBLE : View.GONE);
        viewAccesos.setVisibility(menuId == R.id.nav_accesos ? View.VISIBLE : View.GONE);
        viewCostos.setVisibility(menuId == R.id.nav_costos ? View.VISIBLE : View.GONE);
    }

    private void configurarEventos() {
        bannerSolicitudesPendientes.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_accesos));

        btnCopiarClave.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("Clave Empresa", session.getCodigoEmpresa()));
                Toast.makeText(this, "Clave Única copiada: " + session.getCodigoEmpresa(), Toast.LENGTH_SHORT).show();
            }
        });

        TextWatcher evaluador = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { evaluarUmbralesEnVivo(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etTemp.addTextChangedListener(evaluador);
        etPresion.addTextChangedListener(evaluador);
        etVibracion.addTextChangedListener(evaluador);

        btnGuardarLectura.setOnClickListener(v -> enviarLecturaRapida());
        btnNuevaOrden.setOnClickListener(v -> mostrarDialogoNuevaOrden());
        btnAltaRapidaMaquina.setOnClickListener(v -> mostrarDialogoAltaMaquina());
    }

    private void cargarDatosEmpresa() {
        progressTop.setVisibility(View.VISIBLE);
        try {
            String url = session.getServerUrl() + "/api/resumen?codigoEmpresa=" +
                    URLEncoder.encode(session.getCodigoEmpresa(), "UTF-8") +
                    "&rol=" + URLEncoder.encode(rol, "UTF-8");

            ApiClient.get(url, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject json) {
                    progressTop.setVisibility(View.GONE);
                    if (!json.optBoolean("ok", false)) {
                        Toast.makeText(MainActivity.this, json.optString("error", "Error al cargar datos"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    procesarRespuestaResumen(json);
                }

                @Override
                public void onError(String mensaje) {
                    progressTop.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            progressTop.setVisibility(View.GONE);
        }
    }

    private void procesarRespuestaResumen(JSONObject json) {
        JSONObject kpi = json.optJSONObject("kpi");
        if (kpi != null) {
            double ef = kpi.optDouble("eficiencia", 100.0);
            int total = kpi.optInt("totalEquipos", 0);
            int opt = kpi.optInt("optimos", 0);
            int alert = kpi.optInt("alertas", 0);
            int pendSol = kpi.optInt("solicitudesPendientes", 0);
            double cAdq = kpi.optDouble("totalCostoAdq", 0.0);
            double cMant = kpi.optDouble("totalCostoMant", 0.0);

            tvKpiEficiencia.setText(String.format(Locale.US, "%.1f%%", ef));
            tvKpiOptimos.setText(opt + " de " + total + " en rango óptimo");

            if (alert == 0) {
                tvKpiSeguridad.setText("SEGURO");
                tvKpiSeguridad.setTextColor(ContextCompat.getColor(this, R.color.badge_optimo_text));
                tvKpiAlertasDetalle.setText("Sin incidentes activos");
            } else {
                tvKpiSeguridad.setText("ALERTA (" + alert + ")");
                tvKpiSeguridad.setTextColor(ContextCompat.getColor(this, R.color.badge_alerta_text));
                tvKpiAlertasDetalle.setText(alert == 1 ? "1 equipo fuera de norma" : alert + " equipos fuera de norma");
            }

            if (esSupGeneral && pendSol > 0) {
                bannerSolicitudesPendientes.setVisibility(View.VISIBLE);
                tvBannerSolicitudes.setText("Tienes " + pendSol + " solicitud(es) pendiente(s) de aprobar");
            } else {
                bannerSolicitudesPendientes.setVisibility(View.GONE);
            }

            tvTotalTco.setText(String.format(Locale.US, "$%,.2f MXN", (cAdq + cMant)));
            tvDetalleFinanciero.setText(String.format(Locale.US, "Adquisición: $%,.2f · Mant.: $%,.2f", cAdq, cMant));
        }

        listaMaquinas.clear();
        JSONArray arrMaq = json.optJSONArray("maquinaria");
        if (arrMaq != null) {
            for (int i = 0; i < arrMaq.length(); i++) {
                JSONObject o = arrMaq.optJSONObject(i);
                if (o == null) continue;
                Maquina m = new Maquina();
                m.id = o.optString("id");
                m.nombre = o.optString("nombre");
                m.modelo = o.optString("modelo");
                m.anio = o.optString("anio");
                m.horas = o.optString("horas");
                m.fallas = o.optString("fallas");
                m.costoAdq = o.optString("costoAdq");
                m.costoMant = o.optString("costoMant");
                m.estado = o.optString("estado");
                m.fecha = o.optString("fecha");
                m.temp = o.optString("temp");
                m.pres = o.optString("pres");
                m.vib = o.optString("vib");
                listaMaquinas.add(m);
            }
        }

        listaOrdenes.clear();
        JSONArray arrOrd = json.optJSONArray("mantenimientos");
        if (arrOrd != null) {
            for (int i = 0; i < arrOrd.length(); i++) {
                JSONObject o = arrOrd.optJSONObject(i);
                if (o == null) continue;
                Mantenimiento mt = new Mantenimiento();
                mt.idMant = o.optString("idMant");
                mt.idMaq = o.optString("idMaq");
                mt.tipo = o.optString("tipo");
                mt.actividades = o.optString("act");
                mt.refacciones = o.optString("ref");
                mt.fecha = o.optString("fecha");
                mt.responsable = o.optString("tec");
                mt.alerta = o.optString("alerta");
                listaOrdenes.add(mt);
            }
        }

        listaSolicitudes.clear();
        JSONArray arrSol = json.optJSONArray("solicitudes");
        if (arrSol != null) {
            for (int i = 0; i < arrSol.length(); i++) {
                JSONObject o = arrSol.optJSONObject(i);
                if (o == null) continue;
                SolicitudAcceso s = new SolicitudAcceso();
                s.id = o.optString("id");
                s.nombre = o.optString("nombre");
                s.correo = o.optString("correo");
                s.rol = o.optString("rol");
                listaSolicitudes.add(s);
            }
        }

        pintarMaquinas();
        pintarSpinnerMonitoreo();
        pintarOrdenes();
        pintarSolicitudes();
        pintarCostos();
    }

    private void pintarMaquinas() {
        listaMaquinasContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (listaMaquinas.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aún no hay máquinas registradas en esta empresa.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.aurea_text_secondary));
            empty.setPadding(8, 24, 8, 24);
            listaMaquinasContainer.addView(empty);
            return;
        }

        for (Maquina m : listaMaquinas) {
            View card = inflater.inflate(R.layout.item_maquina, listaMaquinasContainer, false);
            TextView tvNombre = card.findViewById(R.id.tvMaqNombre);
            TextView tvModelo = card.findViewById(R.id.tvMaqModelo);
            TextView tvEstado = card.findViewById(R.id.tvMaqEstado);
            TextView tvTel = card.findViewById(R.id.tvMaqTelemetria);
            TextView tvDiag = card.findViewById(R.id.tvMaqDiagnostico);

            tvNombre.setText(m.nombre);
            tvModelo.setText(m.modelo + " (" + m.anio + ") · Horómetro: " + m.horas + " h");
            tvEstado.setText(m.estado);
            aplicarEstiloBadgeEstado(tvEstado, m.estado);

            tvTel.setText("Temp: " + m.temp + "°C  ·  Pres: " + m.pres + " psi  ·  Vibr: " + m.vib + " mm/s");
            tvDiag.setText("Diagnóstico: " + m.fallas + "  (" + m.fecha + ")");

            card.setOnClickListener(v -> mostrarAccionesRapidasMaquina(m));
            listaMaquinasContainer.addView(card);
        }
    }

    private void pintarCostos() {
        listaCostosContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Maquina m : listaMaquinas) {
            View card = inflater.inflate(R.layout.item_maquina, listaCostosContainer, false);
            TextView tvNombre = card.findViewById(R.id.tvMaqNombre);
            TextView tvModelo = card.findViewById(R.id.tvMaqModelo);
            TextView tvEstado = card.findViewById(R.id.tvMaqEstado);
            TextView tvTel = card.findViewById(R.id.tvMaqTelemetria);
            TextView tvDiag = card.findViewById(R.id.tvMaqDiagnostico);

            tvNombre.setText(m.nombre);
            tvModelo.setText(m.modelo + " · " + m.horas + " h");
            tvEstado.setText(m.estado);
            aplicarEstiloBadgeEstado(tvEstado, m.estado);

            tvTel.setText("Adquisición: $" + m.costoAdq + "  ·  Mant.: $" + m.costoMant);
            tvDiag.setText("Toque aquí para actualizar los costos de este activo");

            card.setOnClickListener(v -> mostrarDialogoCostos(m));
            listaCostosContainer.addView(card);
        }
    }

    private void pintarSpinnerMonitoreo() {
        List<String> items = new ArrayList<>();
        for (Maquina m : listaMaquinas) {
            items.add(m.id + " — " + m.modelo + " [" + m.estado + "]");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinnerEquipoMon.setAdapter(adapter);
    }

    private void pintarOrdenes() {
        listaOrdenesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (listaOrdenes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No hay órdenes de mantenimiento registradas.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.aurea_text_secondary));
            empty.setPadding(8, 24, 8, 24);
            listaOrdenesContainer.addView(empty);
            return;
        }

        for (Mantenimiento mt : listaOrdenes) {
            View card = inflater.inflate(R.layout.item_mantenimiento, listaOrdenesContainer, false);
            TextView tvEquipo = card.findViewById(R.id.tvOrdEquipoTipo);
            TextView tvAlerta = card.findViewById(R.id.tvOrdAlerta);
            TextView tvAct = card.findViewById(R.id.tvOrdActividades);
            TextView tvRef = card.findViewById(R.id.tvOrdRefacciones);
            MaterialButton btnConcluir = card.findViewById(R.id.btnConcluirOrden);

            tvEquipo.setText(mt.idMaq + " · " + mt.tipo);
            tvAlerta.setText(mt.alerta);
            tvAct.setText(mt.actividades);
            tvRef.setText("Refacciones: " + mt.refacciones + " · Fecha: " + mt.fecha);

            boolean completada = "Completado".equalsIgnoreCase(mt.alerta);
            aplicarEstiloBadgeAlerta(tvAlerta, mt.alerta);

            if (completada) {
                btnConcluir.setVisibility(View.GONE);
            } else {
                btnConcluir.setVisibility(View.VISIBLE);
                btnConcluir.setOnClickListener(v -> concluirOrdenRapida(mt));
            }

            listaOrdenesContainer.addView(card);
        }
    }

    private void pintarSolicitudes() {
        listaSolicitudesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (listaSolicitudes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No hay solicitudes de acceso pendientes en este momento.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.aurea_text_secondary));
            empty.setPadding(8, 20, 8, 20);
            listaSolicitudesContainer.addView(empty);
            return;
        }

        for (SolicitudAcceso sol : listaSolicitudes) {
            View card = inflater.inflate(R.layout.item_solicitud, listaSolicitudesContainer, false);
            TextView tvNom = card.findViewById(R.id.tvSolNombre);
            TextView tvCor = card.findViewById(R.id.tvSolCorreo);
            TextView tvRol = card.findViewById(R.id.tvSolRol);
            MaterialButton btnAprobar = card.findViewById(R.id.btnAprobarSol);
            MaterialButton btnRechazar = card.findViewById(R.id.btnRechazarSol);

            tvNom.setText(sol.nombre);
            tvCor.setText(sol.correo);
            tvRol.setText("Rol solicitado: " + sol.rol);

            btnAprobar.setOnClickListener(v -> resolverSolicitud(sol, "aprobar"));
            btnRechazar.setOnClickListener(v -> resolverSolicitud(sol, "rechazar"));

            listaSolicitudesContainer.addView(card);
        }
    }

    private void evaluarUmbralesEnVivo() {
        String tStr = etTemp.getText() != null ? etTemp.getText().toString().trim() : "";
        String pStr = etPresion.getText() != null ? etPresion.getText().toString().trim() : "";
        String vStr = etVibracion.getText() != null ? etVibracion.getText().toString().trim() : "";

        if (tStr.isEmpty() && pStr.isEmpty() && vStr.isEmpty()) {
            tvDictamenVivo.setBackgroundResource(R.drawable.bg_badge_programado);
            tvDictamenVivo.setTextColor(ContextCompat.getColor(this, R.color.badge_programado_text));
            tvDictamenVivo.setText("Ingrese parámetros para evaluar el estado en tiempo real.");
            return;
        }

        List<String> fallas = new ArrayList<>();
        try { if (!tStr.isEmpty() && Double.parseDouble(tStr) > 80.0) fallas.add("Temperatura alta"); } catch (Exception ignored) {}
        try {
            if (!pStr.isEmpty()) {
                double p = Double.parseDouble(pStr);
                if (p < 60.0 || p > 150.0) fallas.add("Presión fuera de rango");
            }
        } catch (Exception ignored) {}
        try { if (!vStr.isEmpty() && Double.parseDouble(vStr) > 6.5) fallas.add("Vibración excesiva"); } catch (Exception ignored) {}

        if (fallas.isEmpty()) {
            tvDictamenVivo.setBackgroundResource(R.drawable.bg_badge_optimo);
            tvDictamenVivo.setTextColor(ContextCompat.getColor(this, R.color.badge_optimo_text));
            tvDictamenVivo.setText("✓ Parámetros dentro de norma (Óptimo)");
        } else {
            tvDictamenVivo.setBackgroundResource(R.drawable.bg_badge_alerta);
            tvDictamenVivo.setTextColor(ContextCompat.getColor(this, R.color.badge_alerta_text));
            tvDictamenVivo.setText("⚠ Alerta detectada: " + String.join(", ", fallas));
        }
    }

    private void enviarLecturaRapida() {
        if (listaMaquinas.isEmpty()) {
            Toast.makeText(this, "No hay máquinas disponibles.", Toast.LENGTH_SHORT).show();
            return;
        }
        int pos = spinnerEquipoMon.getSelectedItemPosition();
        if (pos < 0 || pos >= listaMaquinas.size()) return;
        Maquina m = listaMaquinas.get(pos);

        String temp = etTemp.getText() != null ? etTemp.getText().toString().trim() : "";
        String pres = etPresion.getText() != null ? etPresion.getText().toString().trim() : "";
        String vib = etVibracion.getText() != null ? etVibracion.getText().toString().trim() : "";
        String horas = etHorasTurno.getText() != null ? etHorasTurno.getText().toString().trim() : "0";
        String obs = etObservaciones.getText() != null ? etObservaciones.getText().toString().trim() : "";

        if (temp.isEmpty() || pres.isEmpty() || vib.isEmpty()) {
            Toast.makeText(this, "Capture temperatura, presión y vibración.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressTop.setVisibility(View.VISIBLE);
        btnGuardarLectura.setEnabled(false);

        Map<String, String> params = new HashMap<>();
        params.put("codigoEmpresa", session.getCodigoEmpresa());
        params.put("idMaq", m.id);
        params.put("temperatura", temp);
        params.put("presion", pres);
        params.put("vibracion", vib);
        params.put("horasTurno", horas.isEmpty() ? "0" : horas);
        params.put("observaciones", obs);
        params.put("rol", rol);

        ApiClient.post(session.getServerUrl() + "/api/monitoreo", params, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                btnGuardarLectura.setEnabled(true);
                progressTop.setVisibility(View.GONE);
                if (json.optBoolean("ok", false)) {
                    Toast.makeText(MainActivity.this, json.optString("mensaje", "Lectura guardada"), Toast.LENGTH_LONG).show();
                    etTemp.setText("");
                    etPresion.setText("");
                    etVibracion.setText("");
                    etHorasTurno.setText("0");
                    etObservaciones.setText("");
                    cargarDatosEmpresa();
                } else {
                    Toast.makeText(MainActivity.this, json.optString("error", "Error al guardar"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String mensaje) {
                btnGuardarLectura.setEnabled(true);
                progressTop.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarAccionesRapidasMaquina(Maquina m) {
        List<String> opciones = new ArrayList<>();
        if (puedeMonitorear) opciones.add("Registrar lectura rápida");
        if (esSupProd || esSupGeneral) opciones.add("Cambiar estado operativo");
        if (esSupFinanciero || esSupGeneral) opciones.add("Actualizar costos ($ MXN)");

        if (opciones.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle(m.nombre)
                .setItems(opciones.toArray(new String[0]), (d, which) -> {
                    String sel = opciones.get(which);
                    if (sel.startsWith("Registrar")) {
                        int idx = listaMaquinas.indexOf(m);
                        if (idx >= 0) spinnerEquipoMon.setSelection(idx);
                        bottomNav.setSelectedItemId(R.id.nav_monitoreo);
                    } else if (sel.startsWith("Cambiar")) {
                        mostrarDialogoCambiarEstado(m);
                    } else if (sel.startsWith("Actualizar")) {
                        mostrarDialogoCostos(m);
                    }
                })
                .show();
    }

    private void mostrarDialogoCambiarEstado(Maquina m) {
        String[] estados = {"Óptimo", "En Mantenimiento", "Alerta", "Inactiva"};
        new AlertDialog.Builder(this)
                .setTitle("Estado de " + m.id)
                .setItems(estados, (d, which) -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("accion", "cambiarEstado");
                    params.put("codigoEmpresa", session.getCodigoEmpresa());
                    params.put("idMaq", m.id);
                    params.put("estado", estados[which]);
                    ejecutarAccionPost("/api/maquinaria", params);
                })
                .show();
    }

    private void mostrarDialogoCostos(Maquina m) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(52, 24, 52, 8);

        EditText etAdq = new EditText(this);
        etAdq.setHint("Costo de adquisición ($ MXN)");
        etAdq.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAdq.setText(m.costoAdq);
        layout.addView(etAdq);

        EditText etMant = new EditText(this);
        etMant.setHint("Costo de mantenimiento ($ MXN)");
        etMant.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etMant.setText(m.costoMant);
        layout.addView(etMant);

        new AlertDialog.Builder(this)
                .setTitle("Costos: " + m.id)
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("accion", "actualizarCostos");
                    params.put("codigoEmpresa", session.getCodigoEmpresa());
                    params.put("idMaq", m.id);
                    params.put("costoAdq", etAdq.getText().toString().trim());
                    params.put("costoMant", etMant.getText().toString().trim());
                    ejecutarAccionPost("/api/maquinaria", params);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoAltaMaquina() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(52, 24, 52, 8);

        EditText etId = new EditText(this); etId.setHint("Código (Ej. PR-500)"); layout.addView(etId);
        EditText etNom = new EditText(this); etNom.setHint("Nombre del equipo"); layout.addView(etNom);
        EditText etMod = new EditText(this); etMod.setHint("Modelo"); layout.addView(etMod);
        EditText etAnio = new EditText(this); etAnio.setHint("Año (Ej. 2026)"); etAnio.setInputType(InputType.TYPE_CLASS_NUMBER); etAnio.setText("2026"); layout.addView(etAnio);
        EditText etHor = new EditText(this); etHor.setHint("Horas iniciales"); etHor.setInputType(InputType.TYPE_CLASS_NUMBER); etHor.setText("0"); layout.addView(etHor);

        new AlertDialog.Builder(this)
                .setTitle("Alta Rápida de Máquina")
                .setView(layout)
                .setPositiveButton("Registrar", (d, w) -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("accion", "registrar");
                    params.put("codigoEmpresa", session.getCodigoEmpresa());
                    params.put("id", etId.getText().toString().trim());
                    params.put("nombre", etNom.getText().toString().trim());
                    params.put("modelo", etMod.getText().toString().trim());
                    params.put("anio", etAnio.getText().toString().trim());
                    params.put("horas", etHor.getText().toString().trim());
                    params.put("estado", "Óptimo");
                    ejecutarAccionPost("/api/maquinaria", params);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoNuevaOrden() {
        if (listaMaquinas.isEmpty()) {
            Toast.makeText(this, "No hay equipos registrados.", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(52, 24, 52, 8);

        Spinner spEq = new Spinner(this);
        List<String> cods = new ArrayList<>();
        for (Maquina m : listaMaquinas) cods.add(m.id);
        spEq.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cods));
        layout.addView(spEq);

        Spinner spTipo = new Spinner(this);
        spTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Preventivo", "Correctivo"}));
        layout.addView(spTipo);

        EditText etAct = new EditText(this); etAct.setHint("Actividades a realizar"); layout.addView(etAct);
        EditText etRef = new EditText(this); etRef.setHint("Refacciones / insumos"); layout.addView(etRef);
        EditText etFecha = new EditText(this); etFecha.setHint("Fecha (AAAA-MM-DD)");
        etFecha.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        layout.addView(etFecha);

        new AlertDialog.Builder(this)
                .setTitle("Nueva Orden de Trabajo")
                .setView(layout)
                .setPositiveButton("Crear Orden", (d, w) -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("accion", "registrar");
                    params.put("codigoEmpresa", session.getCodigoEmpresa());
                    params.put("idMaq", spEq.getSelectedItem().toString());
                    params.put("tipo", spTipo.getSelectedItem().toString());
                    params.put("actividades", etAct.getText().toString().trim());
                    params.put("refacciones", etRef.getText().toString().trim());
                    params.put("fecha", etFecha.getText().toString().trim());
                    params.put("rol", rol);
                    ejecutarAccionPost("/api/mantenimiento", params);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void concluirOrdenRapida(Mantenimiento mt) {
        Map<String, String> params = new HashMap<>();
        params.put("accion", "completar");
        params.put("codigoEmpresa", session.getCodigoEmpresa());
        params.put("idMant", mt.idMant);
        params.put("idMaq", mt.idMaq);
        ejecutarAccionPost("/api/mantenimiento", params);
    }

    private void resolverSolicitud(SolicitudAcceso sol, String accion) {
        Map<String, String> params = new HashMap<>();
        params.put("accion", accion);
        params.put("codigoEmpresa", session.getCodigoEmpresa());
        params.put("idUsuario", sol.id);
        params.put("rol", sol.rol);
        ejecutarAccionPost("/api/solicitudes", params);
    }

    private void ejecutarAccionPost(String endpoint, Map<String, String> params) {
        progressTop.setVisibility(View.VISIBLE);
        ApiClient.post(session.getServerUrl() + endpoint, params, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                progressTop.setVisibility(View.GONE);
                if (json.optBoolean("ok", false)) {
                    Toast.makeText(MainActivity.this, json.optString("mensaje", "Operación exitosa"), Toast.LENGTH_SHORT).show();
                    cargarDatosEmpresa();
                } else {
                    Toast.makeText(MainActivity.this, json.optString("error", "No se pudo completar"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String mensaje) {
                progressTop.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void aplicarEstiloBadgeEstado(TextView tv, String estado) {
        if ("Óptimo".equalsIgnoreCase(estado) || "Activa".equalsIgnoreCase(estado)) {
            tv.setBackgroundResource(R.drawable.bg_badge_optimo);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_optimo_text));
        } else if ("Alerta".equalsIgnoreCase(estado) || "En Mantenimiento".equalsIgnoreCase(estado)) {
            tv.setBackgroundResource(R.drawable.bg_badge_alerta);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_alerta_text));
        } else {
            tv.setBackgroundResource(R.drawable.bg_badge_inactiva);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_inactiva_text));
        }
    }

    private void aplicarEstiloBadgeAlerta(TextView tv, String alerta) {
        if ("Completado".equalsIgnoreCase(alerta)) {
            tv.setBackgroundResource(R.drawable.bg_badge_optimo);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_optimo_text));
        } else if ("Vencido".equalsIgnoreCase(alerta)) {
            tv.setBackgroundResource(R.drawable.bg_badge_inactiva);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_inactiva_text));
        } else if (alerta != null && (alerta.contains("Próximo") || "Urgente".equalsIgnoreCase(alerta))) {
            tv.setBackgroundResource(R.drawable.bg_badge_alerta);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_alerta_text));
        } else {
            tv.setBackgroundResource(R.drawable.bg_badge_programado);
            tv.setTextColor(ContextCompat.getColor(this, R.color.badge_programado_text));
        }
    }

    private void volverAlLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
