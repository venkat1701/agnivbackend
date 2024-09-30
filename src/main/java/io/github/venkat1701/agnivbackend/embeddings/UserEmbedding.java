package io.github.venkat1701.agnivbackend.embeddings;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "embedding_1")
    private Float embedding1;

    @Column(name = "embedding_2")
    private Float embedding2;

    @Column(name = "embedding_3")
    private Float embedding3;

    @Column(name = "embedding_4")
    private Float embedding4;

}
