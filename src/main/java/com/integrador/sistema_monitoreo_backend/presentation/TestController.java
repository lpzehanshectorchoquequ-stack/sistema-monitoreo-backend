package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.GestionarTestsUseCase;
import com.integrador.sistema_monitoreo_backend.application.GestionarTestsUseCase.DatosPregunta;
import com.integrador.sistema_monitoreo_backend.application.GestionarTestsUseCase.DatosTest;
import com.integrador.sistema_monitoreo_backend.application.GestionarTestsUseCase.TestDetalle;
import com.integrador.sistema_monitoreo_backend.application.GestionarTestsUseCase.TestResumen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    public record TestRequest(
            @NotBlank(message = "Escribe el nombre del test")
            @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String nombre,
            @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String descripcion,
            @NotBlank(message = "Elige el área del test")
            @Pattern(regexp = "^(TDAH|TEA|APRENDIZAJE|OTRA)$", message = "Área no válida") String trastorno,
            @NotBlank(message = "Elige quién responde el test")
            @Pattern(regexp = "^(Tutor|Psicologo|Docente)$", message = "Quién responde no es válido") String quienResponde,
            @Pattern(regexp = "^(BORRADOR|VALIDADO)?$", message = "Estado no válido") String estadoValidacion) {}

    public record PreguntaRequest(
            @NotBlank(message = "Escribe la pregunta")
            @Size(min = 5, max = 300, message = "La pregunta debe tener entre 5 y 300 caracteres")
            @Pattern(regexp = Validaciones.TEXTO, message = Validaciones.MSG_TEXTO) String texto,
            @Size(max = 30, message = "El grupo admite hasta 30 caracteres")
            @Pattern(regexp = "^[\\p{L}\\p{N} ]*$",
                    message = "El grupo solo puede tener letras, números y espacios") String subescala) {}

    public record UmbralesRequest(
            @NotNull(message = "Indica el umbral moderado")
            @Min(value = 1, message = "El umbral moderado debe ser al menos 1")
            @Max(value = 100, message = "Valor demasiado alto") Integer moderado,
            @NotNull(message = "Indica el umbral alto")
            @Min(value = 2, message = "El umbral alto debe ser al menos 2")
            @Max(value = 100, message = "Valor demasiado alto") Integer alto) {}

    private final GestionarTestsUseCase useCase;

    public TestController(GestionarTestsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<TestResumen> listar() {
        return useCase.listar();
    }

    @GetMapping("/{id}")
    public TestDetalle obtener(@PathVariable Integer id) {
        return useCase.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestDetalle crear(@Valid @RequestBody TestRequest r) {
        return useCase.crear(aDatos(r));
    }

    @PutMapping("/{id}")
    public TestDetalle actualizar(@PathVariable Integer id, @Valid @RequestBody TestRequest r) {
        return useCase.actualizar(id, aDatos(r));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        useCase.eliminar(id);
    }

    @PostMapping("/{id}/preguntas")
    @ResponseStatus(HttpStatus.CREATED)
    public TestDetalle agregarPregunta(@PathVariable Integer id, @Valid @RequestBody PreguntaRequest r) {
        return useCase.agregarPregunta(id, new DatosPregunta(r.texto(), r.subescala()));
    }

    @PutMapping("/{id}/preguntas/{preguntaId}")
    public TestDetalle editarPregunta(@PathVariable Integer id, @PathVariable Integer preguntaId,
                                      @Valid @RequestBody PreguntaRequest r) {
        return useCase.editarPregunta(id, preguntaId, new DatosPregunta(r.texto(), r.subescala()));
    }

    @DeleteMapping("/{id}/preguntas/{preguntaId}")
    public TestDetalle quitarPregunta(@PathVariable Integer id, @PathVariable Integer preguntaId) {
        return useCase.quitarPregunta(id, preguntaId);
    }

    @PutMapping("/{id}/umbrales")
    public TestDetalle guardarUmbrales(@PathVariable Integer id, @Valid @RequestBody UmbralesRequest r) {
        return useCase.guardarUmbrales(id, r.moderado(), r.alto());
    }

    private DatosTest aDatos(TestRequest r) {
        return new DatosTest(r.nombre(), r.descripcion(), r.trastorno(), r.quienResponde(), r.estadoValidacion());
    }
}