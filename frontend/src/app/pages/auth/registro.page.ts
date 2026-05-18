import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { LogoComponent } from '../../shared/logo/logo.component';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';

/** Solo letras Unicode y espacios (nombres compuestos). */
const PAT_NOMBRE = /^[\p{L}\s]+$/u;

function soloLetrasNombre(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = String(control.value ?? '').trim();
    if (!v) return null;
    return PAT_NOMBRE.test(v) ? null : { soloLetras: true };
  };
}

/** Solo dígitos, longitud entre min y max (inclusive). */
function soloDigitosEntre(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = String(control.value ?? '').trim();
    if (!v) return null;
    if (!/^\d+$/.test(v)) return { soloNumeros: true };
    if (v.length < min) return { digitosMin: { min, actual: v.length } };
    if (v.length > max) return { digitosMax: { max, actual: v.length } };
    return null;
  };
}

/**
 * Más de 8 caracteres (mínimo 9), al menos una mayúscula, una minúscula y un carácter especial
 * (no letra ni dígito ni espacio).
 */
function contrasenaFuerte(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = (control.value as string) ?? '';
    if (!v) return null;
    const err: ValidationErrors = {};
    if (v.length < 9) err['passwordCorta'] = { min: 9, actual: v.length };
    if (!/[\p{Lu}]/u.test(v)) err['passwordMayus'] = true;
    if (!/[\p{Ll}]/u.test(v)) err['passwordMinus'] = true;
    if (!/[^\p{L}\p{N}\s]/u.test(v)) err['passwordEspecial'] = true;
    return Object.keys(err).length ? err : null;
  };
}

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
    SelectModule,
    LogoComponent
  ],
  templateUrl: './registro.page.html',
  styleUrl: './registro.page.scss'
})
export class RegistroPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(false);
  mostrarPassword = false;
  mostrarConfirmar = false;
  readonly seccionActiva = signal<string>('identidad');

  readonly secciones: { id: string; icon: string; label: string }[] = [
    { id: 'identidad', icon: 'pi pi-user',     label: 'Nombres Completos' },
    { id: 'contacto',  icon: 'pi pi-envelope',  label: 'Correo Electrónico' },
    { id: 'telefono',  icon: 'pi pi-phone',     label: 'Número de Celular' },
    { id: 'documento', icon: 'pi pi-id-card',   label: 'Documentos' },
    { id: 'seguridad', icon: 'pi pi-shield',    label: 'Seguridad' },
    { id: 'rol',       icon: 'pi pi-users',     label: 'Rol de Usuario' }
  ];

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
    nombre: ['', [Validators.required, Validators.minLength(3), soloLetrasNombre()]],
    email: ['', [Validators.required, Validators.email]],
    telefono: ['', [Validators.required, soloDigitosEntre(10, 10)]],
    tipoDocumento: ['CC', Validators.required],
    numeroDocumento: ['', [Validators.required, soloDigitosEntre(4, 10)]],
    password: ['', [Validators.required, contrasenaFuerte()]],
    confirmar: ['', [Validators.required]]
  });

  contrasenasNoCoinciden(): boolean {
    const v = this.formulario.getRawValue();
    return v.password !== v.confirmar;
  }

  seccionCompleta(id: string): boolean {
    const f = this.formulario.controls;
    switch (id) {
      case 'identidad': return f.nombre.valid;
      case 'contacto':  return f.email.valid;
      case 'telefono':  return f.telefono.valid;
      case 'documento': return f.tipoDocumento.valid && f.numeroDocumento.valid;
      case 'seguridad': return f.password.valid && f.confirmar.valid && !this.contrasenasNoCoinciden();
      case 'rol':       return f.tipo.valid;
      default:          return false;
    }
  }

  focusSeccion(seccion: string) {
    this.seccionActiva.set(seccion);
  }

  /** Devuelve la lista concreta de problemas del formulario, en lenguaje claro. */
  private obtenerProblemas(): string[] {
    const f = this.formulario.controls;
    const problemas: string[] = [];
    if (f.tipo.invalid)            problemas.push('Tipo de cuenta');
    if (f.nombre.errors?.['required']) problemas.push('Nombre completo');
    else if (f.nombre.errors?.['minlength']) problemas.push('Nombre: mínimo 3 caracteres');
    else if (f.nombre.errors?.['soloLetras']) problemas.push('Nombre: solo letras y espacios');
    if (f.email.errors?.['required']) problemas.push('Correo');
    else if (f.email.errors?.['email']) problemas.push('Correo no es válido');
    if (f.telefono.errors?.['required']) problemas.push('Celular');
    else if (f.telefono.errors?.['soloNumeros']) problemas.push('Celular: solo números');
    else if (f.telefono.errors?.['digitosMin'] || f.telefono.errors?.['digitosMax']) {
      problemas.push('Celular: debe tener 10 dígitos');
    }
    if (f.tipoDocumento.invalid)   problemas.push('Tipo de documento');
    if (f.numeroDocumento.errors?.['required']) problemas.push('Número de documento');
    else if (f.numeroDocumento.errors?.['soloNumeros']) problemas.push('Documento: solo números');
    else if (f.numeroDocumento.errors?.['digitosMin']) problemas.push('Documento: mínimo 4 dígitos');
    else if (f.numeroDocumento.errors?.['digitosMax']) problemas.push('Documento: máximo 10 dígitos');
    if (f.password.errors?.['required']) problemas.push('Contraseña');
    else if (f.password.errors?.['passwordCorta']) problemas.push('Contraseña: más de 8 caracteres (mínimo 9)');
    else if (f.password.errors?.['passwordMayus']) problemas.push('Contraseña: incluye una mayúscula');
    else if (f.password.errors?.['passwordMinus']) problemas.push('Contraseña: incluye una minúscula');
    else if (f.password.errors?.['passwordEspecial']) problemas.push('Contraseña: incluye un carácter especial');
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
