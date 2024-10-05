package io.github.venkat1701.agnivbackend.embeddings;

import io.github.venkat1701.agnivbackend.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_embedding")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name_embedding")
    private Float firstNameEmbedding;

    @Column(name = "last_name_embedding")
    private Float lastNameEmbedding;

    @Column(name = "role_embedding")
    private Float roleEmbedding;

    @Column(name = "email_embedding")
    private Float emailEmbedding;

    @Column(name = "password_strength_embedding")
    private Float passwordStrengthEmbedding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
