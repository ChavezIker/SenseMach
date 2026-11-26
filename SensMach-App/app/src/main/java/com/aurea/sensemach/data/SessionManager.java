package com.aurea.sensemach.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "sensemach_mobile_prefs";
    public static final String DEFAULT_SERVER = "https://sensemach-aurea.azurewebsites.net";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void guardarSesion(String id, String nombre, String correo, String rol,
                              String codigoEmpresa, String nombreEmpresa, String sectorEmpresa) {
        prefs.edit()
                .putBoolean("logueado", true)
                .putString("id", id)
                .putString("nombre", nombre)
                .putString("correo", correo)
                .putString("rol", rol)
                .putString("codigoEmpresa", codigoEmpresa)
                .putString("nombreEmpresa", nombreEmpresa)
                .putString("sectorEmpresa", sectorEmpresa)
                .apply();
    }

    public boolean estaLogueado() {
        return prefs.getBoolean("logueado", false) && !getCodigoEmpresa().isEmpty();
    }

    public void cerrarSesion() {
        String server = getServerUrl();
        prefs.edit().clear().putString("serverUrl", server).apply();
    }

    public void setServerUrl(String url) {
        if (url != null) {
            String clean = url.trim();
            while (clean.endsWith("/")) {
                clean = clean.substring(0, clean.length() - 1);
            }
            prefs.edit().putString("serverUrl", clean).apply();
        }
    }

    public String getServerUrl() {
        return prefs.getString("serverUrl", DEFAULT_SERVER);
    }

    public String getNombre() { return prefs.getString("nombre", ""); }
    public String getCorreo() { return prefs.getString("correo", ""); }
    public String getRol() { return prefs.getString("rol", ""); }
    public String getCodigoEmpresa() { return prefs.getString("codigoEmpresa", ""); }
    public String getNombreEmpresa() { return prefs.getString("nombreEmpresa", ""); }
    public String getSectorEmpresa() { return prefs.getString("sectorEmpresa", ""); }
}
