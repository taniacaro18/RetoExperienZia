import { Component, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { CardModule } from 'primeng/card';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { AuthService } from '../../core/auth/auth.service';
import { Rol } from '../../core/models/domain.models';
import { LogoComponent } from '../../shared/logo/logo.component';
import { RecuperarModal } from './recuperar.page';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    CardModule,
    ProgressSpinnerModule,
    RecuperarModal,
    LogoComponent
  ],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss'
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(false);
  mostrarPassword = false;
  readonly mostrarRecuperar = signal(false);
  /** Popover de ayuda junto al logo (contraseña = documento si te registró un organizador). */
  readonly ayudaOrganizadorVisible = signal(false);

  readonly formulario = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });

  enviar() {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.messages.add({
        severity: 'warn',
        summary: 'Datos incompletos',
        detail: 'Ingresa tu correo y tu contraseña.',
        life: 4000
      });
      return;
    }
    this.cargando.set(true);
    this.auth.login(this.formulario.getRawValue()).subscribe({
      next: (u) => {
        this.cargando.set(false);
        this.messages.add({
          severity: 'success',
          summary: 'Bienvenido',
          detail: `Hola ${u.nombre}`,
          life: 2500
        });
        this.router.navigate([this.rutaPorRol(u.rol)]);
      },
      error: (err) => {
        this.cargando.set(false);
        const detalle = err?.error?.message
          || err?.error?.error
          || 'No fue posible iniciar sesión. Verifica tus credenciales.';
        this.messages.add({
          severity: 'error',
          summary: 'Inicio de sesión',
          detail: detalle,
          life: 5500
        });
      }
    });
  }

  toggleAyudaOrganizador(event: Event) {
    event.stopPropagation();
    this.ayudaOrganizadorVisible.update((v) => !v);
  }

  cerrarAyudaOrganizador() {
    this.ayudaOrganizadorVisible.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: MouseEvent) {
    if (!this.ayudaOrganizadorVisible()) return;
    const t = ev.target as HTMLElement | null;
    if (t?.closest('.login-brand-row__hint-wrap')) return;
    this.ayudaOrganizadorVisible.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscapeCerrarAyuda() {
    this.ayudaOrganizadorVisible.set(false);
  }

  private rutaPorRol(rol: Rol): string {
    switch (rol) {
      case 'ADMIN': return '/admin/usuarios';
      case 'ORGANIZADOR': return '/organizador/eventos';
      case 'STAFF': return '/staff/eventos';
      case 'ASISTENTE':
      default: return '/eventos';
    }
  }
}
