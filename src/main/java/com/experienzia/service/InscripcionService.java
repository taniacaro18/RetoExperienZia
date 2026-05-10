package com.experienzia.service;

import com.experienzia.dto.AforoEnVivoDTO;
import com.experienzia.dto.AsistenteEventoDTO;
import com.experienzia.dto.EventoStaffDTO;
import com.experienzia.dto.FilaAsistenteCargaDTO;
import com.experienzia.dto.InscripcionDTO;
import com.experienzia.dto.ResultadoCargaAsistentesDTO;
import com.experienzia.dto.StaffAsignadoDTO;
import com.experienzia.entity.FuncionStaff;

import java.util.List;

public interface InscripcionService {
    InscripcionDTO inscribir(Long usuarioId, Long eventoId);

    /**
     * Inscribe automáticamente al organizador como asistente de su propio evento.
     * Se invoca cuando el evento pasa a ACTIVO (tras aprobarse el pago).
     * Es idempotente: si ya está inscrito, no hace nada y devuelve null.
     */
    InscripcionDTO inscribirOrganizadorEnSuEvento(Long eventoId);

    InscripcionDTO cancelar(Long inscripcionId);
    InscripcionDTO checkIn(Long inscripcionId, Long staffUsuarioId);
    InscripcionDTO checkInPorQR(String codigoQR, Long staffUsuarioId, Long eventoId);
    InscripcionDTO checkOut(Long inscripcionId, Long staffUsuarioId);
    InscripcionDTO checkOutPorQR(String codigoQR, Long staffUsuarioId, Long eventoId);
    List<InscripcionDTO> listarPorEvento(Long eventoId);
    List<InscripcionDTO> listarPorUsuario(Long usuarioId);
    List<AsistenteEventoDTO> listarAsistentesParaStaff(Long eventoId, Long staffUsuarioId, String busqueda);

    /** Igual que listarAsistentesParaStaff pero validando que quien consulta es el organizador del evento. */
    List<AsistenteEventoDTO> listarAsistentesParaOrganizador(Long eventoId, Long organizadorId, String busqueda);
    AforoEnVivoDTO consultarAforoEnVivo(Long eventoId);
    ResultadoCargaAsistentesDTO cargarAsistentesManual(Long eventoId, Long organizadorId, List<FilaAsistenteCargaDTO> filas);
    ResultadoCargaAsistentesDTO cargarAsistentesCsv(Long eventoId, Long organizadorId, String contenidoCsv);

    /** Asigna un staff a un evento con una función específica (CHECK_IN_QR, CHECK_IN_MANUAL, REGISTRO_SALIDA, GENERAL). */
    void asignarStaff(Long eventoId, Long organizadorId, Long staffUsuarioId, FuncionStaff funcion);

    /** Cambia la función asignada a un staff dentro de un evento. */
    StaffAsignadoDTO cambiarFuncionStaff(Long eventoId, Long organizadorId, Long staffUsuarioId, FuncionStaff funcion);

    /** Quita un staff del evento. */
    void desasignarStaff(Long eventoId, Long organizadorId, Long staffUsuarioId);

    /** Listado plano de IDs (compatibilidad con clientes anteriores). */
    List<Long> listarStaffIdsPorEvento(Long eventoId);

    /** Listado enriquecido del staff asignado al evento con su función. */
    List<StaffAsignadoDTO> listarStaffPorEvento(Long eventoId);

    /** Eventos asignados al usuario STAFF (para su panel) con info completa del evento. */
    List<EventoStaffDTO> listarEventosDelStaff(Long staffUsuarioId);
}
