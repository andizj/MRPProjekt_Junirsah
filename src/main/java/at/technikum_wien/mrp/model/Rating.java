package at.technikum_wien.mrp.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rating {
    private int id, mediaId, userId, score;
    private String comment;

    public void setScore(int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("score must be between 1 and 5");
        }
        this.score = score;
    }
}

