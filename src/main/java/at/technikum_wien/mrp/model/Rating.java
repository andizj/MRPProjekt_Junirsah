package at.technikum_wien.mrp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rating {
    private int id;
    private int mediaId;
    private int userId;
    private int stars;
    private String comment;
    private boolean visible;
    private LocalDateTime createdAt;
}
