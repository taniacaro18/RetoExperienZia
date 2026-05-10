import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-registro-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    SelectModule
  ],
  templateUrl: './registro.page.html'
})
export class RegistroPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(false);

  readonly tiposCuenta = [
    { label: 'Asistente', value: 'ASISTENTE' },
    { label: 'Organizador', value: 'ORGANIZADOR' }
  ];

  readonly tiposDocumento = [
    { label: 'Cédula de Ciudadanía', value: 'CC' },
    { label: 'Cédula de Extranjería', value: 'CE' },
    { label: 'Pasaporte', value: 'PA' },
    { label: 'Tarjeta de Identidad', value: 'TI' }
  ];

  readonly formulario = this.fb.nonNullable.group({
    tipo: ['ASISTENTE', Validators.required],
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    telefono: [''],
    tipoDocumento: ['CC', Validators.required],
    numeroDocumento: ['', [Validators.required, Validators.minLength(4)]],
    password: ['', [Validators.required, Validators.minLength(4)]],
    confirmar: ['', [Validators.required]]
  });

  readonly contrasenasNoCoinciden = computed(() => {
    const v = this.formulario.getRawValue();
    return v.password !== v.confirmar;
  });

  /** Devuelve la lista concreta de problemas del formulario, en lenguaje claro. */
  private obtenerProblemas(): string[] {
    const f = this.formulario.controls;
    const problemas: string[] = [];
    if (f.tipo.invalid)            problemas.push('Tipo de cuenta');
    if (f.nombre.errors?.['required']) problemas.push('Nombre completo');
    else if (f.nombre.errors?.['minlength']) problemas.push('Nombre: mínimo 3 caracteres');
    if (f.email.errors?.['required']) problemas.push('Correo');
    else if (f.email.errors?.['email']) problemas.push('Correo no es válido');
    if (f.tipoDocumento.invalid)   problemas.push('Tipo de documento');
    if (f.numeroDocumento.errors?.['required']) problemas.push('Número de documento');
    else if (f.numeroDocumento.errors?.['minlength']) problemas.push('Documento: mínimo 4 dígitos');
    if (f.password.errors?.['required']) problemas.push('Contraseña');
    else if (f.password.errors?.['minlength']) problemas.push('Contraseña: mínimo 4 caracteres');
    if (f.confirmar.errors?.['required']) problemas.push('Confirmación de contraseña');
    return problemas;
  }

  enviar() {
    const problemas = this.obtenerProblemas();
    if (problemas.length > 0) {
      this.formulario.markAllAsTouched();
      this.messages.add({
        severity: 'warn',
        summary: 'Faltan datos',
        detail: problemas.join(' · '),
        life: 6000
      });
      return;
    }
    if (this.contrasenasNoCoinciden()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Contraseñas distintas',
        detail: 'Las contraseñas ingresadas no coinciden.',
        life: 4000
      });
      return;
    }

    this.cargando.set(true);

    const v = this.formulario.getRawValue();
    const payload = {
      tipo: v.tipo,
      nombre: v.nombre.trim(),
      email: v.email.trim().toLowerCase(),
      telefono: v.telefono?.trim() || undefined,
      tipoDocumento: v.tipoDocumento,
      numeroDocumento: v.numeroDocumento.trim(),
      password: v.password
    };

    this.auth.registrar(payload as any).subscribe({
      next: (u) => {
        this.cargando.set(false);
        const esOrg = u.rol === 'ORGANIZADOR';
        this.messages.add({
          severity: 'success',
          summary: 'Te registraste correctamente',
          detail: esOrg
            ? 'Tu solicitud quedó PENDIENTE. Un administrador la revisará pronto.'
            : 'Ya puedes iniciar sesión.',
          life: 5000
        });
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.cargando.set(false);
        // El backend devuelve el mensaje específico (email duplicado, número
        // de documento duplicado, etc.) en err.error.message.
        const detalle = err?.error?.message
          || err?.error?.error
          || 'No se pudo completar el registro. Intenta nuevamente.';
        this.messages.add({
          severity: 'error',
          summary: 'No se pudo registrar',
          detail: detalle,
          life: 6500
        });
      }
    });
  }
}
