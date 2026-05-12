import { Routes } from '@angular/router';
import { authGuard, noAuthGuard, rolGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: 'catalogo',
    loadComponent: () =>
      import('./pages/public/catalogo-publico.page').then((m) => m.CatalogoPublicoPage)
  },
  {
    path: 'catalogo/:id',
    loadComponent: () =>
      import('./pages/public/detalle-evento-publico.page').then((m) => m.DetalleEventoPublicoPage)
  },
  // Rutas públicas (auth)
  {
    path: 'login',
    canActivate: [noAuthGuard],
    loadComponent: () => import('./pages/auth/login.page').then((m) => m.LoginPage)
  },
  {
    path: 'registro',
    canActivate: [noAuthGuard],
    loadComponent: () => import('./pages/auth/registro.page').then((m) => m.RegistroPage)
  },

  // App protegida con shell
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/shell/shell-layout.component').then((m) => m.ShellLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./pages/inicio/inicio.page').then((m) => m.InicioPage)
      },
      {
        path: 'perfil',
        loadComponent: () => import('./pages/common/perfil.page').then((m) => m.PerfilPage)
      },
      {
        // Bandeja de notificaciones in-app, disponible para todos los roles.
        path: 'notificaciones',
        loadComponent: () =>
          import('./pages/common/notificaciones.page').then((m) => m.NotificacionesPage)
      },

      // ----- Asistente / Catálogo público -----
      {
        path: 'eventos',
        canActivate: [rolGuard(['ASISTENTE', 'ADMIN'])],
        loadComponent: () =>
          import('./pages/asistente/eventos-catalogo.page').then((m) => m.EventosCatalogoPage)
      },
      {
        // El detalle de evento también lo abren organizadores (para ver su propio
        // evento desde su lista) y staff (para ver eventos asignados). El propio
        // componente decide qué acciones mostrar según el rol y la propiedad.
        path: 'eventos/:id',
        canActivate: [rolGuard(['ASISTENTE', 'ADMIN', 'ORGANIZADOR', 'STAFF'])],
        loadComponent: () =>
          import('./pages/asistente/evento-detalle.page').then((m) => m.EventoDetallePage)
      },
      {
        path: 'mis-inscripciones',
        canActivate: [rolGuard(['ASISTENTE'])],
        loadComponent: () =>
          import('./pages/asistente/mis-inscripciones.page').then((m) => m.MisInscripcionesPage)
      },
      {
        path: 'mis-certificados',
        canActivate: [rolGuard(['ASISTENTE'])],
        loadComponent: () =>
          import('./pages/asistente/mis-certificados.page').then((m) => m.MisCertificadosPage)
      },

      // ----- Organizador -----
      {
        path: 'organizador/dashboard',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/dashboard.page').then((m) => m.OrgDashboardPage)
      },
      {
        path: 'organizador/eventos',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/eventos-lista.page').then((m) => m.OrgEventosListaPage)
      },
      {
        path: 'organizador/eventos/nuevo',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/evento-form.page').then((m) => m.OrgEventoFormPage)
      },
      {
        path: 'organizador/eventos/:id/editar',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/evento-form.page').then((m) => m.OrgEventoFormPage)
      },
      {
        path: 'organizador/asistentes',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/asistentes.page').then((m) => m.OrgAsistentesPage)
      },
      {
        path: 'organizador/staff',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/staff.page').then((m) => m.OrgStaffPage)
      },
      {
        path: 'organizador/pagos',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/pagos.page').then((m) => m.OrgPagosPage)
      },
      {
        path: 'organizador/reportes',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/reportes.page').then((m) => m.OrgReportesPage)
      },
      {
        path: 'organizador/certificados',
        canActivate: [rolGuard(['ORGANIZADOR'])],
        loadComponent: () =>
          import('./pages/organizador/certificados.page').then((m) => m.OrgCertificadosPage)
      },

      // ----- Staff -----
      {
        path: 'staff/dashboard',
        canActivate: [rolGuard(['STAFF'])],
        loadComponent: () =>
          import('./pages/staff/dashboard.page').then((m) => m.StaffDashboardPage)
      },
      {
        path: 'staff/eventos',
        canActivate: [rolGuard(['STAFF'])],
        loadComponent: () =>
          import('./pages/staff/eventos.page').then((m) => m.StaffEventosPage)
      },
      {
        path: 'staff/eventos/:id',
        canActivate: [rolGuard(['STAFF'])],
        loadComponent: () =>
          import('./pages/staff/evento-detalle.page').then((m) => m.StaffEventoDetallePage)
      },
      {
        path: 'staff/qr',
        canActivate: [rolGuard(['STAFF'])],
        loadComponent: () =>
          import('./pages/staff/validador-qr.page').then((m) => m.StaffValidadorQrPage)
      },

      // ----- Admin -----
      {
        path: 'admin/dashboard',
        canActivate: [rolGuard(['ADMIN'])],
        loadComponent: () =>
          import('./pages/admin/dashboard.page').then((m) => m.AdminDashboardPage)
      },
      {
        path: 'admin/eventos',
        canActivate: [rolGuard(['ADMIN'])],
        loadComponent: () =>
          import('./pages/admin/eventos.page').then((m) => m.AdminEventosPage)
      },
      {
        path: 'admin/usuarios',
        canActivate: [rolGuard(['ADMIN'])],
        loadComponent: () =>
          import('./pages/admin/usuarios.page').then((m) => m.AdminUsuariosPage)
      },
      {
        path: 'admin/pagos',
        canActivate: [rolGuard(['ADMIN'])],
        loadComponent: () =>
          import('./pages/admin/pagos.page').then((m) => m.AdminPagosPage)
      },
      {
        path: 'admin/reportes',
        canActivate: [rolGuard(['ADMIN'])],
        loadComponent: () =>
          import('./pages/admin/reportes.page').then((m) => m.AdminReportesPage)
      },
      {
        path: 'admin/auditoria',
        canActivate: [rolGuard(['ADMIN'])],
        loadComponent: () =>
          import('./pages/admin/auditoria.page').then((m) => m.AdminAuditoriaPage)
      }
    ]
  },
  { path: '**', redirectTo: 'catalogo' }
];
