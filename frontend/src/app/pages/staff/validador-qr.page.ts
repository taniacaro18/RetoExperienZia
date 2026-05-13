import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { BrowserMultiFormatReader, IScannerControls } from '@zxing/browser';
import { BarcodeFormat, DecodeHintType } from '@zxing/library';
import { AuthStore } from '../../core/auth/auth.store';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { EventoStaff, FuncionStaff, Inscripcion } from '../../core/models/domain.models';
import { inscripcionEstadoSeverity } from '../../shared/estado.helpers';

type Modo = 'CHECK_IN' | 'CHECK_OUT';

interface RegistroLog {
  id: string;
  fecha: Date;
  modo: Modo;
  exito: boolean;
  codigo: string;
  mensaje: string;
  estado?: string;
  nombreAsistente?: string;
  lineaDocumento?: string;
  nombreEvento?: string;
  detalleEvento?: string;
}

@Component({
  selector: 'app-staff-validador-qr-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    RouterLink,
    InputTextModule,
    SelectModule,
    TagModule
  ],
  templateUrl: './validador-qr.page.html'
})
export class StaffValidadorQrPage implements OnInit, AfterViewInit, OnDestroy {
  private readonly store = inject(AuthStore);
  private readonly api = inject(InscripcionApi);
  private readonly messages = inject(MessageService);

  readonly videoRef = viewChild<ElementRef<HTMLVideoElement>>('video');

  readonly eventos = signal<EventoStaff[]>([]);
  readonly eventoSeleccionado = signal<number | null>(null);
  readonly modo = signal<Modo>('CHECK_IN');
  readonly codigo = signal('');
  readonly procesando = signal(false);
  readonly historial = signal<RegistroLog[]>([]);

  // Cámara / lector QR
  readonly camaras = signal<MediaDeviceInfo[]>([]);
  readonly camaraId = signal<string | null>(null);
  readonly camaraActiva = signal(false);
  readonly camaraError = signal<string | null>(null);
  private reader: BrowserMultiFormatReader | null = null;
  private controls: IScannerControls | null = null;
  private ultimoCodigo: { v: string; t: number } | null = null;

  readonly miFuncion = computed<FuncionStaff | undefined>(() => {
    const id = this.eventoSeleccionado();
    if (!id) return undefined;
    return this.eventos().find((e) => e.eventoId === id)?.funcion;
  });

  readonly puedeCheckIn = computed(() => {
    const f = this.miFuncion();
    return !f || f === 'CHECK_IN_QR' || f === 'CHECK_IN_MANUAL' || f === 'GENERAL';
  });

  readonly puedeCheckOut = computed(() => {
    const f = this.miFuncion();
    return !f || f === 'REGISTRO_SALIDA' || f === 'GENERAL';
  });

  readonly opcionesEventos = computed(() =>
    this.eventos()
      .slice()
      .sort((a, b) => new Date(a.fechaEvento).getTime() - new Date(b.fechaEvento).getTime())
      .map((e) => ({
        label: e.nombreEvento + ' · ' + new Date(e.fechaEvento).toLocaleDateString(),
        value: e.eventoId
      }))
  );

  estadoSeverity = inscripcionEstadoSeverity;

  ngOnInit() {
    const id = this.store.usuario()?.id;
    if (!id) return;
    this.api.eventosDelStaff(id).subscribe({
      next: (lista) => {
        this.eventos.set(lista);
        const ahora = new Date();
        const masCercano = lista
          .filter((e) => new Date(e.fechaEvento) >= ahora && e.estadoEvento === 'ACTIVO')
          .sort((a, b) => new Date(a.fechaEvento).getTime() - new Date(b.fechaEvento).getTime())[0];
        if (masCercano) this.eventoSeleccionado.set(masCercano.eventoId);
        else if (lista.length > 0) this.eventoSeleccionado.set(lista[0].eventoId);
      }
    });
  }

  ngAfterViewInit(): void {
    // El elemento <video> recién existe ahora. Inicializar acá garantiza que
    // viewChild('video') esté disponible cuando el lector de QR la necesita.
    setTimeout(() => void this.inicializarCamara(), 0);
  }

  ngOnDestroy(): void {
    this.detenerCamara();
  }

  /**
   * Pide permisos, enumera las cámaras disponibles y arranca el escaneo.
   * Es el punto de entrada robusto: tanto el `ngAfterViewInit` como el
   * botón "Iniciar" lo invocan.
   */
  async inicializarCamara() {
    this.camaraError.set(null);
    try {
      if (!window.isSecureContext && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1') {
        this.camaraError.set('La cámara solo funciona en HTTPS o en localhost. Abre la app desde http://localhost:4200.');
        return;
      }
      if (!navigator.mediaDevices?.getUserMedia) {
        this.camaraError.set('Tu navegador no soporta acceso a cámara.');
        return;
      }

      // Para que enumerateDevices devuelva labels, primero pedimos permiso.
      // Si no existe trasera, hacemos fallback a cualquier cámara.
      let tmp: MediaStream | null = null;
      try {
        tmp = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: 'environment' } }
        });
      } catch {
        tmp = await navigator.mediaDevices.getUserMedia({ video: true });
      }
      tmp.getTracks().forEach((t) => t.stop());

      const devices = await navigator.mediaDevices.enumerateDevices();
      const videos = devices.filter((d) => d.kind === 'videoinput');
      this.camaras.set(videos);

      if (videos.length === 0) {
        this.camaraError.set('No se encontró ninguna cámara conectada.');
        return;
      }

      // Preferimos cámara trasera (móviles) si está disponible.
      const trasera = videos.find((v) => /back|rear|environment|trasera/i.test(v.label));
      this.camaraId.set((trasera ?? videos[0]).deviceId);
      await this.iniciarEscaneo();
    } catch (e: any) {
      const name = e?.name;
      let msg = e?.message || 'No fue posible acceder a la cámara.';
      if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
        msg = 'Permiso de cámara denegado. Haz clic en el ícono de la barra del navegador y permite la cámara.';
      } else if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
        msg = 'No se detectó ninguna cámara en este dispositivo.';
      } else if (name === 'NotReadableError') {
        msg = 'Otra aplicación está usando la cámara. Ciérrala e intenta nuevamente.';
      }
      this.camaraError.set(msg);
    }
  }

  async cambiarCamara(deviceId: string) {
    this.camaraId.set(deviceId);
    await this.iniciarEscaneo();
  }

  async iniciarEscaneo() {
    this.detenerCamara();
    // Esperamos un tick a que el <video> esté en el DOM si recién montamos.
    let video = this.videoRef()?.nativeElement;
    if (!video) {
      await new Promise((r) => setTimeout(r, 50));
      video = this.videoRef()?.nativeElement;
    }
    if (!video) {
      this.camaraError.set('No se pudo encontrar el visor de cámara en pantalla.');
      return;
    }

    if (!this.reader) {
      const hints = new Map();
      hints.set(DecodeHintType.POSSIBLE_FORMATS, [BarcodeFormat.QR_CODE]);
      hints.set(DecodeHintType.TRY_HARDER, true);
      this.reader = new BrowserMultiFormatReader(hints);
    }

    const deviceId = this.camaraId();
    try {
      // Intento 1: cámara seleccionada por deviceId.
      if (deviceId) {
        this.controls = await this.reader.decodeFromVideoDevice(deviceId, video, (result) => {
          if (result) this.onCodigoEscaneado(result.getText());
        });
      } else {
        // Intento 2: sin deviceId, dejamos que el navegador escoja (móvil → trasera por defecto).
        this.controls = await this.reader.decodeFromConstraints(
          { video: { facingMode: 'environment' } },
          video,
          (result) => {
            if (result) this.onCodigoEscaneado(result.getText());
          }
        );
      }
      this.camaraActiva.set(true);
      this.camaraError.set(null);
    } catch (e: any) {
      // Reintento defensivo: algunos navegadores fallan con deviceId pero funcionan con constraints.
      try {
        this.controls = await this.reader.decodeFromConstraints(
          { video: { facingMode: 'environment' } },
          video,
          (result) => {
            if (result) this.onCodigoEscaneado(result.getText());
          }
        );
        this.camaraActiva.set(true);
        this.camaraError.set(null);
      } catch (e2: any) {
        this.camaraActiva.set(false);
        const name = e2?.name || e?.name;
        const msg = name === 'NotAllowedError'
          ? 'Permiso de cámara denegado por el navegador.'
          : (e2?.message || e?.message || 'No se pudo iniciar la cámara.');
        this.camaraError.set(msg);
      }
    }
  }

  private onCodigoEscaneado(text: string) {
    const ahora = Date.now();
    // Cooldown de 2s para no enviar el mismo QR varias veces seguidas
    if (this.ultimoCodigo && this.ultimoCodigo.v === text && ahora - this.ultimoCodigo.t < 2500) {
      return;
    }
    this.ultimoCodigo = { v: text, t: ahora };
    this.codigo.set(text.trim());

    // Pausamos la cámara mientras se procesa para evitar dobles lecturas y
    // dar feedback visual claro. Se reanuda al terminar la validación.
    if (this.camaraActiva()) {
      this.detenerCamara();
      this.reanudarTrasValidar = true;
    }
    this.validar();
  }

  /** Cuando es true, al terminar `validar()` la cámara vuelve a iniciarse. */
  private reanudarTrasValidar = false;

  detenerCamara() {
    try { this.controls?.stop(); } catch { /* noop */ }
    this.controls = null;
    this.camaraActiva.set(false);
  }

  cambiarModo(m: Modo) {
    this.modo.set(m);
  }

  validar() {
    const cod = this.codigo().trim();
    if (!cod) {
      this.messages.add({
        severity: 'warn',
        summary: 'Código vacío',
        detail: 'Ingresa o escanea un código QR.'
      });
      return;
    }
    const staffId = this.store.usuario()?.id;
    if (!staffId) return;
    const eventoId = this.eventoSeleccionado() ?? undefined;
    const m = this.modo();

    if (m === 'CHECK_IN' && !this.puedeCheckIn()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Acción no autorizada',
        detail: 'Tu función en este evento no permite hacer check-in.'
      });
      return;
    }
    if (m === 'CHECK_OUT' && !this.puedeCheckOut()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Acción no autorizada',
        detail: 'Tu función en este evento no permite registrar salidas.'
      });
      return;
    }

    this.procesando.set(true);
    const llamada = m === 'CHECK_IN'
      ? this.api.checkInQR(cod, staffId, eventoId)
      : this.api.checkOutQR(cod, staffId, eventoId);

    llamada.subscribe({
      next: (ins) => this.onExito(ins, cod, m),
      error: (err) => this.onError(err, cod, m)
    });
  }

  private onExito(ins: Inscripcion, codigo: string, modo: Modo) {
    this.procesando.set(false);
    const msg = modo === 'CHECK_IN' ? 'Check-in registrado' : 'Check-out registrado';
    const nombre = ins.nombreAsistente?.trim() || 'Asistente';
    const doc =
      ins.tipoDocumento && ins.numeroDocumento
        ? `${ins.tipoDocumento} ${ins.numeroDocumento}`.trim()
        : ins.numeroDocumento?.trim() || '';
    const evNombre = ins.nombreEvento?.trim() || '';
    const evFecha = ins.fechaEvento ? new Date(ins.fechaEvento).toLocaleString() : '';
    const evUbic = ins.ubicacionEvento?.trim();
    const partesToast = [
      nombre,
      doc ? `Documento: ${doc}` : null,
      evNombre ? `Evento: ${evNombre}` : null,
      evFecha ? `Fecha: ${evFecha}` : null,
      evUbic ? `Ubicación: ${evUbic}` : null
    ].filter(Boolean) as string[];
    this.messages.add({
      severity: 'success',
      summary: msg,
      detail: partesToast.join(' · '),
      life: 5500
    });
    const detalleEvento = [evFecha ? evFecha : null, evUbic || null].filter(Boolean).join(' · ');
    this.historial.update((h) => [
      {
        id: crypto.randomUUID(),
        fecha: new Date(),
        modo,
        exito: true,
        codigo,
        mensaje: msg,
        estado: ins.estado,
        nombreAsistente: nombre,
        lineaDocumento: doc || undefined,
        nombreEvento: evNombre || undefined,
        detalleEvento: detalleEvento || undefined
      },
      ...h
    ].slice(0, 20));
    this.codigo.set('');
    this.reanudarSiCorresponde(1200);
  }

  private onError(err: any, codigo: string, modo: Modo) {
    this.procesando.set(false);
    const body = err?.error;
    const detalle = (body && typeof body === 'object' && body.message)
      ? String(body.message)
      : (typeof body === 'string' && body.trim().length > 0 ? body : 'No se pudo procesar el código.');
    const esWarn = err?.status >= 400 && err?.status < 500;
    this.messages.add({
      severity: esWarn ? 'warn' : 'error',
      summary: esWarn ? 'Atención' : 'Error',
      detail: detalle,
      life: 5000
    });
    this.historial.update((h) => [
      {
        id: crypto.randomUUID(),
        fecha: new Date(),
        modo,
        exito: false,
        codigo,
        mensaje: detalle
      },
      ...h
    ].slice(0, 20));
    this.codigo.set('');
    this.reanudarSiCorresponde(1500);
  }

  /** Reanuda la cámara después de la validación si fue pausada por el escaneo. */
  private reanudarSiCorresponde(delayMs: number) {
    if (!this.reanudarTrasValidar) return;
    this.reanudarTrasValidar = false;
    setTimeout(() => {
      // Solo reanuda si el usuario no la apagó manualmente y sigue en la pantalla.
      if (!this.camaraActiva() && this.videoRef()) {
        void this.iniciarEscaneo();
      }
    }, delayMs);
  }

  limpiarHistorial() {
    this.historial.set([]);
  }
}
