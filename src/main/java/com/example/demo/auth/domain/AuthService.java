package com.example.demo.auth.domain;


import com.example.demo.auth.dto.AuthResponseDto;
import com.example.demo.auth.dto.LoginRequestDto;
import com.example.demo.auth.dto.RegisterRequestDto;
import com.example.demo.user.domain.User;
import com.example.demo.user.infraestructure.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper modelMapper;

    public AuthResponseDto login(LoginRequestDto loginRequestDto) {
        User sessionUser = userRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(()-> new RuntimeException("No user found"));
        AuthResponseDto response = new AuthResponseDto();
        response.setToken("User successfully logged in");
        return response;
     }

    public AuthResponseDto register(RegisterRequestDto registerRequestDto) {
        User newUser = modelMapper.map(registerRequestDto, User.class);
        newUser.setCreatedAt(ZonedDateTime.now());
        userRepository.save(newUser);
        AuthResponseDto response = new AuthResponseDto();
        response.setToken("User successfully registered");
        return response;
    }
}
