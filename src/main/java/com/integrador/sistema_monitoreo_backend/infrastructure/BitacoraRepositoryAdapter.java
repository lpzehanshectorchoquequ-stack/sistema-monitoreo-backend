package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Bitacora;
import com.integrador.sistema_monitoreo_backend.domain.BitacoraRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class BitacoraRepositoryAdapter implements BitacoraRepository {

    private final BitacoraRepositoryJPA jpa;
    private final EntityManager em;

    public BitacoraRepositoryAdapter(BitacoraRepositoryJPA jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public Bitacora guardar(Bitacora registro) {
        return jpa.save(registro);
    }

    @Override
    public Pagina buscar(Filtro filtro, int pagina, int tamano) {
        StringBuilder donde = new StringBuilder(" where 1 = 1");
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.texto() != null && !filtro.texto().isBlank()) {
            donde.append(" and (lower(b.usuario) like :texto or lower(b.descripcion) like :texto"
                    + " or lower(b.modulo) like :texto)");
            parametros.put("texto", "%" + filtro.texto().trim().toLowerCase() + "%");
        }
        if (filtro.modulo() != null && !filtro.modulo().isBlank()) {
            donde.append(" and b.modulo = :modulo");
            parametros.put("modulo", filtro.modulo());
        }
        if (filtro.resultado() != null && !filtro.resultado().isBlank()) {
            donde.append(" and b.resultado = :resultado");
            parametros.put("resultado", filtro.resultado());
        }
        if (filtro.desde() != null) {
            donde.append(" and b.fechaHora >= :desde");
            parametros.put("desde", filtro.desde());
        }
        if (filtro.hasta() != null) {
            donde.append(" and b.fechaHora < :hasta");
            parametros.put("hasta", filtro.hasta());
        }

        TypedQuery<Long> conteo = em.createQuery("select count(b) from Bitacora b" + donde, Long.class);
        parametros.forEach(conteo::setParameter);

        TypedQuery<Bitacora> consulta = em.createQuery(
                "select b from Bitacora b" + donde + " order by b.fechaHora desc, b.id desc", Bitacora.class);
        parametros.forEach(consulta::setParameter);
        consulta.setFirstResult(pagina * tamano);
        consulta.setMaxResults(tamano);

        return new Pagina(consulta.getResultList(), conteo.getSingleResult());
    }

    @Override
    public long contarDesde(LocalDateTime desde) {
        return em.createQuery("select count(b) from Bitacora b where b.fechaHora >= :desde", Long.class)
                .setParameter("desde", desde)
                .getSingleResult();
    }

    @Override
    public long contarPorResultadoDesde(String resultado, LocalDateTime desde) {
        return em.createQuery(
                        "select count(b) from Bitacora b where b.resultado = :resultado and b.fechaHora >= :desde",
                        Long.class)
                .setParameter("resultado", resultado)
                .setParameter("desde", desde)
                .getSingleResult();
    }

    @Override
    public long contarPorAccionDesde(String accion, LocalDateTime desde) {
        return em.createQuery(
                        "select count(b) from Bitacora b where b.accion = :accion and b.fechaHora >= :desde",
                        Long.class)
                .setParameter("accion", accion)
                .setParameter("desde", desde)
                .getSingleResult();
    }

    @Override
    public long contarUsuariosActivosDesde(LocalDateTime desde) {
        return em.createQuery(
                        "select count(distinct b.usuario) from Bitacora b where b.fechaHora >= :desde"
                                + " and b.usuario not in ('anonimo', 'sistema')",
                        Long.class)
                .setParameter("desde", desde)
                .getSingleResult();
    }
}