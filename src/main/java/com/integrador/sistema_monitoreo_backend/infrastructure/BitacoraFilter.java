package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.application.RegistrarBitacoraUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class BitacoraFilter extends OncePerRequestFilter {

    private final RegistrarBitacoraUseCase bitacora;

    public BitacoraFilter(RegistrarBitacoraUseCase bitacora) {
        this.bitacora = bitacora;
    }

    // Solo se registran las acciones que cambian datos; el login se registra aparte, con el correo probado
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String metodo = request.getMethod();
        String ruta = request.getRequestURI();
        boolean cambiaDatos = "POST".equals(metodo) || "PUT".equals(metodo) || "DELETE".equals(metodo);
        return !cambiaDatos || !ruta.startsWith("/api/") || ruta.equals("/api/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            registrar(request, response);
        }
    }

    private void registrar(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean identificado = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        String usuario = identificado ? auth.getName() : "anonimo";
        String rol = identificado && !auth.getAuthorities().isEmpty()
                ? nombreRol(auth.getAuthorities().iterator().next().getAuthority())
                : null;

        var descriptor = DescriptorBitacora.describir(request.getMethod(), request.getRequestURI());
        bitacora.registrar(usuario, rol, descriptor.modulo(), descriptor.accion(), descriptor.descripcion(),
                request.getMethod(), request.getRequestURI(), response.getStatus(), request.getRemoteAddr());
    }

    private String nombreRol(String autoridad) {
        return switch (autoridad.replace("ROLE_", "")) {
            case "ADMIN" -> "Admin";
            case "PSICOLOGO" -> "Psicologo";
            case "TUTOR" -> "Tutor";
            default -> null;
        };
    }
}