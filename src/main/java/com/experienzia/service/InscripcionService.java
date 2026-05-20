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

/**
 * Contrato para inscripciones, check-in/out y gestion de staff en eventos.
 */
public interface InscripcionService {

    /** Inscribe a un usuario en un evento si hay cupo. */
    InscripcionDTO inscribir(Long usuarioId, Long eventoId);

    /**
     * Inscribe automáticamente al organizador como asistente de su propio evento.
     * Se invoca cuando el evento pasa a ACTIVO (tras aprobarse el pago).
     * Es idempotente: si ya está inscrito, no hace nada y devuelve null.
     */
    InscripcionDTO inscribirOrganizadorEnSuEvento(Long eventoId);

    /** Cancela la inscripcion de un asistente. */
    InscripcionDTO cancelar(Long inscripcionId);

    /** Registra la entrada manual (staff elige de la lista). */
    InscripcionDTO checkIn(Long inscripcionId, Long staffUsuarioId);

    /** Registra la entrada escaneando el codigo QR. */
    InscripcionDTO checkInPorQR(String codigoQR, Long staffUsuarioId, Long eventoId);

    /** Registra la salida manual. */
    InscripcionDTO checkOut(Long inscripcionId, Long staffUsuarioId);

    /** Registra la salida escaneando QR. */
    InscripcionDTO checkOutPorQR(String codigoQR, Long staffUsuarioId, Long eventoId);

    /** Todas las inscripciones de un evento. */
    List<InscripcionDTO> listarPorEvento(Long eventoId);

    /** Inscripciones de un usuario (mis eventos). */
    List<InscripcionDTO> listarPorUsuario(Long usuarioId);

    /** Lista de asistentes que ve el staff en su panel. */
    List<AsistenteEventoDTO> listarAsistentesParaStaff(Long eventoId, Long staffUsuarioId, String busqueda);

    /** Igual que listarAsistentesParaStaff pero validando que quien consulta es el organizador del evento. */
    List<AsistenteEventoDTO> listarAsistentesParaOrganizador(Long eventoId, Long organizadorId, String busqueda);
    /** Cuenta cuantas personas hay dentro del evento en este momento. */
    AforoEnVivoDTO consultarAforoEnVivo(Long eventoId);

    /** Carga asistentes uno por uno desde una lista manual. */
    ResultadoCargaAsistentesDTO cargarAsistentesManual(Long eventoId, Long organizadorId, List<FilaAsistenteCargaDTO> filas);

    /** Carga asistentes desde un archivo CSV en texto. */
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
