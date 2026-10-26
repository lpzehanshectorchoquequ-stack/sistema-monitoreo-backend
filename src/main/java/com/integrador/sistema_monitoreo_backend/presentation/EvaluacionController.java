package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.GestionarEvaluacionesUseCase;
import com.integrador.sistema_monitoreo_backend.application.GestionarEvaluacionesUseCase.EvaluacionVista;
import com.integrador.sistema_monitoreo_backend.application.GestionarEvaluacionesUseCase.InstrumentoVista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EvaluacionController {

    public record RespuestaRequest(@NotNull Integer preguntaId,
                                   @NotNull @Min(0) @Max(3) Integer valor) {}

    public record EvaluacionRequest(@NotNull Integer instrumentoId,
                                    @NotEmpty @Valid List<RespuestaRequest> respuestas,
                                    String observaciones) {}

    private final GestionarEvaluacionesUseCase useCase;

    public EvaluacionController(GestionarEvaluacionesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/instrumentos")
    public List<InstrumentoVista> instrumentos() {
        return useCase.listarInstrumentos();
    }

    @GetMapping("/instrumentos/{id}")
    public InstrumentoVista instrumento(@PathVariable Integer id) {
        return useCase.obtenerInstrumento(id);
    }

    @PostMapping("/pacientes/{pacienteId}/evaluaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluacionVista aplicar(@PathVariable Integer pacienteId,
                                   @Valid @RequestBody EvaluacionRequest request,
                                   Authentication auth) {
        Map<Integer, Integer> respuestas = request.respuestas().stream()
                .collect(Collectors.toMap(RespuestaRequest::preguntaId, RespuestaRequest::valor, (a, b) -> b));
        return useCase.aplicar(pacienteId, request.instrumentoId(), respuestas,
                request.observaciones(), auth.getName(), rol(auth));
    }

    @GetMapping("/pacientes/{pacienteId}/evaluaciones")
    public List<EvaluacionVista> historial(@PathVariable Integer pacienteId, Authentication auth) {
        return useCase.historial(pacienteId, auth.getName(), rol(auth));
    }

    private String rol(Authentication auth) {
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}