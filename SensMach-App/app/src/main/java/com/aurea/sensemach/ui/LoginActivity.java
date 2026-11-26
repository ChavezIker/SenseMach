package com.aurea.sensemach.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.aurea.sensemach.R;
import com.aurea.sensemach.data.ApiClient;
import com.aurea.sensemach.data.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private SessionManager session;
    private TextInputEditText etCorreo, etPassword, etCodigoEmpresa;
    private TextView tvErrorLogin, tvServidorActual;
    private MaterialButton btnLogin, btnConfigServidor;
    private ProgressBar progressLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        if (session.estaLogueado()) {
            irAlPanelPrincipal();
            return;
        }

        setContentView(R.layout.activity_login);

        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        etCodigoEmpresa = findViewById(R.id.etCodigoEmpresa);
        tvErrorLogin = findViewById(R.id.tvErrorLogin);
        tvServidorActual = findViewById(R.id.tvServidorActual);
        btnLogin = findViewById(R.id.btnLogin);
        btnConfigServidor = findViewById(R.id.btnConfigServidor);
        progressLogin = findViewById(R.id.progressLogin);

        actualizarTextoServidor();

        btnLogin.setOnClickListener(v -> intentarLogin());
        btnConfigServidor.setOnClickListener(v -> mostrarSelectorServidor());
    }

    private void actualizarTextoServidor() {
        tvServidorActual.setText("Conectado a: " + session.getServerUrl());
    }

    private void intentarLogin() {
        String correo = etCorreo.getText() != null ? etCorreo.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String claveEmp = etCodigoEmpresa.getText() != null ? etCodigoEmpresa.getText().toString().trim().toUpperCase() : "";

        if (correo.isEmpty() || password.isEmpty() || claveEmp.isEmpty()) {
            mostrarError("Ingrese los 3 datos: correo, contraseña personal y clave única de empresa.");
            return;
        }

        tvErrorLogin.setVisibility(View.GONE);
        btnLogin.setEnabled(false);
        progressLogin.setVisibility(View.VISIBLE);

        Map<String, String> params = new HashMap<>();
        params.put("correo", correo);
        params.put("password", password);
        params.put("codigoEmpresa", claveEmp);

        String url = session.getServerUrl() + "/api/login";
        ApiClient.post(url, params, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                btnLogin.setEnabled(true);
                progressLogin.setVisibility(View.GONE);

                if (json.optBoolean("ok", false)) {
                    JSONObject u = json.optJSONObject("usuario");
                    if (u != null) {
                        session.guardarSesion(
                                u.optString("id"),
                                u.optString("nombre"),
                                u.optString("correo"),
                                u.optString("rol"),
                                u.optString("codigoEmpresa"),
                                u.optString("nombreEmpresa"),
                                u.optString("sectorEmpresa")
                        );
                        irAlPanelPrincipal();
                        return;
                    }
                }
                mostrarError(json.optString("error", "No fue posible iniciar sesión."));
            }

            @Override
            public void onError(String mensaje) {
                btnLogin.setEnabled(true);
                progressLogin.setVisibility(View.GONE);
                mostrarError(mensaje);
            }
        });
    }

    private void mostrarError(String mensaje) {
        tvErrorLogin.setText(mensaje);
        tvErrorLogin.setVisibility(View.VISIBLE);
    }

    private void mostrarSelectorServidor() {
        final String[] opciones = {
                "Azure App Service (sensemach-aurea)",
                "Azure VM (20.240.199.77)",
                "Emulador Local (10.0.2.2:8080)",
                "Escribir URL personalizada..."
        };
        final String[] urls = {
                "https://sensemach-aurea.azurewebsites.net",
                "http://20.240.199.77",
                "http://10.0.2.2:8080/SenseMach"
        };

        new AlertDialog.Builder(this)
                .setTitle("Servidor de SenseMach")
                .setItems(opciones, (dialog, which) -> {
                    if (which < 3) {
                        session.setServerUrl(urls[which]);
                        actualizarTextoServidor();
                        Toast.makeText(this, "Servidor actualizado", Toast.LENGTH_SHORT).show();
                    } else {
                        mostrarDialogoUrlPersonalizada();
                    }
                })
                .show();
    }

    private void mostrarDialogoUrlPersonalizada() {
        EditText input = new EditText(this);
        input.setText(session.getServerUrl());
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("URL del Servidor")
                .setView(input)
                .setPositiveButton("Guardar", (d, w) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        session.setServerUrl(val);
                        actualizarTextoServidor();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void irAlPanelPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
