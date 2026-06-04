package domain.post;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
public class TemporaryPost {
    private long temporaryKey;
    private String title;
    private String content;
    private String image;
    private LocalDateTime saveTime;
}
