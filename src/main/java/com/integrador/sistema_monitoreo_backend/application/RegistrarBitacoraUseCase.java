package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.Bitacora;
import com.integrador.sistema_monitoreo_backend.domain.BitacoraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RegistrarBitacoraUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarBitacoraUseCase.class);

    private final BitacoraRepository bitacoraRepository;

    public RegistrarBitacoraUseCase(BitacoraRepository bitacoraRepository) {
        this.bitacoraRepository = bitacoraRepository;
    }

    public void registrar(String usuario, String rol, String modulo, String accion, String descripcion,
                          String metodo, String ruta, Integer estadoHttp, String ip) {
        try {
            Bitacora registro = new Bitacora();
            registro.setFechaHora(LocalDateTime.now());
            registro.setUsuario(recortar(usuario == null || usuario.isBlank() ? "anonimo" : usuario, 100));
            registro.setRol(recortar(rol, 30));
            registro.setModulo(recortar(modulo, 40));
            registro.setAccion(recortar(accion, 30));
            registro.setDescripcion(recortar(descripcion, 200));
            registro.setMetodo(recortar(metodo, 10));
            registro.setRuta(recortar(ruta, 200));
            registro.setEstadoHttp(estadoHttp);
            registro.setResultado(estadoHttp != null && estadoHttp >= 400 ? "ERROR" : "EXITO");
            registro.setIp(recortar(ip, 60));
            bitacoraRepository.guardar(registro);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo registrar en la bitácora: {}", e.getMessage());
        }
    }

    private String recortar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
