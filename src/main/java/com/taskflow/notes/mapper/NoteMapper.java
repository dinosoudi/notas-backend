package com.taskflow.notes.mapper;

import com.taskflow.notes.dto.NoteRequest;
import com.taskflow.notes.dto.NoteResponse;
import com.taskflow.notes.entity.Note;
import com.taskflow.tags.entity.Tag;
import org.mapstruct.*;

/**
 * Mapper de MapStruct para notas.
 * Maneja el mapeo de Tag → TagSummary dentro del NoteResponse.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NoteMapper {

    // Note → NoteResponse
    @Mapping(target = "tag", source = "tag")
    NoteResponse toResponse(Note note);

    // Tag → TagSummary (clase interna de NoteResponse)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "color", source = "color")
    NoteResponse.TagSummary toTagSummary(Tag tag);

    // NoteRequest → Note entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tag", ignore = true)       // se resuelve en el Service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Note toEntity(NoteRequest request);

    // Actualizar nota existente desde request
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tag", ignore = true)       // se resuelve en el Service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(NoteRequest request, @MappingTarget Note note);
}
