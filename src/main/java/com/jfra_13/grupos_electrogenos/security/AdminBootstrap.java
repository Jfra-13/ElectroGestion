package com.jfra_13.grupos_electrogenos.security;

import com.jfra_13.grupos_electrogenos.model.entity.Role;
import com.jfra_13.grupos_electrogenos.model.entity.Usuario;
import com.jfra_13.grupos_electrogenos.repository.RoleRepository;
import com.jfra_13.grupos_electrogenos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Crea el primer administrador en producción a partir de credenciales del entorno.
 *
 * <p>Reemplaza al {@code DataInitializer} (solo dev/test). Los roles ya vienen
 * sembrados por la migración Flyway {@code V2__seed_roles.sql}; aquí solo se crea
 * el usuario admin inicial con la contraseña hasheada en BCrypt.
 *
 * <p>Las credenciales se inyectan desde variables de entorno sin valor por
 * defecto ({@code ADMIN_USERNAME}, {@code ADMIN_PASSWORD}); si faltan, el
 * arranque falla (fail-fast), igual que con los secretos JWT/DB.
 */
@Component
@Profile("prod")
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.password}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.email}")
    private String adminEmail;

    public AdminBootstrap(UsuarioRepository usuarioRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByUsername(adminUsername).isPresent()) {
            log.info("Admin inicial '{}' ya existe; no se recrea.", adminUsername);
            return;
        }

        Role adminRole = roleRepository.findByNombre("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_ADMIN no está sembrado. ¿Corrió la migración V2__seed_roles.sql?"));

        Usuario admin = Usuario.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .email(adminEmail)
                .roles(Set.of(adminRole))
                .build();

        usuarioRepository.save(admin);
        log.info("Admin inicial '{}' creado con ROLE_ADMIN.", adminUsername);
    }
}
