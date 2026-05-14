package com.experienzia.service;

import com.experienzia.exceptions.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final String UPLOAD_DIR = "uploads/comprobantes/";

    public String guardarComprobante(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new CustomException("El archivo está vacío.", HttpStatus.BAD_REQUEST);
        }
        String contentType = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
        boolean tipoOk =
                contentType.equals("image/jpeg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/webp")
                        || contentType.equals("application/pdf");
        boolean nombreOk =
                nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")
                        || nombre.endsWith(".png") || nombre.endsWith(".webp")
                        || nombre.endsWith(".pdf");
        if (!tipoOk && !nombreOk) {
            throw new CustomException(
                    "Solo se permiten comprobantes en formato JPG, PNG, WEBP o PDF.",
                    HttpStatus.BAD_REQUEST);
        }
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalName = archivo.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String uniqueName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(uniqueName);
            Files.copy(archivo.getInputStream(), filePath);
            // Devolvemos siempre la ruta pública (URL relativa) con la que el
            // frontend podrá pedirla a /uploads/comprobantes/xxx desde el servidor.
            return "/uploads/comprobantes/" + uniqueName;
        } catch (IOException e) {
            throw new CustomException("Error al guardar el archivo comprobante.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Elimina un comprobante previamente guardado (ruta relativa tipo {@code /uploads/comprobantes/...}). */
    public void borrarComprobantePublico(String urlRelativa) {
        if (urlRelativa == null || urlRelativa.isBlank()) {
            return;
        }
        try {
            String rel = urlRelativa.startsWith("/") ? urlRelativa.substring(1) : urlRelativa;
            if (!rel.startsWith(UPLOAD_DIR)) {
                return;
            }
            Path filePath = Paths.get(rel);
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // No bloquear el flujo si el archivo ya no existe.
        }
    }
}
