package com.integrador.sistema_monitoreo_backend.presentation;

public final class Validaciones {

    private Validaciones() {}

    // Solo letras (con tildes), espacios, apóstrofes y guiones
    public static final String NOMBRE = "^\\p{L}+(?:[ '\u2019-]\\p{L}+)*$";
    public static final String MSG_NOMBRE =
            "El nombre solo puede contener letras, espacios, apóstrofes y guiones";

    // Letras, números y signos básicos; sin < > { } [ ] \ | ^ ~ $ # & = @
    public static final String TEXTO = "^[\\p{L}\\p{N} .,;:\u00A1!\u00BF?()\\-_/'\"\u2019%+\\r\\n]*$";
    public static final String MSG_TEXTO =
            "El texto contiene caracteres no permitidos (usa letras, números y signos básicos)";

    public static final String CORREO = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    public static final String MSG_CORREO = "Ingresa un correo válido, por ejemplo nombre@ejemplo.com";

    public static final String CONTRASENA = "^(?=.*\\p{L})(?=.*\\d)\\S{8,64}$";
    public static final String MSG_CONTRASENA =
            "La contraseña debe tener entre 8 y 64 caracteres, con al menos una letra y un número, sin espacios";
}