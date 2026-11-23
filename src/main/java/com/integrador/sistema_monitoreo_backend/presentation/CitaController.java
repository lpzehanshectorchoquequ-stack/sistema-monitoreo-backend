package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.GestionarCitasUseCase;
import com.integrador.sistema_monitoreo_backend.application.GestionarCitasUseCase.CitaVista;
import com.integrador.sistema_monitoreo_backend.application.GestionarCitasUseCase.EspacioVista;
import com.integrador.sistema_monitoreo_backend.application.GestionarCitasUseCase.MisHorariosVista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CitaController {

    private static final String HORA = "^([01]\\d|2[0-3]):[0-5]\\d$";

    public record HorarioRequest(
            @NotNull(message = "Elige el día") @Min(value = 1, message = "Día no válido")
            @Max(value = 7, message = "Día no válido") Integer diaSemana,
            @NotBlank(message = "Elige la hora de inicio")
            @Pattern(regexp = HORA, message = "Hora de inicio no válida") String horaInicio,
            @NotBlank(message = "Elige la hora de fin")
            @Pattern(regexp = HORA, message = "Hora de fin no válida") String horaFin,
            @NotNull(message = "Elige la duración de cada cita")
            @Min(value = 15, message = "La duración mínima es 15 minutos")
            @Max(value = 180, message = "La duración máxima es 180 minutos") Integer duracionMinutos) {}

    public record TarifaRequest(
            @NotNull(message = "Escribe la tarifa")
            @DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa")
            @DecimalMax(value = "9999.99", message = "La tarifa máxima es 9999.99") BigDecimal tarifa) {}

    public record CitaRequest(
            @NotNull(message = "Elige al niño o niña") @Positive(message = "Elige al niño o niña") Integer pacienteId,
            @NotNull(message = "Elige la fecha") @FutureOrPresent(message = "La fecha no puede ser pasada") LocalDate fecha,
            @NotBlank(message = "Elige la hora")
            @Pattern(regexp = HORA, message = "Hora no válida") String horaInicio,
            @NotBlank(message = "Escribe el motivo de la cita")
            @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String motivo,
            boolean urgente) {}

    public record AccionRequest(
            @NotBlank(message = "Falta la acción")
            @Pattern(regexp = "^(CONFIRMAR|RECHAZAR|CANCELAR|ATENDIDA|NO_ASISTIO)$",
                    message = "Acción no válida") String accion,
            @Size(max = 300, message = "El comentario admite hasta 300 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String comentario,
            @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
            @DecimalMax(value = "9999.99", message = "El costo máximo es 9999.99") BigDecimal costo) {}

    private final GestionarCitasUseCase useCase;

    public CitaController(GestionarCitasUseCase useCase) {
        this.useCase = useCase;
    }

    // ----- Horarios (solo psicóloga) -----

    @GetMapping("/horarios")
    public MisHorariosVista misHorarios(Authentication auth) {
        return useCase.misHorarios(auth.getName());
    }

    @PostMapping("/horarios")
    @ResponseStatus(HttpStatus.CREATED)
    public MisHorariosVista agregarHorario(@Valid @RequestBody HorarioRequest r, Authentication auth) {
        return useCase.agregarHorario(auth.getName(), r.diaSemana(), r.horaInicio(), r.horaFin(), r.duracionMinutos());
    }

    @DeleteMapping("/horarios/{id}")
    public MisHorariosVista quitarHorario(@PathVariable Integer id, Authentication auth) {
        return useCase.quitarHorario(auth.getName(), id);
    }

    @PutMapping("/horarios/tarifa")
    public MisHorariosVista guardarTarifa(@Valid @RequestBody TarifaRequest r, Authentication auth) {
        return useCase.guardarTarifa(auth.getName(), r.tarifa());
    }

    // ----- Citas -----

    @GetMapping("/citas")
    public List<CitaVista> listar(Authentication auth) {
        return useCase.listar(auth.getName(), rol(auth));
    }

    @GetMapping("/citas/espacios-libres")
    public List<EspacioVista> espaciosLibres(@RequestParam Integer pacienteId, Authentication auth) {
        return useCase.espaciosLibres(pacienteId, auth.getName(), rol(auth));
    }

    @PostMapping("/citas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaVista crear(@Valid @RequestBody CitaRequest r, Authentication auth) {
        return useCase.crear(r.pacienteId(), r.fecha(), r.horaInicio(), r.motivo(), r.urgente(),
                auth.getName(), rol(auth));
    }

    @PostMapping("/citas/{id}/accion")
    public CitaVista accion(@PathVariable Integer id, @Valid @RequestBody AccionRequest r, Authentication auth) {
        return useCase.accion(id, r.accion(), r.comentario(), r.costo(), auth.getName(), rol(auth));
    }

    private String rol(Authentication auth) {
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}