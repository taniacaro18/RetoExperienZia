import { Component, inject, signal, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-recuperar-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './recuperar.page.html',
  styleUrl: './recuperar.page.scss'
})
export class RecuperarModal {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly messages = inject(MessageService);

  readonly cerrarModal = output<void>();

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

  cerrar() {
    this.cerrarModal.emit();
  }
}
