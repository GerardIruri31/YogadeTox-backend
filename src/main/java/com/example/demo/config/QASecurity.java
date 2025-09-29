package com.example.demo.config;

import com.example.demo.qa.infraestructure.QARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QASecurity {
    private final QARepository qaRepository;

    /**
     * principalName es el getName() del Authentication en el WS CONNECT.
     * En nuestro interceptor lo seteamos como userId (String).
     */
    public boolean isOwner(String principalName, Long qaId) {
        if (principalName == null) return false;
        Long userId;
        try {
            userId = Long.valueOf(principalName);
        } catch (NumberFormatException e) {
            return false;
        }
        return qaRepository.findById(qaId)
                .map(qa -> qa.getClient() != null && qa.getClient().getId().equals(userId))
                .orElse(false);
    }
}
