import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { AuditoriaApi } from '../../core/api/auditoria.api';
import { UsuarioApi } from '../../core/api/usuario.api';
import { Auditoria, Usuario } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { accionAuditoriaIcono } from '../../shared/estado.helpers';

@Component({
  selector: 'app-admin-auditoria-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    InputTextModule,
    SelectModule,
    StatCardComponent
  ],
  templateUrl: './auditoria.page.html'
})
export class AdminAuditoriaPage {
  private readonly auditoriaApi = inject(AuditoriaApi);
  private readonly usuarioApi = inject(UsuarioApi);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(true);
  readonly registros = signal<Auditoria[]>([]);
  readonly usuarios = signal<Map<number, Usuario>>(new Map());
  readonly busqueda = signal('');
  readonly filtroEntidad = signal<string>('TODAS');

  iconoAccion = accionAuditoriaIcono;

  readonly entidades = computed(() => {
    const set = new Set<string>();
    for (const r of this.registros()) set.add(r.entidad);
    return Array.from(set).sort();
  });

  readonly conteo = computed(() => {
    const items = this.registros();
    const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
    const desde7 = new Date(); desde7.setDate(desde7.getDate() - 7);
    return {
      total: items.length,
      hoy: items.filter((r) => new Date(r.fecha) >= hoy).length,
      semana: items.filter((r) => new Date(r.fecha) >= desde7).length,
      entidades: this.entidades().length
    };
  });

  readonly registrosFiltrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const fe = this.filtroEntidad();
    let lista = [...this.registros()];
    if (fe !== 'TODAS') lista = lista.filter((r) => r.entidad === fe);
    if (q) {
      lista = lista.filter(
        (r) =>
          r.accion.toLowerCase().includes(q) ||
          r.entidad.toLowerCase().includes(q) ||
          (r.direccionIp && r.direccionIp.toLowerCase().includes(q)) ||
          (r.usuarioId != null && this.nombreUsuario(r.usuarioId).toLowerCase().includes(q)) ||
          (r.entidadId != null && String(r.entidadId).includes(q))
      );
    }
    lista.sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime());
    return lista;
  });

  ngOnInit() {
    this.cargando.set(true);
    this.auditoriaApi.listarTodo().subscribe({
      next: (lista) => {
        this.registros.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
    this.usuarioApi.listarTodos().subscribe({
      next: (lista) => {
        const map = new Map<number, Usuario>();
        for (const u of lista) map.set(u.id, u);
        this.usuarios.set(map);
      }
    });
  }

  nombreUsuario(id?: number | null): string {
    if (id == null) return 'Sistema';
    return this.usuarios().get(id)?.nombre ?? '#' + id;
  }

  rolUsuario(id?: number | null): string | null {
    if (id == null) return null;
    return this.usuarios().get(id)?.rol ?? null;
  }

  exportarCsv() {
    const items = this.registrosFiltrados();
    const filas: string[][] = [
      ['ID', 'Fecha', 'Usuario', 'Acción', 'Entidad', 'Entidad ID', 'IP'],
      ...items.map((r) => [
        String(r.id),
        new Date(r.fecha).toISOString(),
        this.nombreUsuario(r.usuarioId),
        r.accion,
        r.entidad,
        r.entidadId != null ? String(r.entidadId) : '',
        r.direccionIp ?? ''
      ])
    ];
    const csv = filas
      .map((f) => f.map((c) => '"' + c.replace(/"/g, '""') + '"').join(','))
      .join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'auditoria.csv';
    a.click();
    URL.revokeObjectURL(a.href);
    this.messages.add({
      severity: 'success',
      summary: 'CSV descargado',
      detail: items.length + ' registros',
      life: 2500
    });
  }
}
