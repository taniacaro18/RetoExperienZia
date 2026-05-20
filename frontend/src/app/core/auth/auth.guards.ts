/**
 * Guardias de rutas: comprueban si hay sesión y si el rol puede entrar.
 */
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/domain.models';
import { AuthStore } from './auth.store';

// Solo deja pasar si el usuario ya inició sesión.
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  if (auth.autenticado()) return true;
  router.navigate(['/login']);
  return false;
};

// Solo deja pasar si el rol del usuario está en la lista permitida.
export const rolGuard = (rolesPermitidos: Rol[]): CanActivateFn => {
  return () => {
    const auth = inject(AuthStore);
    const router = inject(Router);
    const rol = auth.rol();
    if (rol && rolesPermitidos.includes(rol)) return true;
    router.navigate(['/login']);
    return false;
  };
};

// Para login/registro: redirige al inicio si ya hay sesión.
export const noAuthGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  if (!auth.autenticado()) return true;
  router.navigate(['/']);
  return false;
};
