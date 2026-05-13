package com.experienzia;

import com.experienzia.entity.Estado;
import com.experienzia.entity.Rol;
import com.experienzia.entity.Usuario;
import com.experienzia.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * Este Bean se ejecuta una vez que la aplicación ha iniciado.
	 * Crea un usuario administrador por defecto si todavía no existe,
	 * para poder iniciar sesión y administrar la plataforma desde el primer arranque.
	 */
	@Bean
	CommandLineRunner inicializarAdmin(UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder) {
		return args -> {
			final String emailAdmin = "admin@experienzia.com";

			if (usuarioRepository.findByEmail(emailAdmin).isEmpty()) {
				System.out.println("Creando administrador por defecto: " + emailAdmin);

				Usuario admin = new Usuario();
				admin.setNombre("Administrador ExperienZia");
				admin.setEmail(emailAdmin);
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setTipoDocumento("CC");
				admin.setNumeroDocumento("0000000000");
				admin.setTelefono("3000000000");
				admin.setRol(Rol.ADMIN);
				admin.setEstado(Estado.ACTIVO);

				usuarioRepository.save(admin);

				System.out.println("Administrador creado. Credenciales -> "
						+ emailAdmin + " / admin123");
			} else {
				System.out.println("El administrador por defecto (" + emailAdmin + ") ya existe.");
			}
		};
	}
}
