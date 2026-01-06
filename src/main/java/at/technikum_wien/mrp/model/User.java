package at.technikum_wien.mrp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
    private String email;
    private String favoriteGenre;
}
