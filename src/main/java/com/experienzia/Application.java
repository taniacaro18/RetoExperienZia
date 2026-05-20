package com.experienzia;

/**
 * Punto de entrada de la aplicación ExperienZia (backend Spring Boot).
 * <p>
 * Cuando ejecutas este proyecto, Spring arranca el servidor en el puerto configurado
 * (por defecto 8080) y expone las APIs REST que consume el frontend Angular.
 * También crea un usuario administrador la primera vez, para poder entrar sin
 * registrar nada manualmente en la base de datos.
 */
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

// Le dice a Spring que esta clase es la aplicación principal.
@SpringBootApplication
// Permite tareas programadas (por ejemplo, cerrar eventos que ya terminaron).
@EnableScheduling
public class Application {

	// Método main: aquí empieza todo cuando corres la app desde el IDE o con Maven.
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * Bean que se ejecuta UNA vez al terminar de arrancar Spring.
	 * Crea el usuario administrador por defecto si no existe en la tabla usuarios,
	 * para que puedas iniciar sesión en el frontend desde el primer día.
	 */
	@Bean
	CommandLineRunner inicializarAdmin(UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder) {
		return args -> {
			// Correo fijo del admin de prueba (cámbialo en producción).
			final String emailAdmin = "admin@experienzia.com";

			// Si no hay nadie con ese correo, lo creamos.
			if (usuarioRepository.findByEmail(emailAdmin).isEmpty()) {
				System.out.println("Creando administrador por defecto: " + emailAdmin);

				Usuario admin = new Usuario();
				admin.setNombre("Administrador ExperienZia");
				admin.setEmail(emailAdmin);
				// La contraseña se guarda encriptada (no en texto plano).
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
