package at.technikum_wien.mrp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaEntry {
    private int id;
    private String title;
    private String description;
    private String mediaType;
    private int releaseYear;
    private String[] genres;
    private int ageRestriction;
    private int creatorId;
    private LocalDateTime createdAt;
}
