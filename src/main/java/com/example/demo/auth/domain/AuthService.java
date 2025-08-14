package com.example.demo.auth.domain;


import com.example.demo.auth.dto.AuthResponseDto;
import com.example.demo.auth.dto.LoginRequestDto;
import com.example.demo.auth.dto.RegisterRequestDto;
import com.example.demo.auth.exceptions.UserAlreadyExistException;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.admin.domain.Admin;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.config.JwtService;
import com.example.demo.exceptions.PasswordIncorrectException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.Role;
import com.example.demo.user.infraestructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class  AuthService {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AdminRepository adminRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponseDto login(LoginRequestDto loginDTO){
        User user = userRepository.findByEmail(loginDTO.getEmail()).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if(!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            throw new PasswordIncorrectException("Incorrect Password");
        }
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(jwtService.generatedToken(user));
        return response;
    }


    public AuthResponseDto register(RegisterRequestDto registerDto){
        if (userRepository.findByEmail(registerDto.getEmail()).isPresent()) {
            throw new UserAlreadyExistException("Email already registered");
        }
        String encodedPassword = passwordEncoder.encode(registerDto.getPassword());
        Client client = new Client();
        client.setFirstName(registerDto.getFirstName());
        client.setLastName(registerDto.getLastName());
        client.setUsername(registerDto.getUsername());
        client.setEmail(registerDto.getEmail());
        client.setPassword(encodedPassword);
        client.setPhoneNumber(registerDto.getPhoneNumber());
        client.setRole(Role.FREE);
        client.setCreatedAt(ZonedDateTime.now());
        User newUser = clientRepository.save(client);
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(jwtService.generatedToken(newUser));
        return response;
    }

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

        return response;
    }
}