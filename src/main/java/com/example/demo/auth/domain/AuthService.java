package com.example.demo.auth.domain;


import com.example.demo.Events.password.PasswordChangedEmailEvent;
import com.example.demo.Events.password.PasswordResetEmailEvent;
import com.example.demo.Events.welcome.WelcomeEmailEvent;
import com.example.demo.auth.dto.AuthResponseDto;
import com.example.demo.auth.dto.GoogleUserInfoDto;
import com.example.demo.auth.dto.LoginRequestDto;
import com.example.demo.auth.dto.RegisterRequestDto;
import com.example.demo.auth.infraestructure.PasswordResetTokenRepository;
import com.example.demo.exceptions.UserAlreadyExistException;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.admin.domain.Admin;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.clientHistorial.domain.Historial;
import com.example.demo.clientHistorial.infraestructure.HistorialRepository;
import com.example.demo.config.JwtService;
import com.example.demo.exceptions.PasswordIncorrectException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.Role;
import com.example.demo.user.infraestructure.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class  AuthService {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AdminRepository adminRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final HistorialRepository historialRepository;
    private final GoogleOAuthService googleOAuthService;
    private final PasswordResetTokenRepository tokenRepo;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final SecureRandom RAND = new SecureRandom();

    private String generarTokenCrudo(int bytes) {
        byte[] b = new byte[bytes];
        RAND.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b); // seguro p/URL
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    // ===== 1) Solicitar reset =====
    @Transactional
    public void solicitarResetPassword(String email) {
        // 1) Buscar usuario por email (respuesta neutra para no revelar existencia)
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) return;

        User user = opt.get();
        // 2) Si no tiene password local (probablemente solo OAuth), no generes token
        var passwordActual = user.getPassword();
        if (passwordActual == null || passwordActual.isBlank()) {
            return; // opcional: enviar correo indicando que cambie desde Google
        }

        // 3) Generar token seguro y guardar SOLO el hash (SHA-256)
        String tokenCrudo = generarTokenCrudo(32);           // ~43 chars base64-url
        String tokenHash  = sha256(tokenCrudo);

        PasswordResetToken token = new PasswordResetToken();
        token.setUsuario(user);
        token.setTokenHash(tokenHash);
        token.setExpiraEn(Instant.now().plus(1, ChronoUnit.HOURS));
        tokenRepo.save(token);

        // 4) Enviar email con el token crudo en el enlace
        String enlace = tokenCrudo; // <-- cambia dominio

        applicationEventPublisher.publishEvent(
                new PasswordResetEmailEvent(this, user.getEmail(), enlace)  // tu User tiene getEmail()
        );
    }


    @Transactional
    public void resetPassword(String tokenCrudo, String nuevaPassword) {
        // A) Validar token
        String tokenHash = sha256(tokenCrudo);
        var token = tokenRepo.findByTokenHashAndUsadoFalse(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o ya usado"));

        if (token.getExpiraEn().isBefore(Instant.now())) {
            throw new IllegalStateException("Token expirado");
        }

        // B) Actualizar contraseña del usuario
        var user = token.getUsuario();
        user.setPassword(passwordEncoder.encode(nuevaPassword)); // BCrypt recomendado

        // C) Marcar token como usado (one-shot)
        token.setUsado(true);
        token.setUsadoEn(Instant.now());
        tokenRepo.save(token);
        userRepository.save(user);

        // D) (Opcional) invalidar sesiones/JWT previos aquí si manejas versionado de credenciales

        // E) Notificar cambio
        applicationEventPublisher.publishEvent(
                new PasswordChangedEmailEvent(this, user.getEmail())
        );
    }



    public AuthResponseDto login(LoginRequestDto loginDTO){
        User user = userRepository.findByEmail(loginDTO.getEmail()).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if(!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            throw new PasswordIncorrectException("Incorrect Password");
        }
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(jwtService.generatedToken(user));
        return response;
    }


    @Transactional
    public AuthResponseDto register(RegisterRequestDto registerDto){
        if (userRepository.findByEmail(registerDto.getEmail()).isPresent()) {
            throw new UserAlreadyExistException("Email already registered");
        }
        Client client = new Client();
        client.setFirstName(registerDto.getFirstName());
        client.setLastName(registerDto.getLastName());
        client.setUsername(registerDto.getUsername());
        client.setEmail(registerDto.getEmail());
        client.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        client.setPhoneNumber(registerDto.getPhoneNumber());
        client.setRole(Role.FREE);
        client.setCreatedAt(ZonedDateTime.now());

        User savedClient = clientRepository.save(client);
        Historial historial = new Historial();
        historial.setClient(client);
        historialRepository.save(historial);

        AuthResponseDto response = new AuthResponseDto();
        response.setToken(jwtService.generatedToken(savedClient));
        applicationEventPublisher.publishEvent(new WelcomeEmailEvent(this, savedClient.getUsername(),savedClient.getEmail()));
        return response;
    }

    @Transactional
    public AuthResponseDto createAdmin(RegisterRequestDto registerDto) {
        if (userRepository.findByEmail(registerDto.getEmail()).isPresent()) {
            throw new UserAlreadyExistException("Email already registered");
        }

        String encodedPassword = passwordEncoder.encode(registerDto.getPassword());
        
        Admin admin = new Admin();
        admin.setFirstName(registerDto.getFirstName());
        admin.setLastName(registerDto.getLastName());
        admin.setUsername(registerDto.getUsername());
        admin.setEmail(registerDto.getEmail());
        admin.setPassword(encodedPassword);
        admin.setPhoneNumber(registerDto.getPhoneNumber());
        admin.setRole(Role.ADMIN);
        admin.setCreatedAt(ZonedDateTime.now());
        Admin savedAdmin = adminRepository.save(admin);
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(jwtService.generatedToken(savedAdmin));
        applicationEventPublisher.publishEvent(new WelcomeEmailEvent(this, savedAdmin.getUsername(),savedAdmin.getEmail()));
        return response;
    }

    @Transactional
    public AuthResponseDto googleLogin(String code) {
        // Obtener información del usuario de Google
        GoogleUserInfoDto googleUser = googleOAuthService.getUserInfo(code);
        // Verificar si el usuario ya existe
        Optional<User> existingUser = userRepository.findByEmail(googleUser.getEmail());
        User user;
        if (existingUser.isPresent()) {
            // Usuario existente, usar el que ya está en la BD
            user = existingUser.get();
        } else {
            Client client = new Client();
            String firstName = "Usuario";
            String lastName = "Google";
            if (googleUser.getGiven_name() != null && !googleUser.getGiven_name().trim().isEmpty()) {
                firstName = googleUser.getGiven_name().trim();
            }
            if (googleUser.getFamily_name() != null && !googleUser.getFamily_name().trim().isEmpty()) {
                lastName = googleUser.getFamily_name().trim();
            }
            if ((firstName.equals("Usuario") || lastName.equals("Google")) &&
                    googleUser.getName() != null && !googleUser.getName().trim().isEmpty()) {
                String[] nameParts = googleUser.getName().trim().split("\\s+", 2);
                if (nameParts.length >= 1) {
                    firstName = nameParts[0];
                    if (nameParts.length >= 2) {
                        lastName = nameParts[1];
                    }
                }
            }

            String email = googleUser.getEmail();
            String username = email;
            if (email != null && email.endsWith("@gmail.com")) {
                username = email.substring(0, email.indexOf("@gmail.com"));
            }
            client.setUsername(username);
            client.setFirstName(firstName);
            client.setLastName(lastName);
            client.setEmail(googleUser.getEmail());
            client.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Password random
            client.setPhoneNumber(null); // Permitir NULL para usuarios de Google
            client.setRole(Role.FREE);
            client.setCreatedAt(ZonedDateTime.now());
            user = clientRepository.save(client);
            Historial historial = new Historial();
            historial.setClient((Client) user);
            historialRepository.save(historial);
            applicationEventPublisher.publishEvent(new WelcomeEmailEvent(this, username,email));
        }
        String token = jwtService.generatedToken(user);
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        return response;
    }



    // ADMIN LOGIN USANDO GOOGLE
//    @Transactional
//    public AuthResponseDto googleLogin(String code) {
//        // Obtener información del usuario de Google
//        GoogleUserInfoDto googleUser = googleOAuthService.getUserInfo(code);
//        // Verificar si el usuario ya existe
//        Optional<User> existingUser = userRepository.findByEmail(googleUser.getEmail());
//        User user;
//        if (existingUser.isPresent()) {
//            // Usuario existente, usar el que ya está en la BD
//            user = existingUser.get();
//        } else {
//            Admin admin = new Admin();
//            String firstName = "Usuario";
//            String lastName = "Google";
//            if (googleUser.getGiven_name() != null && !googleUser.getGiven_name().trim().isEmpty()) {
//                firstName = googleUser.getGiven_name().trim();
//            }
//            if (googleUser.getFamily_name() != null && !googleUser.getFamily_name().trim().isEmpty()) {
//                lastName = googleUser.getFamily_name().trim();
//            }
//            if ((firstName.equals("Usuario") || lastName.equals("Google")) &&
//                    googleUser.getName() != null && !googleUser.getName().trim().isEmpty()) {
//                String[] nameParts = googleUser.getName().trim().split("\\s+", 2);
//                if (nameParts.length >= 1) {
//                    firstName = nameParts[0];
//                    if (nameParts.length >= 2) {
//                        lastName = nameParts[1];
//                    }
//                }
//            }
//
//            String email = googleUser.getEmail();
//            String username = email;
//            if (email != null && email.endsWith("@gmail.com")) {
//                username = email.substring(0, email.indexOf("@gmail.com"));
//            }
//            admin.setUsername(username);
//            admin.setFirstName(firstName);
//            admin.setLastName(lastName);
//            admin.setEmail(googleUser.getEmail());
//            admin.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Password random
//            admin.setPhoneNumber(null); // Permitir NULL para usuarios de Google
//            admin.setRole(Role.ADMIN);
//            admin.setCreatedAt(ZonedDateTime.now());
//            user = adminRepository.save(admin);
//            applicationEventPublisher.publishEvent(new WelcomeEmailEvent(this, username,email));
//        }
//        String token = jwtService.generatedToken(user);
//        AuthResponseDto response = new AuthResponseDto();
//        response.setToken(token);
//        return response;
//    }

    public AuthResponseDto getCurrentUserInfo() {
        String userEmail = jwtService.getCurrentUserEmail();
        if (userEmail == null) {
            throw new ResourceNotFoundException("Usuario no autenticado");
        }
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(jwtService.generatedToken(user));
        return response;
    }
}