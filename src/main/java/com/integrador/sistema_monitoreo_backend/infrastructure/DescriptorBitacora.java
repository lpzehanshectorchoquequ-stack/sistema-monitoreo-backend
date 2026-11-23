package com.integrador.sistema_monitoreo_backend.infrastructure;

import java.util.List;
import java.util.regex.Pattern;

public final class DescriptorBitacora {

    public record Descriptor(String modulo, String accion, String descripcion) {}

    private record Regla(String metodo, Pattern ruta, Descriptor descriptor) {}

    private static final List<Regla> REGLAS = List.of(
            regla("POST", "^/api/usuarios$", "Cuentas", "CREAR", "Creó una cuenta"),
            regla("POST", "^/api/usuarios/\\d+/restablecer-contrasena$", "Cuentas", "MODIFICAR", "Restableció una contraseña"),
            regla("PUT", "^/api/usuarios/\\d+/estado$", "Cuentas", "MODIFICAR", "Activó o desactivó una cuenta"),
            regla("POST", "^/api/auth/cambiar-contrasena$", "Sesión", "MODIFICAR", "Cambió su contraseña"),
            regla("POST", "^/api/pacientes$", "Pacientes", "CREAR", "Registró un paciente"),
            regla("POST", "^/api/pacientes/mis-hijos$", "Pacientes", "CREAR", "Registró a su hijo o hija"),
            regla("PUT", "^/api/pacientes/\\d+$", "Pacientes", "MODIFICAR", "Editó los datos de un paciente"),
            regla("DELETE", "^/api/pacientes/\\d+$", "Pacientes", "ELIMINAR", "Eliminó un paciente"),
            regla("POST", "^/api/pacientes/\\d+/tutores$", "Pacientes", "CREAR", "Vinculó un tutor a un paciente"),
            regla("DELETE", "^/api/pacientes/\\d+/tutores/\\d+$", "Pacientes", "ELIMINAR", "Quitó un tutor de un paciente"),
            regla("POST", "^/api/pacientes/\\d+/evaluaciones$", "Evaluaciones", "CREAR", "Aplicó una evaluación"),
            regla("POST", "^/api/pacientes/\\d+/evaluaciones/asignar$", "Evaluaciones", "CREAR", "Envió una evaluación al tutor"),
            regla("POST", "^/api/evaluaciones/\\d+/responder$", "Evaluaciones", "MODIFICAR", "Respondió una evaluación"),
            regla("POST", "^/api/evaluaciones/\\d+/aprobar$", "Evaluaciones", "MODIFICAR", "Aprobó el resultado de una evaluación"),
            regla("DELETE", "^/api/evaluaciones/\\d+$", "Evaluaciones", "ELIMINAR", "Canceló una evaluación enviada"),
            regla("POST", "^/api/tests$", "Tests", "CREAR", "Creó un test"),
            regla("PUT", "^/api/tests/\\d+$", "Tests", "MODIFICAR", "Editó un test"),
            regla("DELETE", "^/api/tests/\\d+$", "Tests", "ELIMINAR", "Eliminó un test"),
            regla("POST", "^/api/tests/\\d+/preguntas$", "Tests", "CREAR", "Agregó una pregunta a un test"),
            regla("PUT", "^/api/tests/\\d+/preguntas/\\d+$", "Tests", "MODIFICAR", "Editó una pregunta de un test"),
            regla("DELETE", "^/api/tests/\\d+/preguntas/\\d+$", "Tests", "ELIMINAR", "Quitó una pregunta de un test"),
            regla("PUT", "^/api/tests/\\d+/umbrales$", "Tests", "MODIFICAR", "Cambió los umbrales de un test"),
            regla("POST", "^/api/horarios$", "Citas", "CREAR", "Agregó un horario de atención"),
            regla("DELETE", "^/api/horarios/\\d+$", "Citas", "ELIMINAR", "Quitó un horario de atención"),
            regla("PUT", "^/api/horarios/tarifa$", "Citas", "MODIFICAR", "Cambió su tarifa de consulta"),
            regla("POST", "^/api/citas$", "Citas", "CREAR", "Pidió o propuso una cita"),
            regla("POST", "^/api/citas/\\d+/accion$", "Citas", "MODIFICAR", "Respondió o actualizó una cita"),
            regla("POST", "^/api/respaldos$", "Sistema", "CREAR", "Creó un respaldo de la base de datos"),
            regla("DELETE", "^/api/respaldos/.+$", "Sistema", "ELIMINAR", "Eliminó un respaldo")
    );

    private DescriptorBitacora() {}

    private static Regla regla(String metodo, String patron, String modulo, String accion, String descripcion) {
        return new Regla(metodo, Pattern.compile(patron), new Descriptor(modulo, accion, descripcion));
    }

    public static Descriptor describir(String metodo, String ruta) {
        for (Regla regla : REGLAS) {
            if (regla.metodo().equals(metodo) && regla.ruta().matcher(ruta).matches()) {
                return regla.descriptor();
            }
        }
        return new Descriptor(modulo(ruta), "OTRA", metodo + " " + ruta);
    }

    private static String modulo(String ruta) {
        String[] partes = ruta.split("/");
        if (partes.length > 2) {
            String segmento = partes[2];
            return segmento.substring(0, 1).toUpperCase() + segmento.substring(1);
        }
        return "Sistema";
    }
}