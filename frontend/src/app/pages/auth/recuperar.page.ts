import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CardModule } from 'primeng/card';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-recuperar-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    CardModule
  ],
  templateUrl: './recuperar.page.html'
})
export class RecuperarPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(false);
  readonly passwordTemporal = signal<string | null>(null);

  readonly formulario = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    numeroDocumento: ['', [Validators.required, Validators.minLength(4)]]
  });

  enviar() {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.messages.add({
        severity: 'warn',
        summary: 'Datos incompletos',
        detail: 'Necesitamos tu correo y número de documento para verificar tu cuenta.',
        life: 5000
      });
      return;
    }
    this.cargando.set(true);
    const { email, numeroDocumento } = this.formulario.getRawValue();
    this.auth.recuperar(email, numeroDocumento).subscribe({
      next: (r) => {
        this.cargando.set(false);
        this.passwordTemporal.set(r.passwordTemporal);
        this.messages.add({
          severity: 'success',
          summary: 'Contraseña temporal generada',
          detail: 'Úsala para iniciar sesión y cámbiala en tu perfil.',
          life: 6000
        });
      },
      error: (err) => {
        this.cargando.set(false);
        const detalle = err?.error?.message
          || 'No fue posible recuperar la contraseña. Verifica que el correo y el documento coincidan con tu cuenta.';
        this.messages.add({
          severity: 'error',
          summary: 'Recuperación fallida',
          detail: detalle,
          life: 6500
        });
      }
    });
  }
}
