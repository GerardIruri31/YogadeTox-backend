package com.example.demo.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleUserInfoDto {
    private String id;
    private String email;
    private String name;
    private String given_name;
    private String family_name;
    @JsonProperty("verified_email")
    private Boolean verified_email;
    private String picture;
    private String locale;
}