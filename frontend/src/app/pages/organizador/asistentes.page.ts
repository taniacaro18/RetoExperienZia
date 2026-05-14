import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { AsistenteEvento, EstadoInscripcion, Evento } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { inscripcionEstadoSeverity } from '../../shared/estado.helpers';
import { ExportService } from '../../core/export/export.service';

interface OpcionEvento { label: string; value: number; }
type FiltroEstado = 'TODOS' | EstadoInscripcion;

@Component({
  selector: 'app-org-asistentes-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DatePipe,
    ButtonModule,
    InputTextModule,
    SelectModule,
    DialogModule,
    TagModule,
    ProgressSpinnerModule,
    StatCardComponent
  ],
  templateUrl: './asistentes.page.html'
})
export class OrgAsistentesPage {
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(AuthStore);
  private readonly eventoApi = inject(EventoApi);
  private readonly inscripcionApi = inject(InscripcionApi);
  private readonly messages = inject(MessageService);
  private readonly route = inject(ActivatedRoute);
  private readonly exportSvc = inject(ExportService);

  readonly cargando = signal(true);
  readonly cargandoAsistentes = signal(false);
  readonly eventos = signal<Evento[]>([]);
  readonly eventoSeleccionado = signal<number | null>(null);
  readonly asistentes = signal<AsistenteEvento[]>([]);
  readonly busqueda = signal('');
  readonly filtroEstado = signal<FiltroEstado>('TODOS');

  readonly modalManual = signal(false);
  readonly modalCsv = signal(false);
  readonly archivoCsv = signal<File | null>(null);
  readonly modalPreviewCsv = signal(false);
  readonly previewFilas = signal<Record<string, string>[]>([]);
  readonly previewTotalFilas = signal(0);
  readonly csvArchivoPendiente = signal<File | null>(null);
  readonly procesandoCarga = signal(false);
  readonly modalResultado = signal<{
    cuentasNuevasCreadas: number;
    inscripcionesRegistradas: number;
    filasOmitidasDuplicadoUOtros: number;
    errores: string[];
  } | null>(null);

  readonly opcionesEventos = computed<OpcionEvento[]>(() =>
    this.eventos()
      .filter((e) => e.estado === 'ACTIVO')
      .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())
      .map((e) => ({ label: e.nombre, value: e.id }))
  );

  readonly tiposDocumento = [
    { label: 'Cédula de Ciudadanía', value: 'CC' },
    { label: 'Cédula de Extranjería', value: 'CE' },
    { label: 'Tarjeta de Identidad', value: 'TI' },
    { label: 'Pasaporte', value: 'PA' }
  ];

  readonly stats = computed(() => {
    const items = this.asistentes();
    return {
      total: items.length,
      asistieron: items.filter((a) => a.estadoInscripcion === 'ASISTIO').length,
      inscritos: items.filter((a) => a.estadoInscripcion === 'INSCRITO').length,
      cancelados: items.filter((a) => a.estadoInscripcion === 'CANCELADO').length
    };
  });

  readonly asistentesFiltrados = computed(() => {
    const f = this.filtroEstado();
    if (f === 'TODOS') return this.asistentes();
    return this.asistentes().filter((a) => a.estadoInscripcion === f);
  });

  readonly eventoActual = computed(() =>
    this.eventos().find((e) => e.id === this.eventoSeleccionado())
  );

  readonly previewColumnas = computed(() => {
    const f = this.previewFilas();
    if (f.length === 0) return [] as string[];
    return Object.keys(f[0]);
  });

  readonly formManual = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    tipoDocumento: ['CC', Validators.required],
    numeroDocumento: ['', [Validators.required, Validators.minLength(4)]],
    telefono: ['']
  });

  estadoSeverity = inscripcionEstadoSeverity;

  ngOnInit() {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    const eventoIdQP = Number(this.route.snapshot.queryParamMap.get('evento')) || null;
    this.eventoApi.listarPorOrganizador(orgId).subscribe({
      next: (lista) => {
        this.eventos.set(lista);
        this.cargando.set(false);
        const activos = lista.filter((e) => e.estado === 'ACTIVO');
        // Si nos pasaron ?evento=ID y existe y está ACTIVO, lo seleccionamos.
        const target = eventoIdQP && activos.find((e) => e.id === eventoIdQP);
        if (target) {
          this.cambiarEvento(target.id);
        } else if (activos.length > 0) {
          this.cambiarEvento(activos[0].id);
        }
      },
      error: () => this.cargando.set(false)
    });
  }

  cambiarEvento(eventoId: number) {
    this.eventoSeleccionado.set(eventoId);
    this.busqueda.set('');
    this.filtroEstado.set('TODOS');
    this.recargarAsistentes();
  }

  buscarAsistentes() {
    this.recargarAsistentes();
  }

  cambiarFiltroEstado(f: FiltroEstado) {
    this.filtroEstado.set(f);
  }

  private recargarAsistentes() {
    const orgId = this.store.usuario()?.id;
    const eventoId = this.eventoSeleccionado();
    if (!orgId || !eventoId) return;
    this.cargandoAsistentes.set(true);
    this.inscripcionApi.asistentesParaOrganizador(eventoId, orgId, this.busqueda()).subscribe({
      next: (lista) => {
        this.asistentes.set(lista);
        this.cargandoAsistentes.set(false);
      },
      error: () => this.cargandoAsistentes.set(false)
    });
  }

  abrirModalManual() {
    if (!this.eventoSeleccionado()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Selecciona un evento',
        detail: 'Elige primero un evento ACTIVO al que asignar el asistente.'
      });
      return;
    }
    this.formManual.reset({
      nombre: '',
      email: '',
      tipoDocumento: 'CC',
      numeroDocumento: '',
      telefono: ''
    });
    this.modalManual.set(true);
  }
  cerrarModalManual() { this.modalManual.set(false); }

  enviarManual() {
    if (this.formManual.invalid) {
      this.formManual.markAllAsTouched();
      return;
    }
    const orgId = this.store.usuario()?.id;
    const eventoId = this.eventoSeleccionado();
    if (!orgId || !eventoId) return;

    const v = this.formManual.getRawValue();
    this.procesandoCarga.set(true);
    this.inscripcionApi.cargaManual(eventoId, orgId, [
      {
        nombre: v.nombre.trim(),
        email: v.email.trim().toLowerCase(),
        telefono: v.telefono?.trim() || undefined,
        tipoDocumento: v.tipoDocumento,
        numeroDocumento: v.numeroDocumento.trim()
      }
    ]).subscribe({
      next: (r) => {
        this.procesandoCarga.set(false);
        this.modalManual.set(false);
        if (r.errores.length > 0) {
          this.messages.add({
            severity: 'warn',
            summary: 'Carga con observaciones',
            detail: r.errores.join(' · '),
            life: 7000
          });
        } else {
          this.messages.add({
            severity: 'success',
            summary: 'Asistente registrado',
            detail: `Cuentas nuevas: ${r.cuentasNuevasCreadas} · Inscripciones: ${r.inscripcionesRegistradas}`,
            life: 5000
          });
        }
        this.recargarAsistentes();
      },
      error: () => this.procesandoCarga.set(false)
    });
  }

  abrirModalCsv() {
    if (!this.eventoSeleccionado()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Selecciona un evento',
        detail: 'Elige primero un evento ACTIVO antes de importar.'
      });
      return;
    }
    this.archivoCsv.set(null);
    this.previewFilas.set([]);
    this.csvArchivoPendiente.set(null);
    this.modalPreviewCsv.set(false);
    this.modalCsv.set(true);
  }
  cerrarModalCsv() {
    this.modalCsv.set(false);
    this.archivoCsv.set(null);
    this.previewFilas.set([]);
    this.csvArchivoPendiente.set(null);
  }

  cerrarPreviewCsv() {
    this.modalPreviewCsv.set(false);
    this.csvArchivoPendiente.set(null);
    this.previewFilas.set([]);
  }

  archivoSeleccionado(ev: Event) {
    const input = ev.target as HTMLInputElement;
    this.archivoCsv.set(input.files?.[0] ?? null);
  }

  descargarPlantilla() {
    this.exportSvc.descargarPlantillaExcel(
      'plantilla_asistentes',
      'Asistentes',
      ['nombre', 'email', 'telefono', 'tipoDocumento', 'numeroDocumento'],
      [
        ['Juan Perez', 'juan.perez@correo.com', '3001234567', 'CC', '1010101010'],
        ['Ana Garcia', 'ana.garcia@correo.com', '3009876543', 'CC', '2020202020']
      ]
    );
  }

  /** Lee el archivo, valida columnas y muestra previsualización antes de enviar al API. */
  async previsualizarCsv() {
    const archivo = this.archivoCsv();
    const orgId = this.store.usuario()?.id;
    const eventoId = this.eventoSeleccionado();
    if (!archivo || !orgId || !eventoId) return;

    this.procesandoCarga.set(true);

    try {
      const filas = await this.exportSvc.leerExcelOCsv(archivo);
      const requeridas = ['nombre', 'email', 'tipoDocumento', 'numeroDocumento'];
      const headers = filas.length > 0 ? Object.keys(filas[0]).map((k) => k.trim()) : [];
      const faltantes = requeridas.filter((c) => !headers.includes(c));
      if (filas.length === 0) {
        this.procesandoCarga.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Archivo vacío',
          detail: 'El archivo no contiene filas con datos.'
        });
        return;
      }
      if (faltantes.length > 0) {
        this.procesandoCarga.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Encabezados inválidos',
          detail: 'Faltan columnas obligatorias: ' + faltantes.join(', '),
          life: 7000
        });
        return;
      }

      const headersOrden = ['nombre', 'email', 'telefono', 'tipoDocumento', 'numeroDocumento'];
      const filasArr: (string | number)[][] = filas.map((f) =>
        headersOrden.map((h) => (f[h] ?? '').toString().trim())
      );
      const csv = this.exportSvc.filasACsv(headersOrden, filasArr);
      const csvFile = new File([csv], 'asistentes.csv', { type: 'text/csv' });

      this.previewTotalFilas.set(filas.length);
      this.previewFilas.set(filas.slice(0, 30) as Record<string, string>[]);
      this.csvArchivoPendiente.set(csvFile);
      this.procesandoCarga.set(false);
      this.modalCsv.set(false);
      this.modalPreviewCsv.set(true);
    } catch (e: any) {
      this.procesandoCarga.set(false);
      this.messages.add({
        severity: 'error',
        summary: 'No se pudo leer el archivo',
        detail: e?.message || 'Asegúrate de subir un .xlsx, .xls o .csv válido.'
      });
    }
  }

  confirmarCargaCsv() {
    const csvFile = this.csvArchivoPendiente();
    const orgId = this.store.usuario()?.id;
    const eventoId = this.eventoSeleccionado();
    if (!csvFile || !orgId || !eventoId) return;

    this.procesandoCarga.set(true);
    this.inscripcionApi.cargaCsv(eventoId, orgId, csvFile).subscribe({
      next: (r) => {
        this.procesandoCarga.set(false);
        this.modalPreviewCsv.set(false);
        this.csvArchivoPendiente.set(null);
        this.previewFilas.set([]);
        this.archivoCsv.set(null);
        this.modalResultado.set(r);
        this.recargarAsistentes();
      },
      error: () => this.procesandoCarga.set(false)
    });
  }

  cerrarResultado() { this.modalResultado.set(null); }

  exportarExcel() {
    const items = this.asistentesFiltrados();
    if (items.length === 0) {
      this.messages.add({ severity: 'info', summary: 'Sin datos', detail: 'No hay asistentes para exportar.' });
      return;
    }
    const evento = this.eventoActual();
    const nombre = evento ? `asistentes_${evento.nombre.replace(/\s+/g, '_')}` : 'asistentes';
    this.exportSvc.exportarExcel(nombre, 'Asistentes', this.columnasExport(), items);
  }

  exportarPdf() {
    const items = this.asistentesFiltrados();
    if (items.length === 0) {
      this.messages.add({ severity: 'info', summary: 'Sin datos', detail: 'No hay asistentes para exportar.' });
      return;
    }
    const evento = this.eventoActual();
    const nombre = evento ? `asistentes_${evento.nombre.replace(/\s+/g, '_')}` : 'asistentes';
    const titulo = evento ? `Asistentes · ${evento.nombre}` : 'Asistentes';
    this.exportSvc.exportarPdf(nombre, titulo, this.columnasExport(), items,
      `Total: ${items.length} asistente(s)`);
  }

  private columnasExport() {
    return [
      { header: 'Nombre', value: (a: AsistenteEvento) => a.nombre },
      { header: 'Email', value: (a: AsistenteEvento) => a.email },
      { header: 'Teléfono', value: (a: AsistenteEvento) => a.telefono ?? '' },
      { header: 'Tipo doc.', value: (a: AsistenteEvento) => a.tipoDocumento ?? '' },
      { header: 'Número doc.', value: (a: AsistenteEvento) => a.numeroDocumento ?? '' },
      { header: 'Estado', value: (a: AsistenteEvento) => a.estadoInscripcion },
      { header: 'Inscripción', value: (a: AsistenteEvento) => a.fechaInscripcion ?? '' },
      { header: 'Check-in', value: (a: AsistenteEvento) => a.fechaCheckIn ?? '' },
      { header: 'Check-out', value: (a: AsistenteEvento) => a.fechaCheckOut ?? '' }
    ];
  }
}
