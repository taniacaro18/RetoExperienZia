import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { InputNumberModule } from 'primeng/inputnumber';
import { DatePickerModule } from 'primeng/datepicker';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { Evento } from '../../core/models/domain.models';
import { eventoVentanaYaCerro } from '../../shared/evento-catalogo.helpers';
import { SalonDisponibilidadDialogComponent } from '../../shared/salon-disponibilidad/salon-disponibilidad-dialog.component';

const PRECIO_POR_HORA = 100000;
const AFORO_MAXIMO_PERMITIDO = 600;
const UBICACION_DEFECTO = 'Salón principal';

/** ISO sin zona: el backend usa LocalDateTime; evita el desfase de `toISOString()` (UTC). */
function toLocalDateTimeIso(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

/**
 * Inicio/fin del evento en fecha del calendario: si la hora de fin es menor o igual que la de inicio,
 * el fin se interpreta al día siguiente (misma noche).
 */
function ventanaHorariaLocal(
  fecha: Date,
  horaInicio: Date,
  horaFin: Date
): { inicio: Date; fin: Date } | null {
  const base = new Date(fecha);
  const inicio = new Date(base);
  inicio.setHours(horaInicio.getHours(), horaInicio.getMinutes(), 0, 0);
  const fin = new Date(base);
  fin.setHours(horaFin.getHours(), horaFin.getMinutes(), 0, 0);
  if (fin.getTime() <= inicio.getTime()) {
    fin.setDate(fin.getDate() + 1);
  }
  return { inicio, fin };
}

function minutosDelDia(d: Date): number {
  return d.getHours() * 60 + d.getMinutes();
}

function fechaFutura(control: AbstractControl): ValidationErrors | null {
  const v = control.value;
  if (!v) return null;
  const f = new Date(v);
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  return f.getTime() < hoy.getTime() ? { fechaPasada: true } : null;
}

@Component({
  selector: 'app-org-evento-form-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    InputNumberModule,
    DatePickerModule,
    ProgressSpinnerModule,
    SalonDisponibilidadDialogComponent
  ],
  templateUrl: './evento-form.page.html'
})
export class OrgEventoFormPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(EventoApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly store = inject(AuthStore);
  private readonly messages = inject(MessageService);

  readonly editando = signal(false);
  readonly eventoId = signal<number | null>(null);
  readonly cargandoInicial = signal(false);
  readonly guardando = signal(false);
  readonly minFecha = new Date();
  readonly imagenError = signal(false);
  /** Al editar: costo original del API (para textos de ayuda). */
  readonly costoEventoCargado = signal<number | null>(null);
  readonly mostrarDisponibilidadSalon = signal(false);

  readonly precioPorHora = PRECIO_POR_HORA;
  readonly aforoMaximoPermitido = AFORO_MAXIMO_PERMITIDO;
  readonly ubicacionDefecto = UBICACION_DEFECTO;

  readonly categorias = [
    { label: 'Fiesta', value: 'FIESTA' },
    { label: 'Concierto', value: 'CONCIERTO' },
    { label: 'Juegos', value: 'JUEGOS' },
    { label: 'Recepción', value: 'RECEPCION' },
    { label: 'Otros', value: 'OTROS' }
  ];

  readonly tiposEvento = [
    { label: 'Público (en catálogo general)', value: 'PUBLICO' },
    { label: 'Privado (solo asistentes invitados)', value: 'PRIVADO' }
  ];

  readonly formulario = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    categoria: ['FIESTA', Validators.required],
    tipoEvento: ['PUBLICO' as 'PUBLICO' | 'PRIVADO', Validators.required],
    descripcion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    aforoMaximo: [
      50,
      [Validators.required, Validators.min(1), Validators.max(AFORO_MAXIMO_PERMITIDO)]
    ],
    fecha: [null as Date | null, [Validators.required, fechaFutura]],
    horaInicio: [null as Date | null, Validators.required],
    horaFin: [null as Date | null, Validators.required],
    ubicacion: [UBICACION_DEFECTO, [Validators.maxLength(200)]],
    imagen: ['', [Validators.maxLength(500)]]
  });

  readonly tituloFigma = computed(() =>
    this.editando() ? 'Editar Evento' : 'Crear Evento'
  );

  readonly imagenUrl = computed(() => {
    const v = this.formulario.controls.imagen.value;
    if (!v || this.imagenError()) return null;
    if (!/^https?:\/\//i.test(v)) return null;
    return v;
  });

  /** Duración en horas calculada a partir de horaInicio y horaFin (redondeo hacia arriba). */
  readonly duracionCalculada = computed(() => {
    const fecha = this.formulario.controls.fecha.value;
    const inicio = this.formulario.controls.horaInicio.value;
    const fin = this.formulario.controls.horaFin.value;
    if (!fecha || !inicio || !fin) return 0;
    const v = ventanaHorariaLocal(fecha, inicio, fin);
    if (!v) return 0;
    const minutos = (v.fin.getTime() - v.inicio.getTime()) / 60000;
    if (minutos <= 0) return 0;
    return Math.ceil(minutos / 60);
  });

  /** Costo automático = precio por hora * duración. */
  readonly costoCalculado = computed(() => this.precioPorHora * Math.max(0, this.duracionCalculada()));

  /** Misma hora en el reloj (mismo día calendario del evento) → duración cero; no permitir guardar. */
  readonly horaInicioFinIguales = computed(() => {
    const a = this.formulario.controls.horaInicio.value;
    const b = this.formulario.controls.horaFin.value;
    if (!a || !b) return false;
    return minutosDelDia(a) === minutosDelDia(b);
  });

  /** Solo reloj: fin antes que inicio → se guardará el fin al día siguiente. */
  readonly ubicacionParaSalon = computed(
    () => this.formulario.controls.ubicacion.value?.trim() || UBICACION_DEFECTO
  );

  readonly cruzaMedianoche = computed(() => {
    const a = this.formulario.controls.horaInicio.value;
    const b = this.formulario.controls.horaFin.value;
    if (!a || !b) return false;
    return minutosDelDia(b) < minutosDelDia(a);
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.eventoId.set(id);
      this.editando.set(true);
      this.cargarEvento(id);
    }
    this.formulario.controls.imagen.valueChanges.subscribe(() => this.imagenError.set(false));
  }

  private cargarEvento(id: number) {
    this.cargandoInicial.set(true);
    this.api.obtener(id).subscribe({
      next: (e) => {
        const orgId = this.store.usuario()?.id;
        if (orgId && e.organizadorId !== orgId) {
          this.messages.add({
            severity: 'error',
            summary: 'No autorizado',
            detail: 'Este evento no te pertenece.'
          });
          this.router.navigate(['/organizador/eventos']);
          return;
        }
        if (['PENDIENTE_REVISION', 'PENDIENTE_SUPLEMENTO', 'PENDIENTE_CANCELACION'].includes(e.estado)) {
          this.messages.add({
            severity: 'info',
            summary: 'Edición no disponible',
            detail:
              e.estado === 'PENDIENTE_SUPLEMENTO'
                ? 'Este evento tiene un pago adicional pendiente. Completa el proceso en Pagos o espera la revisión del administrador.'
                : 'Hay un trámite pendiente con el administrador. No puedes editar el evento hasta que se resuelva.'
          });
          this.router.navigate(['/organizador/eventos']);
          return;
        }
        if (e.estado === 'FINALIZADO' || (e.estado === 'ACTIVO' && eventoVentanaYaCerro(e))) {
          this.messages.add({
            severity: 'info',
            summary: 'Evento finalizado',
            detail: 'Este evento ya terminó; no se puede editar.'
          });
          this.router.navigate(['/organizador/eventos']);
          return;
        }
        const fechaInicio = new Date(e.fecha);
        const horasFallback =
          e.duracionHoras != null && e.duracionHoras > 0 ? e.duracionHoras : 1;
        const fechaFin = e.fechaFin
          ? new Date(e.fechaFin)
          : new Date(fechaInicio.getTime() + horasFallback * 3600000);
        this.costoEventoCargado.set(e.costo ?? 0);
        this.formulario.patchValue({
          nombre: e.nombre,
          categoria: (e.categoria || 'FIESTA').toUpperCase(),
          tipoEvento: e.tipoEvento,
          descripcion: e.descripcion || '',
          aforoMaximo: e.aforoMaximo,
          fecha: fechaInicio,
          horaInicio: fechaInicio,
          horaFin: fechaFin,
          ubicacion: e.ubicacion || UBICACION_DEFECTO,
          imagen: e.imagen || ''
        });
        this.cargandoInicial.set(false);
      },
      error: () => this.cargandoInicial.set(false)
    });
  }

  volver() {
    this.router.navigate(['/organizador/eventos']);
  }

  onImagenError() {
    this.imagenError.set(true);
  }

  /**
   * Devuelve una lista legible con todos los problemas concretos del formulario.
   * Se usa en el toast de "Faltan datos" para que el organizador sepa exactamente
   * qué corregir sin tener que adivinar.
   */
  private obtenerProblemas(): string[] {
    const f = this.formulario.controls;
    const problemas: string[] = [];

    if (f.nombre.errors?.['required']) problemas.push('Nombre del evento');
    else if (f.nombre.errors?.['minlength']) problemas.push('Nombre: mínimo 3 caracteres');

    if (f.categoria.errors?.['required']) problemas.push('Categoría');
    if (f.tipoEvento.errors?.['required']) problemas.push('Tipo de evento');

    if (f.descripcion.errors?.['required']) problemas.push('Descripción');
    else if (f.descripcion.errors?.['minlength']) {
      const actual = f.descripcion.value?.length ?? 0;
      problemas.push(`Descripción: mínimo 10 caracteres (tienes ${actual})`);
    }

    if (f.aforoMaximo.errors?.['required']) problemas.push('Aforo máximo');
    else if (f.aforoMaximo.errors?.['min']) problemas.push('Aforo mínimo: 1 persona');
    else if (f.aforoMaximo.errors?.['max']) {
      problemas.push(`Aforo máximo: ${AFORO_MAXIMO_PERMITIDO} personas`);
    }

    if (f.fecha.errors?.['required']) problemas.push('Fecha del evento');
    else if (f.fecha.errors?.['fechaPasada']) problemas.push('La fecha no puede ser pasada');

    if (f.horaInicio.errors?.['required']) problemas.push('Hora de inicio');
    if (f.horaFin.errors?.['required']) problemas.push('Hora de fin');

    if (!f.horaInicio.errors && !f.horaFin.errors && this.horaInicioFinIguales()) {
      problemas.push('La hora de fin no puede ser igual a la de inicio');
    }

    return problemas;
  }

  enviar() {
    // Calculamos cada problema concreto del formulario para que el toast
    // enumere QUÉ falta en vez de un genérico "Revisa los campos obligatorios".
    const problemas = this.obtenerProblemas();
    if (problemas.length > 0) {
      this.formulario.markAllAsTouched();
      this.messages.add({
        severity: 'warn',
        summary: 'Faltan datos',
        detail: problemas.join(' · '),
        life: 6000
      });
      return;
    }
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;

    const v = this.formulario.getRawValue();
    const ventana = ventanaHorariaLocal(v.fecha!, v.horaInicio!, v.horaFin!);
    if (!ventana) return;
    const { inicio, fin } = ventana;

    if (inicio.getTime() < Date.now()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Fecha inválida',
        detail: 'La fecha y hora de inicio deben ser posteriores a este momento.'
      });
      return;
    }

    const ubicacionFinal = (v.ubicacion?.trim() || UBICACION_DEFECTO);

    const payload: Partial<Evento> = {
      nombre: v.nombre.trim(),
      categoria: v.categoria,
      tipoEvento: v.tipoEvento,
      descripcion: v.descripcion.trim(),
      aforoMaximo: v.aforoMaximo,
      fecha: toLocalDateTimeIso(inicio),
      fechaFin: toLocalDateTimeIso(fin),
      ubicacion: ubicacionFinal,
      imagen: v.imagen?.trim() || undefined,
      organizadorId: orgId
    };

    this.guardando.set(true);
    if (this.editando()) {
        this.api.editar(this.eventoId()!, payload).subscribe({
        next: (ev) => {
          this.guardando.set(false);
          const alerta = ev.alertaNegocio?.trim();
          let detail: string;
          let severity: 'success' | 'info' | 'warn' = 'success';
          if (alerta) {
            detail = alerta;
            severity =
              ev.estado === 'PENDIENTE_SUPLEMENTO' || ev.estado === 'PENDIENTE_REVISION'
                ? 'info'
                : 'success';
          } else if (ev.estado === 'PENDIENTE_REVISION') {
            detail = alerta?.includes('excedente')
              ? alerta
              : 'Los cambios quedaron pendientes de aprobación del administrador. No se solicita nuevo pago salvo que hayas ampliado horas (revisa Pagos si aplica).';
            severity = 'info';
          } else if (ev.estado === 'PENDIENTE_SUPLEMENTO') {
            detail =
              'El administrador aprobó la ampliación de horas. Sube en Pagos el comprobante solo por el incremento pendiente.';
            severity = 'warn';
          } else if (ev.estado === 'PENDIENTE_CANCELACION') {
            detail =
              'Solicitud de cancelación registrada. Un administrador la revisará; si se aprueba, la devolución orientativa es del 70% del valor pagado.';
            severity = 'info';
          } else if (ev.estado === 'PENDIENTE') {
            detail =
              'Los cambios se enviaron a re-aprobación del administrador. Revisa el detalle en la bandeja de eventos.';
            severity = 'info';
          } else {
            detail = 'Los cambios se guardaron correctamente.';
          }
          this.messages.add({
            severity,
            summary: 'Evento actualizado',
            detail,
            life: 7000
          });
          this.router.navigate(['/organizador/eventos']);
        },
        error: () => this.guardando.set(false)
      });
    } else {
      this.api.crear(payload).subscribe({
        next: () => {
          this.guardando.set(false);
          this.messages.add({
            severity: 'success',
            summary: 'Solicitud creada',
            detail: 'Tu evento quedó PENDIENTE de aprobación por el administrador.',
            life: 5500
          });
          this.router.navigate(['/organizador/eventos']);
        },
        error: () => this.guardando.set(false)
      });
    }
  }
}
