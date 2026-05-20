package com.experienzia.util;

import com.experienzia.entity.Evento;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Utilidades para calcular inicio/fin de eventos y detectar solapamiento de horarios. */
public final class EventoVentanaUtil {

    private EventoVentanaUtil() {
    }

    /** Convierte el string de zona horaria a ZoneId (por defecto America/Bogota). */
    public static ZoneId zoneId(String zonaConfig) {
        String z = zonaConfig != null ? zonaConfig.trim() : "America/Bogota";
        try {
            return ZoneId.of(z);
        } catch (Exception ex) {
            return ZoneId.systemDefault();
        }
    }

    /** Fin de la ventana: {@code fechaFin} si es válida; si no, inicio + duración (mín. 1 h). */
    public static LocalDateTime instanteFin(Evento e) {
        LocalDateTime inicio = e.getFecha();
        LocalDateTime fin = e.getFechaFin();
        if (fin != null && !fin.isBefore(inicio)) {
            return fin;
        }
        int horas = e.getDuracionHoras() != null && e.getDuracionHoras() > 0 ? e.getDuracionHoras() : 1;
        return inicio.plusHours(horas);
    }

    /** Convierte el fin del evento a ZonedDateTime en la zona indicada. */
    public static ZonedDateTime instanteFinZoned(Evento e, ZoneId zone) {
        return instanteFin(e).atZone(zone);
    }

    /** Convierte el inicio del evento a ZonedDateTime. */
    public static ZonedDateTime instanteInicioZoned(Evento e, ZoneId zone) {
        return e.getFecha().atZone(zone);
    }

    /** Solapamiento estricto de intervalos [inicio, fin). */
    public static boolean ventanasSeSolapan(LocalDateTime inicio1, LocalDateTime fin1, Evento otro) {
        LocalDateTime inicio2 = otro.getFecha();
        LocalDateTime fin2 = instanteFin(otro);
        return inicio1.isBefore(fin2) && inicio2.isBefore(fin1);
    }

    /** true si el evento ya termino segun la hora actual en esa zona. */
    public static boolean eventoYaFinalizo(Evento e, ZoneId zone) {
        return !instanteFinZoned(e, zone).isAfter(ZonedDateTime.now(zone));
    }
}
