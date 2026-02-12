package com.ajeitai.backend.infra.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Cliente HTTP mínimo para integração com o Object Storage da Magalu Cloud,
 * que expõe uma API S3-compatível e autenticação por API Key.
 *
 * Este cliente assume o modelo:
 *   - Endpoint base configurável (ex.: https://object.magalucloud.com)
 *   - Path-style: {endpoint}/{bucket}/{key}
 *   - Header de autenticação configurável (ex.: X-API-Key: <key>)
 */
public class MagaluS3HttpClient {

    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;
    private final String apiKeyHeader;

    public MagaluS3HttpClient(String endpoint, String apiKey, String apiKeyHeader) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Endpoint do Object Storage Magalu não pode ser vazio.");
        }
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.apiKey = apiKey;
        this.apiKeyHeader = (apiKeyHeader == null || apiKeyHeader.isBlank()) ? "X-API-Key" : apiKeyHeader;
        this.httpClient = HttpClient.newHttpClient();
    }

    private String buildUrl(String bucket, String key) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket não pode ser vazio.");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key não pode ser vazia.");
        }
        // Assumimos path-style: {endpoint}/{bucket}/{key}
        return endpoint + "/" + bucket + "/" + key;
    }

    public void putObject(String bucket, String key, InputStream data, long contentLength, String contentType) throws IOException {
        String url = buildUrl(bucket, key);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> data));

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header(apiKeyHeader, apiKey);
        }
        if (contentType != null && !contentType.isBlank()) {
            builder.header("Content-Type", contentType);
        }

        HttpResponse<Void> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrompido ao enviar requisição para Magalu Object Storage", e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new IOException("Falha ao fazer upload para Magalu Object Storage. HTTP " + response.statusCode());
        }
    }

    public InputStream getObject(String bucket, String key) throws IOException {
        String url = buildUrl(bucket, key);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header(apiKeyHeader, apiKey);
        }

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrompido ao baixar objeto do Magalu Object Storage", e);
        }

        int status = response.statusCode();
        if (status == 404) {
            // Não encontrado — deixamos o StorageService decidir como tratar (normalmente retorna null)
            return null;
        }
        if (status / 100 != 2) {
            throw new IOException("Falha ao obter objeto do Magalu Object Storage. HTTP " + status);
        }

        return response.body();
    }

    public void deleteObject(String bucket, String key) throws IOException {
        String url = buildUrl(bucket, key);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header(apiKeyHeader, apiKey);
        }

        HttpResponse<Void> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrompido ao excluir objeto do Magalu Object Storage", e);
        }

        if (response.statusCode() / 100 != 2 && response.statusCode() != 404) {
            throw new IOException("Falha ao excluir objeto do Magalu Object Storage. HTTP " + response.statusCode());
        }
    }
}

