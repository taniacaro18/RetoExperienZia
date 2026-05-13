import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { AuthStore } from '../../core/auth/auth.store';
import { UsuarioApi } from '../../core/api/usuario.api';
import { EstadoUsuario, Rol, Usuario } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import {
  rolLabel,
  rolSeverity,
  usuarioEstadoSeverity
} from '../../shared/estado.helpers';

type FiltroEstado = 'TODOS' | EstadoUsuario;
type FiltroRol = 'TODOS' | Rol;

@Component({
  selector: 'app-admin-usuarios-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    InputTextModule,
    SelectModule,
    DialogModule,
    ConfirmDialogModule,
    StatCardComponent
  ],
  providers: [ConfirmationService],
  templateUrl: './usuarios.page.html'
})
export class AdminUsuariosPage {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(AuthStore);
  private readonly usuarioApi = inject(UsuarioApi);
  private readonly messages = inject(MessageService);
  private readonly confirm = inject(ConfirmationService);

  readonly cargando = signal(true);
  readonly usuarios = signal<Usuario[]>([]);
  readonly busqueda = signal('');
  readonly filtroRol = signal<FiltroRol>('TODOS');
  readonly filtroEstado = signal<FiltroEstado>('TODOS');
  readonly procesando = signal<number | null>(null);

  readonly mostrarModalRol = signal(false);
  readonly usuarioRol = signal<Usuario | null>(null);
  rolNuevo: Rol = 'ASISTENTE';

  readonly opcionesRol: { label: string; value: Rol }[] = [
    { label: 'Administrador', value: 'ADMIN' },
    { label: 'Organizador', value: 'ORGANIZADOR' },
    { label: 'Asistente', value: 'ASISTENTE' },
    { label: 'Staff', value: 'STAFF' }
  ];

  rolLabel = rolLabel;
  rolSeverity = rolSeverity;
  usuarioEstadoSeverity = usuarioEstadoSeverity;

  readonly conteo = computed(() => {
    const items = this.usuarios();
    return {
      total: items.length,
      pendientes: items.filter((u) => u.estado === 'PENDIENTE').length,
      activos: items.filter((u) => u.estado === 'ACTIVO').length,
      inactivos: items.filter((u) => u.estado === 'INACTIVO').length,
      organizadores: items.filter((u) => u.rol === 'ORGANIZADOR').length
    };
  });

  readonly usuariosFiltrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const fr = this.filtroRol();
    const fe = this.filtroEstado();
    let lista = [...this.usuarios()];
    if (fr !== 'TODOS') lista = lista.filter((u) => u.rol === fr);
    if (fe !== 'TODOS') lista = lista.filter((u) => u.estado === fe);
    if (q) {
      lista = lista.filter(
        (u) =>
          u.nombre.toLowerCase().includes(q) ||
          u.email.toLowerCase().includes(q) ||
          (u.numeroDocumento || '').includes(q)
      );
    }
    lista.sort((a, b) => {
      const orden = { PENDIENTE: 0, ACTIVO: 1, INACTIVO: 2, RECHAZADO: 3 } as const;
      const oa = orden[a.estado as keyof typeof orden] ?? 9;
      const ob = orden[b.estado as keyof typeof orden] ?? 9;
      if (oa !== ob) return oa - ob;
      return a.nombre.localeCompare(b.nombre);
    });
    return lista;
  });

  ngOnInit() {
    this.route.queryParamMap.subscribe((qp) => {
      const rol = qp.get('rol') as FiltroRol | null;
      const estado = qp.get('estado') as FiltroEstado | null;
      if (rol) this.filtroRol.set(rol);
      if (estado) this.filtroEstado.set(estado);
    });
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.usuarioApi.listarTodos().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  aprobarOrganizador(u: Usuario) {
    const adminId = this.store.usuario()?.id;
    this.procesando.set(u.id);
    this.usuarioApi.aprobarOrganizador(u.id, adminId).subscribe({
      next: (actualizado) => {
        this.procesando.set(null);
        this.actualizar(actualizado);
        this.messages.add({
          severity: 'success',
          summary: 'Organizador aprobado',
          detail: `${u.nombre} ya puede iniciar sesión.`,
          life: 3500
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  rechazarOrganizador(u: Usuario) {
    this.confirm.confirm({
      header: 'Rechazar organizador',
      message: `¿Confirmas el rechazo de ${u.nombre}? Se le notificará por el sistema.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Rechazar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: '!bg-rose-500 !border-rose-500',
      accept: () => {
        const adminId = this.store.usuario()?.id;
        this.procesando.set(u.id);
        this.usuarioApi.rechazarOrganizador(u.id, adminId).subscribe({
          next: (actualizado) => {
            this.procesando.set(null);
            this.actualizar(actualizado);
            this.messages.add({
              severity: 'success',
              summary: 'Organizador rechazado',
              detail: 'Se notificó al usuario.',
              life: 3500
            });
          },
          error: () => this.procesando.set(null)
        });
      }
    });
  }

  desactivar(u: Usuario) {
    this.confirm.confirm({
      header: 'Desactivar cuenta',
      message: `¿Desactivar a ${u.nombre}? No podrá iniciar sesión hasta ser reactivado.`,
      icon: 'pi pi-ban',
      acceptLabel: 'Desactivar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: '!bg-rose-500 !border-rose-500',
      accept: () => {
        const adminId = this.store.usuario()?.id;
        this.procesando.set(u.id);
        this.usuarioApi.desactivar(u.id, adminId).subscribe({
          next: (a) => {
            this.procesando.set(null);
            this.actualizar(a);
            this.messages.add({
              severity: 'success',
              summary: 'Usuario desactivado',
              life: 3500
            });
          },
          error: () => this.procesando.set(null)
        });
      }
    });
  }

  reactivar(u: Usuario) {
    const adminId = this.store.usuario()?.id;
    this.procesando.set(u.id);
    this.usuarioApi.reactivar(u.id, adminId).subscribe({
      next: (a) => {
        this.procesando.set(null);
        this.actualizar(a);
        this.messages.add({
          severity: 'success',
          summary: 'Usuario reactivado',
          detail: `${u.nombre} ya puede iniciar sesión.`,
          life: 3500
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  abrirCambioRol(u: Usuario) {
    this.usuarioRol.set(u);
    this.rolNuevo = u.rol;
    this.mostrarModalRol.set(true);
  }

  confirmarCambioRol() {
    const u = this.usuarioRol();
    if (!u) return;
    if (this.rolNuevo === u.rol) {
      this.messages.add({
        severity: 'warn',
        summary: 'Sin cambios',
        detail: 'Selecciona un rol distinto al actual.'
      });
      return;
    }
    const adminId = this.store.usuario()?.id;
    this.procesando.set(u.id);
    this.usuarioApi.cambiarRol(u.id, this.rolNuevo, adminId).subscribe({
      next: (actualizado) => {
        this.procesando.set(null);
        this.mostrarModalRol.set(false);
        this.actualizar(actualizado);
        this.messages.add({
          severity: 'success',
          summary: 'Rol actualizado',
          detail: `${u.nombre} ahora es ${rolLabel(this.rolNuevo)}.`,
          life: 3500
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  private actualizar(u: Usuario) {
    this.usuarios.update((items) => items.map((x) => (x.id === u.id ? u : x)));
  }

  iniciales(nombre?: string): string {
    if (!nombre) return '?';
    const partes = nombre.trim().split(/\s+/);
    return ((partes[0]?.[0] ?? '') + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
  }
}
