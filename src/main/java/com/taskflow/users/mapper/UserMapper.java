package com.taskflow.users.mapper;

import com.taskflow.shared.dto.UserDTO;
import com.taskflow.users.entity.User;
import org.mapstruct.*;

/**
 * Mapper de MapStruct para convertir entre User entity y UserDTO.
 *
 * MapStruct genera la implementación en tiempo de compilación —
 * no usa reflexión, es código Java puro generado automáticamente.
 *
 * componentModel = "spring" hace que MapStruct genere un @Component
 * para que Spring lo inyecte con @Autowired o constructor injection.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    /**
     * Convierte User entity → UserDTO.
     *
     * MapStruct mapea automáticamente campos con el mismo nombre.
     * Solo necesitas declarar explícitamente los que difieren.
     *
     * User.id          → UserDTO.id          ✓ mismo nombre
     * User.name        → UserDTO.name        ✓ mismo nombre
     * User.email       → UserDTO.email       ✓ mismo nombre
     * User.phone       → UserDTO.phone       ✓ mismo nombre
     * User.emailVerified → UserDTO.emailVerified ✓ mismo nombre
     * User.authProvider → UserDTO.authProvider  ✓ mismo nombre
     * User.createdAt   → UserDTO.createdAt   ✓ mismo nombre
     *
     * Campos ignorados intencionalmente — nunca exponer en responses:
     * passwordHash, resetCode, resetCodeExpires, resetAttempts,
     * verificationToken, verificationExpires, preferences,
     * deletedAt, deletionScheduledAt, tags, notes, refreshTokens
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "emailVerified", source = "emailVerified")
    @Mapping(target = "authProvider", source = "authProvider")
    @Mapping(target = "createdAt", source = "createdAt")
    UserDTO toDTO(User user);

    /**
     * Convierte RegisterRequest → User entity.
     * Los campos que el backend asigna (id, createdAt, etc.) se ignoran
     * porque los maneja Hibernate o el Service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)      // lo hashea AuthService
    @Mapping(target = "emailVerified", ignore = true)     // default false
    @Mapping(target = "authProvider", ignore = true)      // default EMAIL
    @Mapping(target = "resetCode", ignore = true)
    @Mapping(target = "resetCodeExpires", ignore = true)
    @Mapping(target = "resetAttempts", ignore = true)
    @Mapping(target = "verificationToken", ignore = true)
    @Mapping(target = "verificationExpires", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletionScheduledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    User toEntity(com.taskflow.auth.dto.RegisterRequest request);

    /**
     * Actualiza campos de un User existente desde un request de actualización.
     * @MappingTarget indica que el primer parámetro es el objeto a modificar.
     * NullValuePropertyMappingStrategy.IGNORE hace que los campos null
     * del request no sobreescriban los valores existentes en la entidad.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)             // email no se edita en v1
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "phone", ignore = true)             // phone tiene su propio endpoint
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "resetCode", ignore = true)
    @Mapping(target = "resetCodeExpires", ignore = true)
    @Mapping(target = "resetAttempts", ignore = true)
    @Mapping(target = "verificationToken", ignore = true)
    @Mapping(target = "verificationExpires", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletionScheduledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    void updateEntityFromRequest(
            com.taskflow.users.dto.UpdateNameRequest request,
            @MappingTarget User user
    );
}