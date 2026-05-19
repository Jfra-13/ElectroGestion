package com.jfra_13.grupos_electrogenos.security;

import com.jfra_13.grupos_electrogenos.model.entity.Role;
import com.jfra_13.grupos_electrogenos.model.entity.Usuario;
import com.jfra_13.grupos_electrogenos.repository.RoleRepository;
import com.jfra_13.grupos_electrogenos.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile({"dev", "test"})
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Crear roles si no existen
        Role adminRole = roleRepository.findByNombre("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().nombre("ROLE_ADMIN").build()));
        
        roleRepository.findByNombre("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().nombre("ROLE_USER").build()));

        // Crear usuario admin inicial si no existe
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario admin = Usuario.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@electrogenos.com")
                    .roles(Set.of(adminRole))
                    .build();
            usuarioRepository.save(admin);
        }
    }
}
