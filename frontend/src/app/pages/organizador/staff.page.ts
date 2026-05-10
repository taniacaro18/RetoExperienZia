import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { UsuarioApi } from '../../core/api/usuario.api';
import { Evento, FuncionStaff, Usuario } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { usuarioEstadoSeverity } from '../../shared/estado.helpers';
import { forkJoin } from 'rxjs';

interface StaffConAsignacion {
  usuario: Usuario;
  asignaciones: { eventoId: number; eventoNombre: string; funcion: FuncionStaff }[];
}

type FiltroEstado = 'TODOS' | 'ACTIVO' | 'INACTIVO';

@Component({
  selector: 'app-org-staff-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    SelectModule,
    DialogModule,
    TagModule,
    ProgressSpinnerModule,
    ConfirmDialogModule,
    StatCardComponent
  ],
  providers: [ConfirmationService],
  templateUrl: './staff.page.html'
})
export class OrgStaffPage {
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(AuthStore);
  private readonly eventoApi = inject(EventoApi);
  private readonly inscripcionApi = inject(InscripcionApi);
  private readonly usuarioApi = inject(UsuarioApi);
  private readonly messages = inject(MessageService);
  private readonly confirm = inject(ConfirmationService);

  readonly cargando = signal(true);
  readonly staffs = signal<StaffConAsignacion[]>([]);
  readonly eventos = signal<Evento[]>([]);

  readonly busqueda = signal('');
  readonly filtroEstado = signal<FiltroEstado>('TODOS');

  readonly modalCrear = signal(false);
  readonly modalAsignar = signal<{ staffId: number; staffNombre: string } | null>(null);
  readonly procesando = signal(false);

  readonly tiposDocumento = [
    { label: 'Cédula de Ciudadanía', value: 'CC' },
    { label: 'Cédula de Extranjería', value: 'CE' },
    { label: 'Tarjeta de Identidad', value: 'TI' },
    { label: 'Pasaporte', value: 'PA' }
  ];

  readonly funciones = [
    { label: 'Check-in / QR', value: 'CHECK_IN_QR' as FuncionStaff },
    { label: 'Check-in Manual', value: 'CHECK_IN_MANUAL' as FuncionStaff },
    { label: 'Registro de Salida', value: 'REGISTRO_SALIDA' as FuncionStaff },
    { label: 'General (multi-función)', value: 'GENERAL' as FuncionStaff }
  ];

  readonly opcionesEventos = computed(() =>
    this.eventos()
      .filter((e) => e.estado === 'ACTIVO')
      .map((e) => ({ label: e.nombre, value: e.id }))
  );

  readonly stats = computed(() => {
    const items = this.staffs();
    return {
      total: items.length,
      activos: items.filter((s) => s.usuario.estado === 'ACTIVO').length,
      inactivos: items.filter((s) => s.usuario.estado === 'INACTIVO').length,
      asignados: items.filter((s) => s.asignaciones.length > 0).length
    };
  });

  readonly staffsFiltrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const f = this.filtroEstado();
    let lista = [...this.staffs()];
    if (f !== 'TODOS') lista = lista.filter((s) => s.usuario.estado === f);
    if (q) {
      lista = lista.filter(
        (s) =>
          s.usuario.nombre.toLowerCase().includes(q) ||
          s.usuario.email.toLowerCase().includes(q) ||
          (s.usuario.numeroDocumento || '').toLowerCase().includes(q)
      );
    }
    return lista;
  });

  readonly formCrear = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    tipoDocumento: ['CC', Validators.required],
    numeroDocumento: ['', [Validators.required, Validators.minLength(4)]],
    telefono: [''],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });

  readonly formAsignar = this.fb.nonNullable.group({
    eventoId: [null as number | null, Validators.required],
    funcion: ['GENERAL' as FuncionStaff, Validators.required]
  });

  estadoSeverity = usuarioEstadoSeverity;

  ngOnInit() {
    this.cargar();
  }

  private cargar() {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.cargando.set(true);

    forkJoin({
      eventos: this.eventoApi.listarPorOrganizador(orgId),
      misStaff: this.usuarioApi.buscar({ rol: 'STAFF', organizadorId: orgId })
    }).subscribe({
      next: ({ eventos, misStaff }) => {
        this.eventos.set(eventos);

        if (misStaff.length === 0) {
          this.staffs.set([]);
          this.cargando.set(false);
          return;
        }
        const peticiones = misStaff.map((u) => this.inscripcionApi.eventosDelStaff(u.id));
        forkJoin(peticiones).subscribe({
          next: (asigsList) => {
            const items: StaffConAsignacion[] = misStaff.map((u, i) => ({
              usuario: u,
              asignaciones: (asigsList[i] || []).map((sa) => ({
                eventoId: sa.eventoId,
                eventoNombre: sa.nombreEvento,
                funcion: sa.funcion
              }))
            }));
            this.staffs.set(this.ordenar(items));
            this.cargando.set(false);
          },
          error: () => {
            this.staffs.set(misStaff.map((u) => ({ usuario: u, asignaciones: [] })));
            this.cargando.set(false);
          }
        });
      },
      error: () => this.cargando.set(false)
    });
  }

  private ordenar(items: StaffConAsignacion[]): StaffConAsignacion[] {
    return items.sort((a, b) => {
      // Activos primero, luego por nombre
      if (a.usuario.estado !== b.usuario.estado) {
        if (a.usuario.estado === 'ACTIVO') return -1;
        if (b.usuario.estado === 'ACTIVO') return 1;
      }
      return a.usuario.nombre.localeCompare(b.usuario.nombre);
    });
  }

  private nombreEvento(id: number): string {
    return this.eventos().find((e) => e.id === id)?.nombre ?? 'Evento';
  }

  limpiarFiltros() {
    this.busqueda.set('');
    this.filtroEstado.set('TODOS');
  }

  // ---- Crear staff ----
  abrirCrear() {
    this.formCrear.reset({
      nombre: '',
      email: '',
      tipoDocumento: 'CC',
      numeroDocumento: '',
      telefono: '',
      password: ''
    });
    this.modalCrear.set(true);
  }
  cerrarCrear() {
    this.modalCrear.set(false);
  }
  enviarCrear() {
    if (this.formCrear.invalid) {
      this.formCrear.markAllAsTouched();
      return;
    }
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    const v = this.formCrear.getRawValue();
    this.procesando.set(true);
    this.usuarioApi.crearStaff({
      organizadorId: orgId,
      nombre: v.nombre.trim(),
      email: v.email.trim().toLowerCase(),
      password: v.password,
      telefono: v.telefono?.trim() || undefined,
      tipoDocumento: v.tipoDocumento,
      numeroDocumento: v.numeroDocumento.trim()
    }).subscribe({
      next: () => {
        this.procesando.set(false);
        this.messages.add({
          severity: 'success',
          summary: 'Staff registrado',
          detail: 'Ya puede iniciar sesión con sus credenciales.',
          life: 4500
        });
        this.modalCrear.set(false);
        this.cargar();
      },
      error: () => this.procesando.set(false)
    });
  }

  precargarPasswordConDoc() {
    const doc = this.formCrear.controls.numeroDocumento.value;
    if (doc && !this.formCrear.controls.password.value) {
      this.formCrear.controls.password.setValue(doc);
    }
  }

  // ---- Asignar a evento ----
  abrirAsignar(staffId: number, staffNombre: string) {
    if (this.opcionesEventos().length === 0) {
      this.messages.add({
        severity: 'warn',
        summary: 'Sin eventos activos',
        detail: 'Necesitas un evento ACTIVO para asignar staff.'
      });
      return;
    }
    const yaAsignados = this.staffs().find((s) => s.usuario.id === staffId)?.asignaciones || [];
    const idsYaAsignados = new Set(yaAsignados.map((a) => a.eventoId));
    const disponible = this.opcionesEventos().find((o) => !idsYaAsignados.has(o.value));
    this.formAsignar.reset({
      eventoId: disponible?.value ?? this.opcionesEventos()[0]?.value ?? null,
      funcion: 'GENERAL'
    });
    this.modalAsignar.set({ staffId, staffNombre });
  }
  cerrarAsignar() {
    this.modalAsignar.set(null);
  }
  enviarAsignar() {
    const dato = this.modalAsignar();
    const orgId = this.store.usuario()?.id;
    if (!dato || !orgId) return;
    if (this.formAsignar.invalid) {
      this.formAsignar.markAllAsTouched();
      return;
    }
    const { eventoId, funcion } = this.formAsignar.getRawValue();
    if (!eventoId) return;
    this.procesando.set(true);
    this.inscripcionApi.asignarStaff(eventoId, orgId, dato.staffId, funcion).subscribe({
      next: () => {
        // Actualización optimista en lugar de recargar todo
        const ev = this.eventos().find((e) => e.id === eventoId);
        const nombreEv = ev?.nombre ?? 'Evento';
        this.staffs.update((items) =>
          items.map((it) =>
            it.usuario.id === dato.staffId
              ? {
                  ...it,
                  asignaciones: [
                    ...it.asignaciones.filter((a) => a.eventoId !== eventoId),
                    { eventoId, eventoNombre: nombreEv, funcion }
                  ]
                }
              : it
          )
        );
        this.procesando.set(false);
        this.messages.add({
          severity: 'success',
          summary: 'Asignación creada',
          detail: `${dato.staffNombre} fue asignado al evento.`,
          life: 4000
        });
        this.modalAsignar.set(null);
      },
      error: () => this.procesando.set(false)
    });
  }

  // ---- Activar / Desactivar ----
  toggleEstado(s: StaffConAsignacion) {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    const accion = s.usuario.estado === 'ACTIVO' ? 'desactivar' : 'reactivar';
    this.confirm.confirm({
      header: accion === 'desactivar' ? 'Desactivar staff' : 'Reactivar staff',
      message: accion === 'desactivar'
        ? `¿Estás seguro de desactivar a ${s.usuario.nombre}? No podrá iniciar sesión hasta que lo reactives.`
        : `¿Reactivar la cuenta de ${s.usuario.nombre}?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: accion === 'desactivar' ? 'Desactivar' : 'Reactivar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: accion === 'desactivar'
        ? 'p-button-danger'
        : '!bg-brand-600 !border-brand-600',
      accept: () => {
        const obs = accion === 'desactivar'
          ? this.usuarioApi.desactivarStaff(orgId, s.usuario.id)
          : this.usuarioApi.reactivarStaff(orgId, s.usuario.id);
        obs.subscribe({
          next: (actualizado) => {
            this.staffs.update((items) =>
              this.ordenar(
                items.map((it) =>
                  it.usuario.id === s.usuario.id ? { ...it, usuario: actualizado } : it
                )
              )
            );
            this.messages.add({
              severity: 'success',
              summary: accion === 'desactivar' ? 'Staff desactivado' : 'Staff reactivado',
              detail: s.usuario.nombre
            });
          }
        });
      }
    });
  }

  desasignar(staffId: number, eventoId: number) {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.confirm.confirm({
      header: 'Quitar asignación',
      message: '¿Quitar a este staff del evento?',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Quitar',
      rejectLabel: 'Cancelar',
      accept: () => {
        this.inscripcionApi.desasignarStaff(eventoId, staffId, orgId).subscribe({
          next: () => {
            this.staffs.update((items) =>
              items.map((it) =>
                it.usuario.id === staffId
                  ? { ...it, asignaciones: it.asignaciones.filter((a) => a.eventoId !== eventoId) }
                  : it
              )
            );
            this.messages.add({ severity: 'success', summary: 'Asignación eliminada' });
          }
        });
      }
    });
  }

  funcionLabel(f: FuncionStaff): string {
    return this.funciones.find((x) => x.value === f)?.label ?? f;
  }
}
