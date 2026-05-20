/**
 * Tipos e interfaces del dominio ExperienZia (lo que devuelve el API).
 */

// Rol del usuario en la app.
export type Rol = 'ADMIN' | 'ORGANIZADOR' | 'ASISTENTE' | 'STAFF';
// Si la cuenta está activa, pendiente de aprobación, etc.
export type EstadoUsuario = 'ACTIVO' | 'PENDIENTE' | 'RECHAZADO' | 'INACTIVO';
// Evento abierto a todos o solo por invitación/carga.
export type TipoEvento = 'PUBLICO' | 'PRIVADO';
// Ciclo de vida del evento (pendiente, activo, cancelado…).
export type EstadoEvento =
  | 'PENDIENTE'
  | 'APROBADO'
  | 'ACTIVO'
  | 'RECHAZADO'
  | 'CANCELADO'
  | 'FINALIZADO'
  | 'PENDIENTE_REVISION'
  | 'PENDIENTE_SUPLEMENTO'
  | 'PENDIENTE_CANCELACION';
// Estado de la inscripción del asistente al evento.
export type EstadoInscripcion = 'INSCRITO' | 'ASISTIO' | 'CANCELADO';
// Estado del pago del organizador por el evento.
export type EstadoPago = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
// Tipo de cambio que pidió el organizador (horas, cancelación…).
export type TipoNovedadEvento =
  | 'EDICION_METADATOS'
  | 'EDICION_TIPO_CATEGORIA'
  | 'AUMENTO_HORAS'
  | 'DISMINUCION_HORAS'
  | 'CANCELACION_SOLICITUD';
// Si el admin ya resolvió esa novedad.
export type EstadoNovedadEvento = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
// Qué puede hacer el staff en puerta (QR, manual, salida…).
export type FuncionStaff = 'CHECK_IN_QR' | 'CHECK_IN_MANUAL' | 'REGISTRO_SALIDA' | 'GENERAL';
// Tipo visual de la notificación in-app.
export type TipoNotificacion = 'INFO' | 'ALERTA' | 'ERROR';

export interface Usuario {
  id: number; // id en base de datos
  nombre: string;
  email: string;
  password?: string | null; // solo en formularios, no suele venir del API
  telefono?: string | null;
  tipoDocumento?: string | null; // CC, CE, etc.
  numeroDocumento?: string | null;
  rol: Rol;
  estado: EstadoUsuario;
  organizadorId?: number | null; // si es STAFF, a quién pertenece
  tipo?: 'ASISTENTE' | 'ORGANIZADOR'; // en registro público
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Respuesta del login con JWT y datos del usuario. */
export interface LoginResponse {
  accessToken: string; // token para el interceptor
  usuario: Usuario;
}

export interface ActualizarPerfil {
  telefono?: string;
  nuevaPassword?: string;
}

export interface Evento {
  id: number;
  nombre: string;
  descripcion?: string;
  fecha: string; // inicio ISO
  fechaFin?: string;
  ubicacion?: string; // salón o sede
  tipoEvento: TipoEvento;
  estado: EstadoEvento;
  aforoMaximo: number;
  aforoActual: number; // inscritos o presentes según contexto
  costo: number; // lo que paga el organizador
  organizadorId: number;
  organizadorNombre?: string | null;
  organizadorEmail?: string | null;
  imagen?: string | null;
  categoria?: string | null;
  duracionHoras?: number | null;
  motivoRechazo?: string | null;
  motivoCancelacion?: string | null;
  resumenSolicitudEdicion?: string | null; // texto para el admin
  estadoPrevioRevision?: EstadoEvento | null;
  alertaNegocio?: string | null; // aviso temporal al guardar
}

/** Historial de cambios pedidos sobre un evento. */
export interface EventoNovedad {
  id: number;
  eventoId: number;
  usuarioSolicitanteId?: number | null;
  tipo: TipoNovedadEvento;
  estado: EstadoNovedadEvento;
  fechaSolicitud: string;
  fechaResolucion?: string | null;
  motivoResolucion?: string | null;
  detalleJson?: string | null; // cambios en JSON
}

export interface Inscripcion {
  id: number;
  usuarioId: number;
  eventoId: number;
  fechaInscripcion: string;
  estado: EstadoInscripcion;
  fechaCheckIn?: string | null;
  fechaCheckOut?: string | null;
  codigoQR?: string | null; // para entrada con lector
  nombreAsistente?: string | null; // en respuestas de check-in
  emailAsistente?: string | null;
  tipoDocumento?: string | null;
  numeroDocumento?: string | null;
  nombreEvento?: string | null;
  fechaEvento?: string | null;
  fechaFinEvento?: string | null;
  ubicacionEvento?: string | null;
}

export interface AsistenteEvento {
  inscripcionId: number;
  usuarioId: number;
  nombre: string;
  email: string;
  telefono?: string;
  tipoDocumento?: string;
  numeroDocumento?: string;
  codigoQR?: string | null;
  estadoInscripcion: EstadoInscripcion;
  fechaInscripcion?: string;
  fechaCheckIn?: string | null;
  fechaCheckOut?: string | null;
}

export interface AforoEnVivo {
  eventoId: number;
  nombreEvento: string;
  aforoMaximo: number;
  inscritos: number;
  asistencias: number; // personas que ya hicieron check-in alguna vez
  presentes: number; // dentro ahora (check-in sin check-out)
  cuposDisponibles: number;
  porcentajeOcupacion: number;
}

export interface StaffAsignado {
  asignacionId: number;
  staffUsuarioId: number;
  nombre?: string;
  email?: string;
  telefono?: string;
  estadoUsuario?: string;
  funcion: FuncionStaff;
}

export interface EventoStaff {
  asignacionId: number;
  eventoId: number;
  nombreEvento: string;
  descripcion?: string;
  fechaEvento: string;
  ubicacion?: string;
  tipoEvento: TipoEvento;
  estadoEvento: EstadoEvento;
  categoria?: string;
  aforoMaximo: number;
  aforoActual: number;
  organizadorId: number;
  funcion: FuncionStaff; // qué hace este staff en el evento
}

export interface Pago {
  id: number;
  eventoId: number;
  organizadorId: number;
  comprobanteUrl?: string; // ruta del archivo subido
  monto?: number;
  saldoAprobadoPrevio?: number | null; // pago complementario
  estado: EstadoPago;
  fecha: string;
  motivoRechazo?: string;
  aprobadorId?: number | null;
  fechaResolucion?: string | null;
  nombreEvento?: string;
  fechaEvento?: string;
  nombreOrganizador?: string;
  emailOrganizador?: string;
}

export interface Auditoria {
  id: number;
  usuarioId?: number | null; // quién hizo la acción
  accion: string; // CREATE, UPDATE, LOGIN…
  entidad: string; // tabla o tipo (EVENTO, USUARIO…)
  entidadId?: number | null;
  fecha: string;
  direccionIp?: string | null;
}

export interface Notificacion {
  id: number;
  usuarioId: number;
  mensaje: string;
  tipo: TipoNotificacion;
  leida: boolean;
  fecha: string;
}

export interface Certificado {
  id: number;
  inscripcionId: number;
  usuarioId: number;
  eventoId: number;
  codigoUnico?: string; // nombre en backend
  codigo?: string; // alias en algunas respuestas
  fechaGeneracion: string;
  nombreAsistente?: string;
  numeroDocumento?: string;
  nombreEvento?: string;
  fechaEvento?: string;
  duracionHoras?: number;
  nombreOrganizador?: string | null;
  ciudadExpedicion?: string | null;
}

export interface PuntoSerie {
  periodo: string; // mes o etiqueta del eje X
  valor: number;
}

export interface FranjaOcupacionSalon {
  eventoId: number;
  nombreEvento: string;
  estado: EstadoEvento;
  inicio: string;
  fin: string;
  nombreOrganizador?: string;
}

export interface DisponibilidadSalon {
  ubicacion: string;
  desde: string;
  hasta: string;
  propuestaDisponible?: boolean | null; // si el horario nuevo cabe
  mensajePropuesta?: string;
  ocupaciones: FranjaOcupacionSalon[];
}

export interface DashboardOrganizador {
  organizadorId: number;
  eventosActivos: number;
  eventosPendientes: number;
  eventosCancelados: number;
  eventosTotales: number;
  totalInscritos: number;
  aforoMaximoPorEvento: number; // tope por evento, no del edificio
  cuposOcupadosEventosActivos: number;
  asistenciasUltimos30Dias: number;
  serieMensualEventos: PuntoSerie[];
  serieMensualInscripciones: PuntoSerie[];
}

export interface DashboardAdmin {
  eventosActivos: number;
  eventosPendientes: number;
  eventosCancelados: number;
  eventosTotales: number;
  usuariosTotales: number;
  usuariosActivos: number;
  usuariosPendientes: number;
  organizadoresActivos: number;
  asistentesTotales: number;
  staffTotales: number;
  inscripcionesTotales: number;
  serieMensualEventos: PuntoSerie[];
  serieMensualUsuarios: PuntoSerie[];
}
