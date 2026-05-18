export type Rol = 'ADMIN' | 'ORGANIZADOR' | 'ASISTENTE' | 'STAFF';
export type EstadoUsuario = 'ACTIVO' | 'PENDIENTE' | 'RECHAZADO' | 'INACTIVO';
export type TipoEvento = 'PUBLICO' | 'PRIVADO';
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
export type EstadoInscripcion = 'INSCRITO' | 'ASISTIO' | 'CANCELADO';
export type EstadoPago = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
export type TipoNovedadEvento =
  | 'EDICION_METADATOS'
  | 'EDICION_TIPO_CATEGORIA'
  | 'AUMENTO_HORAS'
  | 'DISMINUCION_HORAS'
  | 'CANCELACION_SOLICITUD';
export type EstadoNovedadEvento = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
export type FuncionStaff = 'CHECK_IN_QR' | 'CHECK_IN_MANUAL' | 'REGISTRO_SALIDA' | 'GENERAL';
export type TipoNotificacion = 'INFO' | 'ALERTA' | 'ERROR';

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  password?: string | null;
  telefono?: string | null;
  tipoDocumento?: string | null;
  numeroDocumento?: string | null;
  rol: Rol;
  estado: EstadoUsuario;
  organizadorId?: number | null;
  tipo?: 'ASISTENTE' | 'ORGANIZADOR';
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Respuesta de POST /api/usuarios/login */
export interface LoginResponse {
  accessToken: string;
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
  fecha: string;
  fechaFin?: string;
  ubicacion?: string;
  tipoEvento: TipoEvento;
  estado: EstadoEvento;
  aforoMaximo: number;
  aforoActual: number;
  costo: number;
  organizadorId: number;
  /** Expuesto por el API (no en catálogo público anonimizado). */
  organizadorNombre?: string | null;
  organizadorEmail?: string | null;
  imagen?: string | null;
  categoria?: string | null;
  duracionHoras?: number | null;
  motivoRechazo?: string | null;
  motivoCancelacion?: string | null;
  /** Texto para el admin: qué cambió en la solicitud de edición. */
  resumenSolicitudEdicion?: string | null;
  estadoPrevioRevision?: EstadoEvento | null;
  /** Mensaje de negocio devuelto por el API al editar (no persistido). */
  alertaNegocio?: string | null;
}

/** Historial de solicitudes y cambios del evento (GET /api/eventos/{id}/novedades). */
export interface EventoNovedad {
  id: number;
  eventoId: number;
  usuarioSolicitanteId?: number | null;
  tipo: TipoNovedadEvento;
  estado: EstadoNovedadEvento;
  fechaSolicitud: string;
  fechaResolucion?: string | null;
  motivoResolucion?: string | null;
  detalleJson?: string | null;
}

export interface Inscripcion {
  id: number;
  usuarioId: number;
  eventoId: number;
  fechaInscripcion: string;
  estado: EstadoInscripcion;
  fechaCheckIn?: string | null;
  fechaCheckOut?: string | null;
  codigoQR?: string | null;
  /** Solo en respuestas de check-in / check-out (QR o manual). */
  nombreAsistente?: string | null;
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
  /** Código QR de la inscripción (búsqueda y verificación en staff). */
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
  asistencias: number;
  presentes: number;
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
  funcion: FuncionStaff;
}

export interface Pago {
  id: number;
  eventoId: number;
  organizadorId: number;
  comprobanteUrl?: string;
  monto?: number;
  /** Si existe, el comprobante pendiente es complemento sobre este monto ya aprobado. */
  saldoAprobadoPrevio?: number | null;
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
  usuarioId?: number | null;
  accion: string;
  entidad: string;
  entidadId?: number | null;
  fecha: string;
  /** IP del cliente (auditoría tipo ROOM_911) */
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
  /** API backend (codigoUnico) */
  codigoUnico?: string;
  codigo?: string;
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
  periodo: string;
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
  propuestaDisponible?: boolean | null;
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
  /** Límite por evento (no capacidad global del salón). */
  aforoMaximoPorEvento: number;
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
