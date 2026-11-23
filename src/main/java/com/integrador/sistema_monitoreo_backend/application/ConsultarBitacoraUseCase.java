package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.Bitacora;
import com.integrador.sistema_monitoreo_backend.domain.BitacoraRepository;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultarBitacoraUseCase {

    public record RegistroVista(Integer id, String fechaHora, String usuario, String rol, String modulo,
                                String accion, String descripcion, String metodo, String ruta,
                                Integer estadoHttp, String resultado, String ip) {}

    public record PaginaVista(List<RegistroVista> items, int pagina, int tamano, long total, int totalPaginas) {}

    public record ResumenVista(long registrosHoy, long erroresUltimas24h, long sesionesFallidasUltimas24h,
                               long usuariosActivosHoy) {}

    private final BitacoraRepository bitacoraRepository;

    public ConsultarBitacoraUseCase(BitacoraRepository bitacoraRepository) {
        this.bitacoraRepository = bitacoraRepository;
    }

    @Transactional(readOnly = true)
    public PaginaVista buscar(String texto, String modulo, String resultado, LocalDate desde, LocalDate hasta,
                              int pagina, int tamano) {
        if (resultado != null && !resultado.isBlank() && !List.of("EXITO", "ERROR").contains(resultado)) {
            throw new ReglaNegocioException("El resultado debe ser EXITO o ERROR");
        }
        if (texto != null && texto.length() > 100) {
            throw new ReglaNegocioException("La búsqueda admite hasta 100 caracteres");
        }
        int paginaSegura = Math.max(0, pagina);
        int tamanoSeguro = Math.min(500, Math.max(1, tamano));

        var filtro = new BitacoraRepository.Filtro(
                texto, modulo, resultado,
                desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay());
        var resultadoBusqueda = bitacoraRepository.buscar(filtro, paginaSegura, tamanoSeguro);

        int totalPaginas = (int) Math.max(1, Math.ceil(resultadoBusqueda.total() / (double) tamanoSeguro));
        return new PaginaVista(resultadoBusqueda.items().stream().map(this::aVista).toList(),
                paginaSegura, tamanoSeguro, resultadoBusqueda.total(), totalPaginas);
    }

    @Transactional(readOnly = true)
    public ResumenVista resumen() {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime hace24h = LocalDateTime.now().minusHours(24);
        return new ResumenVista(
                bitacoraRepository.contarDesde(inicioHoy),
                bitacoraRepository.contarPorResultadoDesde("ERROR", hace24h),
                bitacoraRepository.contarPorAccionDesde("SESION_FALLIDA", hace24h),
                bitacoraRepository.contarUsuariosActivosDesde(inicioHoy));
    }

    private RegistroVista aVista(Bitacora b) {
        return new RegistroVista(b.getId(), b.getFechaHora().toString(), b.getUsuario(), b.getRol(), b.getModulo(),
                b.getAccion(), b.getDescripcion(), b.getMetodo(), b.getRuta(), b.getEstadoHttp(),
                b.getResultado(), b.getIp());
    }
}