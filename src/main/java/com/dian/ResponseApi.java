package com.dian;

import lombok.Data;

import java.time.Instant;

@Data
public class ResponseApi {
    private String mensaje;
    private String codigo;
    private String timestamp;
    private String path;
    private int status;
    private boolean succes;
    private ResponseApiError error;

    public ResponseApi() {}

    public ResponseApi(String mensaje, String codigo) {
        this.mensaje = mensaje;
        this.codigo = codigo;
        this.timestamp = Instant.now().toString();
    }
}

