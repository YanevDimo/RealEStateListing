package app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean isActive;
    private String createdAt;
    private String updatedAt;
}













