package com.example.demo.content.domain;

import com.example.demo.content.dto.ContentRequestDto;
import com.example.demo.content.dto.ContentResponse;
import com.example.demo.content.infraestructure.ContentRepository;
import com.example.demo.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentService {
    private final ContentRepository contentRepository;
    private final ModelMapper modelMapper;

    //Funciones Admin
    
    //Crear curso
    /*
    Falta validar rol, falta validar que los atributos no sean nulos y esas cosas (yo me encargo)
     */
    public ContentResponse createContent(ContentRequestDto contentRequestDto){
        Content content = new Content();
        content.setTitle(contentRequestDto.getTitle());
        content.setIdiom(contentRequestDto.getIdiom());
        content.setKeyS3Bucket(contentRequestDto.getKeyS3Bucket());
        //content.setDuration(contentRequestDto.getDuration());
        content.setDescriptionKeywords(contentRequestDto.getDescriptionKeywords());
        content.setTag(contentRequestDto.getTag());
        content.setIsPremium(contentRequestDto.getIsPremium());
        contentRepository.save(content);
        return modelMapper.map(content, ContentResponse.class);
    }

    public void updateContent(Long contentId, ContentRequestDto dto){
        Content content = contentRepository.findById(contentId).orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado"));
        if(dto.getTitle() != null) content.setTitle(dto.getTitle());
        if(dto.getIdiom() != null) content.setIdiom(dto.getIdiom());
        if(dto.getKeyS3Bucket() != null) content.setKeyS3Bucket(dto.getKeyS3Bucket());
        if(dto.getDescriptionKeywords() != null) content.setDescriptionKeywords(dto.getDescriptionKeywords());
        if(dto.getTag() != null) content.setTag(dto.getTag());
        if(dto.getIsPremium() != null) content.setIsPremium(dto.getIsPremium());
        contentRepository.save(content);
    }





}
