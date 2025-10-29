package app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponseDto {

    private boolean success;
    private String message;
    private Object data;
    private String error;

    public static ApiResponseDto success(String message) {
        return ApiResponseDto.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static ApiResponseDto success(String message, Object data) {
        return ApiResponseDto.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponseDto error(String message) {
        return ApiResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }

    public static ApiResponseDto error(String message, String error) {
        return ApiResponseDto.builder()
                .success(false)
                .message(message)
                .error(error)
                .build();
    }
}













