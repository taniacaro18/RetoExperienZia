import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn
} from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { AvatarModule } from 'primeng/avatar';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';
import { AuthStore } from '../../core/auth/auth.store';
import { ActualizarPerfil } from '../../core/models/domain.models';

function telefonoPerfilValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = (control.value ?? '').toString().trim();
    if (!raw) return null;
    if (raw.length < 7 || raw.length > 20) return { telefonoLongitud: true };
    if (!/^[0-9+\s().-]+$/.test(raw)) return { telefonoFormato: true };
    return null;
  };
}

function nuevaPasswordOpcionalValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = (control.value ?? '').toString();
    if (!raw.trim()) return null;
    if (raw.length < 4) return { minlength: { requiredLength: 4, actualLength: raw.length } };
    return null;
  };
}

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

  readonly formulario = this.fb.nonNullable.group({
    telefono: [this.store.usuario()?.telefono ?? '', [telefonoPerfilValidator()]],
    nuevaPassword: ['', [nuevaPasswordOpcionalValidator()]]
  });

  guardar() {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const u = this.store.usuario();
    if (!u) return;

    const v = this.formulario.getRawValue();
    const telefonoAntes = (u.telefono ?? '').trim();
    const telefonoDespues = (v.telefono ?? '').trim();
    const payload: ActualizarPerfil = {};
    if (telefonoDespues !== telefonoAntes) {
      payload.telefono = telefonoDespues;
    }
    const pass = v.nuevaPassword?.trim() ?? '';
    if (pass.length > 0) {
      payload.nuevaPassword = pass;
    }

    if (Object.keys(payload).length === 0) {
      this.messages.add({
        severity: 'info',
        summary: 'Sin cambios',
        detail: 'No modificaste el teléfono ni la contraseña.'
      });
      return;
    }

    this.cargando.set(true);
    this.auth.actualizarPerfil(u.id, payload).subscribe({
      next: () => {
        this.cargando.set(false);
        this.formulario.patchValue({ nuevaPassword: '' });
        this.formulario.controls.nuevaPassword.markAsUntouched();
        const t = this.store.usuario()?.telefono ?? '';
        this.formulario.patchValue({ telefono: t });
        this.messages.add({
          severity: 'success',
          summary: 'Perfil actualizado',
          detail: 'Tus cambios fueron guardados.'
        });
      },
      error: (err) => {
        this.cargando.set(false);
        const detalle =
          err?.error?.message ||
          'No fue posible actualizar el perfil. Revisa los datos e intenta de nuevo.';
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

  etiquetaTipoDocumento(codigo?: string | null): string {
    const map: Record<string, string> = {
      CC: 'Cédula de Ciudadanía',
      CE: 'Cédula de Extranjería',
      PA: 'Pasaporte',
      TI: 'Tarjeta de Identidad'
    };
    if (!codigo) return '—';
    return map[codigo] ?? codigo;
  }
}
