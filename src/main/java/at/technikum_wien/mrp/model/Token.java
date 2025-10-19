package at.technikum_wien.mrp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    private String token;
    private int userId;
    private LocalDateTime createdAt;
}

