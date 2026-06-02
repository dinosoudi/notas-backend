package com.taskflow.tags.mapper;

import com.taskflow.tags.dto.TagRequest;
import com.taskflow.tags.dto.TagResponse;
import com.taskflow.tags.entity.Tag;
import org.mapstruct.*;

/**
 * Mapper de MapStruct para tags.
 *
 * noteCount no viene de la entidad directamente —
 * se calcula con COUNT en la query del repositorio.
 * Por eso el método toResponse recibe noteCount como parámetro separado.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TagMapper {

    // Tag → TagResponse
    // noteCount viene separado porque se calcula en la query
    @Mapping(target = "id", source = "tag.id")
    @Mapping(target = "name", source = "tag.name")
    @Mapping(target = "color", source = "tag.color")
    @Mapping(target = "createdAt", source = "tag.createdAt")
    @Mapping(target = "noteCount", source = "noteCount")
    TagResponse toResponse(Tag tag, Integer noteCount);

    // TagRequest → Tag entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tag toEntity(TagRequest request);

    // Actualizar tag existente desde request
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(TagRequest request, @MappingTarget Tag tag);
}
