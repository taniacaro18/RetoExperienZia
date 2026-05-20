/**
 * Diálogo para consultar si el salón está libre en un mes y un día concreto.
 * Llama al API de eventos y muestra franjas ocupadas; sirve al crear o editar eventos.
 */
import { Component, effect, inject, input, model, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { DatePickerModule } from 'primeng/datepicker';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { EventoApi } from '../../core/api/evento.api';
import { DisponibilidadSalon, FranjaOcupacionSalon } from '../../core/models/domain.models';
import { eventoEstadoLabel, eventoEstadoSeverity } from '../estado.helpers';

// Formato ISO local sin zona horaria rara (para el backend).
function toLocalDateTimeIso(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

// Junta la fecha del día con la hora de inicio y fin (si fin < inicio, fin es al día siguiente).
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

// Comprueba si dos fechas son el mismo día del calendario.
function mismoDia(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

@Component({
  selector: 'app-salon-disponibilidad-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DialogModule, DatePickerModule, ProgressSpinnerModule, TagModule],
  templateUrl: './salon-disponibilidad-dialog.component.html'
})
export class SalonDisponibilidadDialogComponent {
  private readonly eventoApi = inject(EventoApi);

  // Si el modal está abierto o cerrado (two-way binding con el padre).
  readonly visible = model(false);
  // Nombre del salón a consultar.
  readonly ubicacion = input('Salón principal');
  // Al editar un evento, no contar su propia ocupación.
  readonly excluirEventoId = input<number | null>(null);
  // Fecha y horas del formulario del evento (para marcar la propuesta en el API).
  readonly fechaEvento = input<Date | null>(null);
  readonly horaInicio = input<Date | null>(null);
  readonly horaFin = input<Date | null>(null);

  readonly cargando = signal(false);
  // Primer día del mes que estamos viendo en el calendario.
  readonly mesReferencia = signal(new Date());
  // Día que el usuario eligió en el calendario (panel derecho).
  readonly diaConsulta = signal<Date>(new Date());
  // Respuesta completa del backend para ese mes.
  readonly datos = signal<DisponibilidadSalon | null>(null);

  // Reutilizamos los helpers de etiquetas de estado de evento.
  estadoLabel = eventoEstadoLabel;
  estadoSeverity = eventoEstadoSeverity;

  // Solo las ocupaciones del día seleccionado.
  readonly ocupacionesDelDia = signal<FranjaOcupacionSalon[]>([]);

  constructor() {
    // Cuando se abre el diálogo, alinea el día con la fecha del formulario y pide datos.
    effect(() => {
      if (this.visible()) {
        const f = this.fechaEvento();
        const base = f ? new Date(f) : new Date();
        base.setHours(12, 0, 0, 0);
        this.diaConsulta.set(base);
        this.mesReferencia.set(new Date(base.getFullYear(), base.getMonth(), 1));
        this.cargar();
      }
    });
  }

  // El usuario cambió de día en el datepicker.
  onDiaChange(d: Date | null) {
    if (!d) return;
    const normalizado = new Date(d);
    normalizado.setHours(12, 0, 0, 0);
    this.diaConsulta.set(normalizado);
    const mesActual = this.mesReferencia();
    if (
      normalizado.getFullYear() !== mesActual.getFullYear() ||
      normalizado.getMonth() !== mesActual.getMonth()
    ) {
      this.mesReferencia.set(new Date(normalizado.getFullYear(), normalizado.getMonth(), 1));
      this.cargar();
    } else {
      this.actualizarOcupacionesDia();
    }
  }

  // Pide al backend ocupaciones del mes (y opcionalmente valida la propuesta del formulario).
  cargar() {
    const ref = this.mesReferencia();
    const desde = new Date(ref.getFullYear(), ref.getMonth(), 1, 0, 0, 0);
    const hasta = new Date(ref.getFullYear(), ref.getMonth() + 1, 0, 23, 59, 59);

    let propuestaInicio: string | undefined;
    let propuestaFin: string | undefined;
    const fecha = this.fechaEvento();
    const hi = this.horaInicio();
    const hf = this.horaFin();
    if (fecha && hi && hf) {
      const v = ventanaHorariaLocal(fecha, hi, hf);
      if (v) {
        propuestaInicio = toLocalDateTimeIso(v.inicio);
        propuestaFin = toLocalDateTimeIso(v.fin);
      }
    }

    this.cargando.set(true);
    this.eventoApi
      .consultarDisponibilidadSalon({
        ubicacion: (this.ubicacion() || '').trim() || 'Salón principal',
        desde: toLocalDateTimeIso(desde),
        hasta: toLocalDateTimeIso(hasta),
        excluirEventoId: this.excluirEventoId() ?? undefined,
        propuestaInicio,
        propuestaFin
      })
      .subscribe({
        next: (d) => {
          this.datos.set(d);
          this.actualizarOcupacionesDia();
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false)
      });
  }

  // Filtra del mes solo las franjas que caen en diaConsulta.
  actualizarOcupacionesDia() {
    const d = this.datos();
    const fecha = this.diaConsulta();
    if (!d) {
      this.ocupacionesDelDia.set([]);
      return;
    }
    const lista = (d.ocupaciones ?? []).filter((o) => {
      const ini = new Date(o.inicio);
      return mismoDia(ini, fecha);
    });
    this.ocupacionesDelDia.set(lista);
  }

  cerrar() {
    this.visible.set(false);
  }
}
