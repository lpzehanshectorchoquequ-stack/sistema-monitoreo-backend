package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.GestionarPacientesUseCase;
import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    public record PacienteRequest(
            @NotBlank(message = "Ingresa el nombre del paciente")
            @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
            @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE) String nombre,

            @NotNull(message = "Ingresa la fecha de nacimiento")
            @PastOrPresent(message = "La fecha de nacimiento no puede ser futura") LocalDate fechaNacimiento,

            @Size(max = 500, message = "El motivo de consulta admite hasta 500 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String motivoConsulta,

            @NotNull(message = "Selecciona un tutor")
            @Positive(message = "Selecciona un tutor") Integer tutorId,

            @Pattern(regexp = "^(NINO|NINA)?$", message = "Género no válido") String genero,

            @Size(max = 500, message = "Las fortalezas admiten hasta 500 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String fortalezas,

            @Size(max = 500, message = "Las áreas de apoyo admiten hasta 500 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String dificultades
    ) {}

    public record HijoRequest(
            @NotBlank(message = "Ingresa el nombre")
            @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
            @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE) String nombre,

            @NotNull(message = "Ingresa la fecha de nacimiento")
            @PastOrPresent(message = "La fecha de nacimiento no puede ser futura") LocalDate fechaNacimiento,

            @NotBlank(message = "Elige si es niño o niña")
            @Pattern(regexp = "^(NINO|NINA)$", message = "Género no válido") String genero,

            @Size(max = 500, message = "El motivo admite hasta 500 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String motivoConsulta
    ) {}

    public record PacienteResponse(Integer id, String nombre, LocalDate fechaNacimiento, String genero,
                                   String motivoConsulta, String fortalezas, String dificultades,
                                   Integer tutorId, String tutorNombre, String psicologoNombre) {}

    public record PersonaResponse(Integer id, String nombre, String correo) {}

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

    @PostMapping("/mis-hijos")
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse registrarHijo(@Valid @RequestBody HijoRequest r, Authentication auth) {
        var datos = new GestionarPacientesUseCase.DatosHijo(
                r.nombre(), r.fechaNacimiento(), r.genero(), r.motivoConsulta());
        return aRespuesta(useCase.registrarHijo(datos, auth.getName()));
    }

    @GetMapping("/tutores")
    public List<PersonaResponse> tutores() {
        return useCase.listarTutores().stream().map(this::aPersona).toList();
    }

    @GetMapping("/psicologos")
    public List<PersonaResponse> psicologos() {
        return useCase.listarPsicologos().stream().map(this::aPersona).toList();
    }

    @GetMapping("/{id}")
    public PacienteResponse obtener(@PathVariable Integer id, Authentication auth) {
        return aRespuesta(useCase.obtener(id, auth.getName(), rol(auth)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse crear(@Valid @RequestBody PacienteRequest r, Authentication auth) {
        return aRespuesta(useCase.crear(aDatos(r), auth.getName()));
    }

    @PutMapping("/{id}")
    public PacienteResponse actualizar(@PathVariable Integer id,
                                       @Valid @RequestBody PacienteRequest r,
                                       Authentication auth) {
        return aRespuesta(useCase.actualizar(id, aDatos(r), auth.getName(), rol(auth)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id, Authentication auth) {
        useCase.eliminar(id, auth.getName(), rol(auth));
    }

    private GestionarPacientesUseCase.Datos aDatos(PacienteRequest r) {
        return new GestionarPacientesUseCase.Datos(r.nombre(), r.fechaNacimiento(), r.motivoConsulta(),
                r.tutorId(), r.genero(), r.fortalezas(), r.dificultades());
    }

    private String rol(Authentication auth) {
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    private PersonaResponse aPersona(Usuario u) {
        return new PersonaResponse(u.getId(), u.getNombre(), u.getCorreo());
    }

    private PacienteResponse aRespuesta(Paciente p) {
        Usuario tutor = p.getTutor();
        Usuario psicologo = p.getPsicologo();
        return new PacienteResponse(
                p.getId(), p.getNombre(), p.getFechaNacimiento(), p.getGenero(),
                p.getMotivoConsulta(), p.getFortalezas(), p.getDificultades(),
                tutor != null ? tutor.getId() : null,
                tutor != null ? tutor.getNombre() : null,
                psicologo != null ? psicologo.getNombre() : null);
    }
}