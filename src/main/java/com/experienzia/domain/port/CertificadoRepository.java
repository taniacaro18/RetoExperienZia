package com.experienzia.domain.port;

import com.experienzia.domain.model.Certificado;
import java.util.List;
import java.util.Optional;

public interface CertificadoRepository {
    Certificado guardar(Certificado certificado);
    List<Certificado> listarPorUsuario(Long usuarioId);
    Optional<Certificado> buscarPorCodigo(String codigo);
    Optional<Certificado> buscarPorUsuarioYEvento(Long usuarioId, Long eventoId);
}
