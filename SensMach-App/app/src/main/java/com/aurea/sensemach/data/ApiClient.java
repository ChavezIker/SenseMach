package com.aurea.sensemach.data;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// cliente http ligero conectado al ApiServlet de SenseMach (misma base de datos sensemac_db)
public class ApiClient {

    public interface ApiCallback {
        void onSuccess(JSONObject json);
        void onError(String mensaje);
    }

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void get(String fullUrl, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(fullUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String body = leerRespuesta(conn);
                JSONObject json = new JSONObject(body);
                mainHandler.post(() -> callback.onSuccess(json));
            } catch (Exception e) {
                String msg = "Error de conexión con el servidor. Verifique Internet o el servidor configurado.";
                mainHandler.post(() -> callback.onError(msg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static void post(String fullUrl, Map<String, String> params, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(fullUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                StringBuilder form = new StringBuilder();
                boolean first = true;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (!first) form.append("&");
                    first = false;
                    form.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    form.append("=");
                    form.append(URLEncoder.encode(entry.getValue() != null ? entry.getValue() : "", "UTF-8"));
                }

                byte[] bytes = form.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }

                String body = leerRespuesta(conn);
                JSONObject json = new JSONObject(body);
                mainHandler.post(() -> callback.onSuccess(json));
            } catch (Exception e) {
                String msg = "No se pudo conectar al servidor. Verifique que esté encendido y con acceso a red.";
                mainHandler.post(() -> callback.onError(msg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String leerRespuesta(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "{\"ok\":false,\"error\":\"Respuesta vacía del servidor\"}";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
