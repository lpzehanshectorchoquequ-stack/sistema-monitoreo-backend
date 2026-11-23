package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.application.ConsultarEstadisticasUseCase.Conteo;
import com.integrador.sistema_monitoreo_backend.domain.Cita;
import com.integrador.sistema_monitoreo_backend.domain.CitaRepository;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ConsultarEstadisticasCitasUseCase {

    public record SemanaVista(String desde, long atendidas, long canceladas, long noAsistio,
                              long confirmadas, BigDecimal ingresos) {}

    public record PsicologaVista(String nombre, long totalCitas, long atendidas, BigDecimal ingresos,
                                 Integer asistencia) {}

    public record EstadisticasCitasVista(String periodo, String desde, String hasta, long totalCitas,
                                         long atendidas, long canceladas, long noAsistio, long confirmadas,
                                         long pendientes, long pendientesRespuesta, long urgentesPendientes,
                                         BigDecimal ingresosPeriodo, BigDecimal ingresosSemana,
                                         Integer asistencia, List<SemanaVista> semanas,
                                         List<Conteo> porEstado, List<Conteo> porDia,
                                         List<PsicologaVista> porPsicologa) {}

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final Set<String> NO_CUENTAN = Set.of("CANCELADA", "RECHAZADA");

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;

    public ConsultarEstadisticasCitasUseCase(CitaRepository citaRepository, UsuarioRepository usuarioRepository) {
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public EstadisticasCitasVista consultar(String periodoPedido, String correo, String rol) {
        boolean esAdmin = "ADMIN".equals(rol);
        String periodo = List.of("SEMANA", "MES", "TRIMESTRE").contains(periodoPedido) ? periodoPedido : "MES";

        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        LocalDate[] rango = rango(periodo, hoy);

        List<Cita> todas = esAdmin ? citaRepository.listarTodas() : citaRepository.listarPorPsicologo(correo);
        List<Cita> enPeriodo = entre(todas, rango[0], rango[1]);

        long atendidas = contar(enPeriodo, "ATENDIDA");
        long noAsistio = contar(enPeriodo, "NO_ASISTIO");
        long canceladas = contar(enPeriodo, "CANCELADA", "RECHAZADA");
        long confirmadas = contar(enPeriodo, "CONFIRMADA");
        long pendientes = contar(enPeriodo, "SOLICITADA", "PROPUESTA");

        List<Conteo> porEstado = List.of(
                new Conteo("ATENDIDA", atendidas),
                new Conteo("CONFIRMADA", confirmadas),
                new Conteo("PENDIENTE", pendientes),
                new Conteo("CANCELADA", canceladas),
                new Conteo("NO_ASISTIO", noAsistio));

        long[] porDiaSemana = new long[7];
        for (Cita c : enPeriodo) {
            if (!NO_CUENTAN.contains(c.getEstado())) {
                porDiaSemana[c.getFecha().getDayOfWeek().getValue() - 1]++;
            }
        }
        List<Conteo> porDia = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            porDia.add(new Conteo(DIAS[i], porDiaSemana[i]));
        }

        // Últimas 8 semanas, de la más antigua a la actual
        List<SemanaVista> semanas = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            LocalDate inicio = lunes.minusWeeks(i);
            List<Cita> deLaSemana = entre(todas, inicio, inicio.plusDays(6));
            semanas.add(new SemanaVista(inicio.toString(), contar(deLaSemana, "ATENDIDA"),
                    contar(deLaSemana, "CANCELADA", "RECHAZADA"), contar(deLaSemana, "NO_ASISTIO"),
                    contar(deLaSemana, "CONFIRMADA"), ingresos(deLaSemana)));
        }

        List<PsicologaVista> porPsicologa = new ArrayList<>();
        if (esAdmin) {
            for (Usuario p : usuarioRepository.listarPorRol("Psicologo")) {
                List<Cita> suyas = enPeriodo.stream()
                        .filter(c -> c.getPsicologo().getId().equals(p.getId()))
                        .toList();
                long atendidasSuyas = contar(suyas, "ATENDIDA");
                long noAsistioSuyas = contar(suyas, "NO_ASISTIO");
                porPsicologa.add(new PsicologaVista(p.getNombreCompleto(), suyas.size(), atendidasSuyas,
                        ingresos(suyas), asistencia(atendidasSuyas, noAsistioSuyas)));
            }
            porPsicologa.sort(Comparator.comparing(PsicologaVista::ingresos).reversed());
        }

        long urgentes = todas.stream()
                .filter(c -> "SOLICITADA".equals(c.getEstado()) && Boolean.TRUE.equals(c.getUrgente()))
                .count();

        return new EstadisticasCitasVista(periodo, rango[0].toString(), rango[1].toString(), enPeriodo.size(),
                atendidas, canceladas, noAsistio, confirmadas, pendientes, contar(todas, "SOLICITADA"), urgentes,
                ingresos(enPeriodo), ingresos(entre(todas, lunes, lunes.plusDays(6))),
                asistencia(atendidas, noAsistio), semanas, porEstado, porDia, porPsicologa);
    }

    private LocalDate[] rango(String periodo, LocalDate hoy) {
        return switch (periodo) {
            case "SEMANA" -> new LocalDate[]{hoy.with(DayOfWeek.MONDAY), hoy.with(DayOfWeek.MONDAY).plusDays(6)};
            case "TRIMESTRE" -> new LocalDate[]{hoy.withDayOfMonth(1).minusMonths(2),
                    hoy.withDayOfMonth(hoy.lengthOfMonth())};
            default -> new LocalDate[]{hoy.withDayOfMonth(1), hoy.withDayOfMonth(hoy.lengthOfMonth())};
        };
    }

    private List<Cita> entre(List<Cita> citas, LocalDate desde, LocalDate hasta) {
        return citas.stream()
                .filter(c -> !c.getFecha().isBefore(desde) && !c.getFecha().isAfter(hasta))
                .toList();
    }

    private long contar(List<Cita> citas, String... estados) {
        Set<String> buscados = Set.of(estados);
        return citas.stream().filter(c -> buscados.contains(c.getEstado())).count();
    }

    private BigDecimal ingresos(List<Cita> citas) {
        return citas.stream()
                .filter(c -> "ATENDIDA".equals(c.getEstado()))
                .map(Cita::getCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer asistencia(long atendidas, long noAsistio) {
        long total = atendidas + noAsistio;
        return total == 0 ? null : Math.round(atendidas * 100f / total);
    }
}