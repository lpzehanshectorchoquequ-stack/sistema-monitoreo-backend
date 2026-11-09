package com.integrador.sistema_monitoreo_backend.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String RUTA_CAMBIO = "/api/auth/cambiar-contrasena";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String encabezado = request.getHeader("Authorization");

        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            try {
                Claims claims = jwtTokenProvider.leerClaims(encabezado.substring(7));

                Boolean cambio = claims.get("cambio", Boolean.class);
                if (Boolean.TRUE.equals(cambio) && !RUTA_CAMBIO.equals(request.getRequestURI())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"mensaje\":\"Debes cambiar tu contraseña temporal antes de continuar\"}");
                    return;
                }

                String correo = claims.getSubject();
                String rol = claims.get("rol", String.class);
                var autoridad = new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase());
                var autenticacion = new UsernamePasswordAuthenticationToken(correo, null, List.of(autoridad));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (JwtException | IllegalArgumentException e) {
                // Token inválido o vencido: la petición sigue sin autenticar
            }
        }
        filterChain.doFilter(request, response);
    }
}