package com.example.demo.user.domain;

import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.user.domain.User;
import com.example.demo.user.dto.PatchUserInfoRequest;
import com.example.demo.user.dto.UserProfileResponse;
import com.example.demo.user.dto.UserResponseDto;
import com.example.demo.user.infraestructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @PostConstruct
    public void configureModelMapper() {
        modelMapper.createTypeMap(User.class, UserProfileResponse.class)
                .addMappings(mapper -> {
                    mapper.map(User::getRealUsername, UserProfileResponse::setUsername);
                });
    }

    public UserProfileResponse userInfo(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return modelMapper.map(user, UserProfileResponse.class);
    }

    public UserProfileResponse actualizeUserInfo(Long id, PatchUserInfoRequest dto) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        userRepository.save(user);
        return modelMapper.map(user, UserProfileResponse.class);
    }

    @Bean(name = "UserDetailsService")
    public UserDetailsService userDetailsService(){
        return username -> {
            User user = userRepository.findByEmail(username).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            return (UserDetails) user;
        };
    }

    //Recomiendo usar esto para el Get Me por lado del Cliente
    public UserResponseDto getMe(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        String email = ((UserDetails) auth.getPrincipal()).getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return modelMapper.map(user, UserResponseDto.class);
    }
}
