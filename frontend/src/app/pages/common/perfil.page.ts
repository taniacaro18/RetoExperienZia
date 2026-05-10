import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { AvatarModule } from 'primeng/avatar';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-perfil-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    CardModule,
    TagModule,
    SelectModule,
    AvatarModule
  ],
  templateUrl: './perfil.page.html'
})
export class PerfilPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  readonly store = inject(AuthStore);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(false);

  readonly tiposDocumento = [
    { label: 'Cédula de Ciudadanía', value: 'CC' },
    { label: 'Cédula de Extranjería', value: 'CE' },
    { label: 'Pasaporte', value: 'PA' },
    { label: 'Tarjeta de Identidad', value: 'TI' }
  ];

  readonly formulario = this.fb.nonNullable.group({
    nombre: [this.store.usuario()?.nombre ?? '', [Validators.required, Validators.minLength(3)]],
    email: [this.store.usuario()?.email ?? '', [Validators.required, Validators.email]],
    telefono: [this.store.usuario()?.telefono ?? ''],
    tipoDocumento: [this.store.usuario()?.tipoDocumento ?? 'CC'],
    numeroDocumento: [this.store.usuario()?.numeroDocumento ?? ''],
    nuevaPassword: ['']
  });

  guardar() {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const u = this.store.usuario();
    if (!u) return;

    const v = this.formulario.value;
    const payload: any = {};
    if (v.nombre && v.nombre !== u.nombre) payload.nombre = v.nombre;
    if (v.email && v.email !== u.email) payload.email = v.email;
    if (v.telefono !== undefined) payload.telefono = v.telefono;
    if (v.tipoDocumento && v.tipoDocumento !== u.tipoDocumento) payload.tipoDocumento = v.tipoDocumento;
    if (v.numeroDocumento && v.numeroDocumento !== u.numeroDocumento) payload.numeroDocumento = v.numeroDocumento;
    if (v.nuevaPassword && v.nuevaPassword.length > 0) payload.nuevaPassword = v.nuevaPassword;

    if (Object.keys(payload).length === 0) {
      this.messages.add({
        severity: 'info',
        summary: 'Sin cambios',
        detail: 'No modificaste ningún campo.'
      });
      return;
    }

    this.cargando.set(true);
    this.auth.actualizarPerfil(u.id, payload).subscribe({
      next: () => {
        this.cargando.set(false);
        this.formulario.patchValue({ nuevaPassword: '' });
        this.messages.add({
          severity: 'success',
          summary: 'Perfil actualizado',
          detail: 'Tus cambios fueron guardados.'
        });
      },
      error: (err) => {
        this.cargando.set(false);
        const detalle = err?.error?.message
          || 'No fue posible actualizar el perfil. Revisa los datos e intenta de nuevo.';
        this.messages.add({
          severity: 'error',
          summary: 'No se pudo guardar',
          detail: detalle,
          life: 6000
        });
      }
    });
  }

  iniciales(nombre?: string | null): string {
    if (!nombre) return '?';
    const partes = nombre.trim().split(/\s+/);
    const a = partes[0]?.[0] ?? '';
    const b = partes[partes.length - 1]?.[0] ?? '';
    return (a + (partes.length > 1 ? b : '')).toUpperCase();
  }
}
