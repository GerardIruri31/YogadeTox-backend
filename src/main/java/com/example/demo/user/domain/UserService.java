package com.example.demo.user.domain;

import com.example.demo.user.domain.User;
import com.example.demo.user.dto.PatchUserInfoRequest;
import com.example.demo.user.dto.UserProfileResponse;
import com.example.demo.user.infraestructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

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
}
