package com.integrador.sistema_monitoreo_backend.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface BitacoraRepository {

    record Filtro(String texto, String modulo, String resultado, LocalDateTime desde, LocalDateTime hasta) {}

    record Pagina(List<Bitacora> items, long total) {}

    Bitacora guardar(Bitacora registro);

    Pagina buscar(Filtro filtro, int pagina, int tamano);

    long contarDesde(LocalDateTime desde);

    long contarPorResultadoDesde(String resultado, LocalDateTime desde);

    long contarPorAccionDesde(String accion, LocalDateTime desde);

    long contarUsuariosActivosDesde(LocalDateTime desde);
}