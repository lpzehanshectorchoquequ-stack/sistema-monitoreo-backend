package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.GestionarPacientesUseCase;
import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    public record PacienteRequest(
            @NotBlank String nombre,
            @NotNull @PastOrPresent LocalDate fechaNacimiento,
            String motivoConsulta,
            @NotNull Integer tutorId) {}

    public record PacienteResponse(Integer id, String nombre, LocalDate fechaNacimiento,
                                   String motivoConsulta, Integer tutorId, String tutorNombre) {}

    public record TutorResponse(Integer id, String nombre, String correo) {}

    private final GestionarPacientesUseCase useCase;

    public PacienteController(GestionarPacientesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<PacienteResponse> listar(Authentication auth) {
        return useCase.listar(auth.getName(), rol(auth)).stream().map(this::aRespuesta).toList();
    }

    @GetMapping("/mis-hijos")
    public List<PacienteResponse> misHijos(Authentication auth) {
        return useCase.misHijos(auth.getName()).stream().map(this::aRespuesta).toList();
    }

    @GetMapping("/tutores")
    public List<TutorResponse> tutores() {
        return useCase.listarTutores().stream()
                .map(u -> new TutorResponse(u.getId(), u.getNombre(), u.getCorreo()))
                .toList();
    }

    @GetMapping("/{id}")
    public PacienteResponse obtener(@PathVariable Integer id, Authentication auth) {
        return aRespuesta(useCase.obtener(id, auth.getName(), rol(auth)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse crear(@Valid @RequestBody PacienteRequest r, Authentication auth) {
        var datos = new GestionarPacientesUseCase.Datos(
                r.nombre(), r.fechaNacimiento(), r.motivoConsulta(), r.tutorId());
        return aRespuesta(useCase.crear(datos, auth.getName()));
    }

    @PutMapping("/{id}")
    public PacienteResponse actualizar(@PathVariable Integer id,
                                       @Valid @RequestBody PacienteRequest r,
                                       Authentication auth) {
        var datos = new GestionarPacientesUseCase.Datos(
                r.nombre(), r.fechaNacimiento(), r.motivoConsulta(), r.tutorId());
        return aRespuesta(useCase.actualizar(id, datos, auth.getName(), rol(auth)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id, Authentication auth) {
        useCase.eliminar(id, auth.getName(), rol(auth));
    }

    private String rol(Authentication auth) {
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    private PacienteResponse aRespuesta(Paciente p) {
        Usuario tutor = p.getTutor();
        return new PacienteResponse(
                p.getId(), p.getNombre(), p.getFechaNacimiento(), p.getMotivoConsulta(),
                tutor != null ? tutor.getId() : null,
                tutor != null ? tutor.getNombre() : null);
    }
}