package dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class TemporaryPost {
    @NotNull
    private long temporaryKey;
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private String image;
    @NotBlank
    private LocalDateTime saveTime;
}
