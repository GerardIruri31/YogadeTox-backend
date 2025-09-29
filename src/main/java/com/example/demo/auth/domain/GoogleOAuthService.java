package com.example.demo.auth.domain;

import com.example.demo.auth.dto.GoogleTokenResponseDto;
import com.example.demo.auth.dto.GoogleUserInfoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.client.id}")
    private String clientId;
    @Value("${google.client.secret}")
    private String clientSecret;
    @Value("${server.host:http://localhost:8080}")
    private String serverHost;
    
    public GoogleUserInfoDto getUserInfo(String code) {
        // 1. Intercambiar código por access token
        String accessToken = getAccessToken(code);
        // 2. Usar access token para obtener información del usuario
        return getUserProfile(accessToken);
    }

    private String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", serverHost + "/auth/grant-code");
        params.add("grant_type", "authorization_code");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        String tokenUrl = "https://oauth2.googleapis.com/token";
        ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, request, String.class);
        try {
            GoogleTokenResponseDto tokenResponse = objectMapper.readValue(response.getBody(), GoogleTokenResponseDto.class);
            return tokenResponse.getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Google token response", e);
        }
    }

    private GoogleUserInfoDto getUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, String.class);
        try {
            return objectMapper.readValue(response.getBody(), GoogleUserInfoDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Google user info", e);
        }
    }
}