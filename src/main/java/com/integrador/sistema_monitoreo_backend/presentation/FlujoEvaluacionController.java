package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.FlujoEvaluacionUseCase;
import com.integrador.sistema_monitoreo_backend.application.FlujoEvaluacionUseCase.DetalleEvaluacion;
import com.integrador.sistema_monitoreo_backend.application.FlujoEvaluacionUseCase.ResumenEvaluacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class FlujoEvaluacionController {

    public record AsignarRequest(@NotNull(message = "Selecciona un test") @Positive Integer instrumentoId) {}

    public record RespuestaEnvio(
            @NotNull @Positive Integer preguntaId,
            @NotNull @Min(value = 0, message = "La respuesta debe estar entre 0 y 3")
            @Max(value = 3, message = "La respuesta debe estar entre 0 y 3") Integer valor) {}

    public record ResponderRequest(
            @NotEmpty(message = "Faltan las respuestas") @Size(max = 100, message = "Demasiadas respuestas")
            @Valid List<RespuestaEnvio> respuestas,
            @NotNull(message = "Falta el número de cambios")
            @Min(value = 0, message = "Cambios no válidos")
            @Max(value = 2, message = "Solo se permite cambiar hasta 2 respuestas") Integer cambios,
            @Size(max = 500, message = "Las observaciones admiten hasta 500 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String observaciones) {}

    public record AprobarRequest(
            @Size(max = 500, message = "El comentario admite hasta 500 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String comentario) {}

    private final FlujoEvaluacionUseCase useCase;

    public FlujoEvaluacionController(FlujoEvaluacionUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/evaluaciones")
    public List<ResumenEvaluacion> listar(Authentication auth) {
        return useCase.listar(auth.getName(), rol(auth));
    }

    @GetMapping("/evaluaciones/{id}")
    public DetalleEvaluacion detalle(@PathVariable Integer id, Authentication auth) {
        return useCase.detalle(id, auth.getName(), rol(auth));
    }

    @PostMapping("/pacientes/{pacienteId}/evaluaciones/asignar")
    @ResponseStatus(HttpStatus.CREATED)
    public DetalleEvaluacion asignar(@PathVariable Integer pacienteId,
                                     @Valid @RequestBody AsignarRequest r, Authentication auth) {
        return useCase.asignar(pacienteId, r.instrumentoId(), auth.getName(), rol(auth));
    }

    @PostMapping("/evaluaciones/{id}/responder")
    public DetalleEvaluacion responder(@PathVariable Integer id,
                                       @Valid @RequestBody ResponderRequest r, Authentication auth) {
        Map<Integer, Integer> respuestas = r.respuestas().stream()
                .collect(Collectors.toMap(RespuestaEnvio::preguntaId, RespuestaEnvio::valor, (a, b) -> b));
        return useCase.responder(id, respuestas, r.cambios(), r.observaciones(), auth.getName(), rol(auth));
    }

    @PostMapping("/evaluaciones/{id}/aprobar")
    public DetalleEvaluacion aprobar(@PathVariable Integer id,
                                     @Valid @RequestBody AprobarRequest r, Authentication auth) {
        return useCase.aprobar(id, r.comentario(), auth.getName(), rol(auth));
    }

    @DeleteMapping("/evaluaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Integer id, Authentication auth) {
        useCase.cancelar(id, auth.getName(), rol(auth));
    }

    private String rol(Authentication auth) {
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}