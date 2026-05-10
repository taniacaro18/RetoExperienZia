export type Rol = 'ADMIN' | 'ORGANIZADOR' | 'ASISTENTE' | 'STAFF';
export type EstadoUsuario = 'ACTIVO' | 'PENDIENTE' | 'RECHAZADO' | 'INACTIVO';
export type TipoEvento = 'PUBLICO' | 'PRIVADO';
export type EstadoEvento = 'PENDIENTE' | 'APROBADO' | 'ACTIVO' | 'RECHAZADO' | 'CANCELADO' | 'FINALIZADO';
export type EstadoInscripcion = 'INSCRITO' | 'ASISTIO' | 'CANCELADO';
export type EstadoPago = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
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

export interface ActualizarPerfil {
  nombre?: string;
  email?: string;
  telefono?: string;
  tipoDocumento?: string;
  numeroDocumento?: string;
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
  imagen?: string | null;
  categoria?: string | null;
  duracionHoras?: number | null;
  motivoRechazo?: string | null;
  motivoCancelacion?: string | null;
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
}

export interface AsistenteEvento {
  inscripcionId: number;
  usuarioId: number;
  nombre: string;
  email: string;
  telefono?: string;
  tipoDocumento?: string;
  numeroDocumento?: string;
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
  codigo: string;
  fechaGeneracion: string;
  nombreAsistente?: string;
  numeroDocumento?: string;
  nombreEvento?: string;
  fechaEvento?: string;
  duracionHoras?: number;
}

export interface PuntoSerie {
  periodo: string;
  valor: number;
}

export interface DashboardOrganizador {
  organizadorId: number;
  eventosActivos: number;
  eventosPendientes: number;
  eventosCancelados: number;
  eventosTotales: number;
  totalInscritos: number;
  aforoMaximoTotal: number;
  aforoDisponibleTotal: number;
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
