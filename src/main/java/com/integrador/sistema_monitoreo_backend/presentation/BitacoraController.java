package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.ConsultarBitacoraUseCase;
import com.integrador.sistema_monitoreo_backend.application.ConsultarBitacoraUseCase.PaginaVista;
import com.integrador.sistema_monitoreo_backend.application.ConsultarBitacoraUseCase.ResumenVista;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/bitacora")
public class BitacoraController {

    private final ConsultarBitacoraUseCase useCase;

    public BitacoraController(ConsultarBitacoraUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public PaginaVista buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {
        return useCase.buscar(texto, modulo, resultado, desde, hasta, pagina, tamano);
    }

    @GetMapping("/resumen")
    public ResumenVista resumen() {
        return useCase.resumen();
    }
}