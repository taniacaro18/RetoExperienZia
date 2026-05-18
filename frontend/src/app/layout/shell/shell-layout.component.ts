import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { MenuModule } from 'primeng/menu';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MenuItem } from 'primeng/api';
import { LogoComponent } from '../../shared/logo/logo.component';
import { AuthStore } from '../../core/auth/auth.store';
import { AuthService } from '../../core/auth/auth.service';
import { NotificacionStore } from '../../core/state/notificacion.store';
import { Rol } from '../../core/models/domain.models';

interface ItemNavegacion {
  etiqueta: string;
  icono: string;
  ruta: string;
  roles: Rol[];
}

@Component({
  selector: 'app-shell-layout',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ButtonModule,
    AvatarModule,
    BadgeModule,
    MenuModule,
    ToastModule,
    ConfirmDialogModule,
    LogoComponent
  ],
  templateUrl: './shell-layout.component.html',
  styleUrl: './shell-layout.component.scss'
})
export class ShellLayoutComponent implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly store = inject(AuthStore);
  readonly notif = inject(NotificacionStore);

  private mqEscritorio?: MediaQueryList;
  private readonly mqListener = () =>
    this.pantallaGrande.set(this.mqEscritorio?.matches ?? false);

  /** Desde 1024px el sidebar va en el flujo (estático); en pantallas pequeñas es drawer fijo. */
  readonly pantallaGrande = signal(
    typeof window !== 'undefined' && window.innerWidth >= 1024
  );

  readonly sidebarAbierto = signal(false);
  textoBusqueda = '';
  readonly anio = new Date().getFullYear();

  ngOnInit() {
    this.notif.iniciarPolling();
    this.mqEscritorio = window.matchMedia('(min-width: 1024px)');
    this.pantallaGrande.set(this.mqEscritorio.matches);
    this.mqEscritorio.addEventListener('change', this.mqListener);
  }

  ngOnDestroy() {
    this.mqEscritorio?.removeEventListener('change', this.mqListener);
  }

  toggleSidebar() {
    this.sidebarAbierto.update((v) => !v);
  }
  cerrarSidebarMobile() {
    if (window.innerWidth < 1024) this.sidebarAbierto.set(false);
  }

  buscar() {
    const q = this.textoBusqueda.trim();
    if (!q) return;
    const rol = this.store.rol();
    let destino = '/eventos';
    if (rol === 'ORGANIZADOR') destino = '/organizador/eventos';
    else if (rol === 'ADMIN') destino = '/admin/eventos';
    else if (rol === 'STAFF') destino = '/staff/eventos';
    this.router.navigate([destino], { queryParams: { q } });
  }

  /** Si el rol no tiene búsqueda asociada, mejor ocultarla. */
  readonly mostrarBuscador = computed(() => {
    const rol = this.store.rol();
    return rol === 'ASISTENTE' || rol === 'ADMIN' || rol === 'ORGANIZADOR';
  });

  cerrarSesion() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  readonly items: ItemNavegacion[] = [
    { etiqueta: 'Inicio', icono: 'pi-home', ruta: '/inicio', roles: ['ADMIN', 'ORGANIZADOR', 'ASISTENTE', 'STAFF'] },
    { etiqueta: 'Mi perfil', icono: 'pi-user', ruta: '/perfil', roles: ['ADMIN', 'ORGANIZADOR', 'ASISTENTE', 'STAFF'] },
    { etiqueta: 'Notificaciones', icono: 'pi-bell', ruta: '/notificaciones', roles: ['ADMIN', 'ORGANIZADOR', 'ASISTENTE', 'STAFF'] },

    // ASISTENTE: solo catálogo, mis inscripciones, mis certificados
    { etiqueta: 'Explorar eventos', icono: 'pi-compass', ruta: '/eventos', roles: ['ASISTENTE'] },
    { etiqueta: 'Mis inscripciones', icono: 'pi-list', ruta: '/mis-inscripciones', roles: ['ASISTENTE'] },
    { etiqueta: 'Mis certificados', icono: 'pi-verified', ruta: '/mis-certificados', roles: ['ASISTENTE'] },

    // ORGANIZADOR: dashboard + gestión + pagos + reportes (sin "Explorar eventos" ni "Mis inscripciones")
    { etiqueta: 'Dashboard', icono: 'pi-chart-bar', ruta: '/organizador/dashboard', roles: ['ORGANIZADOR'] },
    { etiqueta: 'Gestión eventos', icono: 'pi-calendar', ruta: '/organizador/eventos', roles: ['ORGANIZADOR'] },
    { etiqueta: 'Gestión asistentes', icono: 'pi-users', ruta: '/organizador/asistentes', roles: ['ORGANIZADOR'] },
    { etiqueta: 'Gestión staff', icono: 'pi-id-card', ruta: '/organizador/staff', roles: ['ORGANIZADOR'] },
    { etiqueta: 'Pagos', icono: 'pi-wallet', ruta: '/organizador/pagos', roles: ['ORGANIZADOR'] },
    { etiqueta: 'Reportes', icono: 'pi-chart-line', ruta: '/organizador/reportes', roles: ['ORGANIZADOR'] },
    { etiqueta: 'Certificados', icono: 'pi-verified', ruta: '/organizador/certificados', roles: ['ORGANIZADOR'] },

    // STAFF: solo lo necesario para validar asistencia
    { etiqueta: 'Mi panel', icono: 'pi-th-large', ruta: '/staff/dashboard', roles: ['STAFF'] },
    { etiqueta: 'Eventos asignados', icono: 'pi-id-card', ruta: '/staff/eventos', roles: ['STAFF'] },
    { etiqueta: 'Validar QR', icono: 'pi-qrcode', ruta: '/staff/qr', roles: ['STAFF'] },

    // ADMIN: todo
    { etiqueta: 'Mi panel', icono: 'pi-th-large', ruta: '/admin/dashboard', roles: ['ADMIN'] },
    { etiqueta: 'Eventos', icono: 'pi-calendar', ruta: '/admin/eventos', roles: ['ADMIN'] },
    { etiqueta: 'Usuarios', icono: 'pi-users', ruta: '/admin/usuarios', roles: ['ADMIN'] },
    { etiqueta: 'Pagos', icono: 'pi-wallet', ruta: '/admin/pagos', roles: ['ADMIN'] },
    { etiqueta: 'Reportes', icono: 'pi-chart-line', ruta: '/admin/reportes', roles: ['ADMIN'] },
    { etiqueta: 'Auditoría', icono: 'pi-shield', ruta: '/admin/auditoria', roles: ['ADMIN'] }
  ];

  readonly itemsVisibles = computed(() => {
    const rol = this.store.rol();
    if (!rol) return [];
    return this.items.filter((i) => i.roles.includes(rol));
  });

  readonly menuPerfil: MenuItem[] = [
    { label: 'Mi perfil', icon: 'pi pi-user', routerLink: '/perfil' },
    { separator: true },
    {
      label: 'Cerrar sesión',
      icon: 'pi pi-sign-out',
      command: () => this.cerrarSesion()
    }
  ];

  iniciales(nombre?: string | null): string {
    if (!nombre) return '?';
    const partes = nombre.trim().split(/\s+/);
    const a = partes[0]?.[0] ?? '';
    const b = partes[partes.length - 1]?.[0] ?? '';
    return (a + (partes.length > 1 ? b : '')).toUpperCase();
  }

  etiquetaRol(): string {
    const rol = this.store.rol();
    switch (rol) {
      case 'ADMIN': return 'Administrador';
      case 'ORGANIZADOR': return 'Organizador';
      case 'ASISTENTE': return 'Asistente';
      case 'STAFF': return 'Staff';
      default: return '';
    }
  }
}
