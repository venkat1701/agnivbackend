package io.github.venkat1701.agnivbackend.repository.embeddings;

import io.github.venkat1701.agnivbackend.embeddings.UserEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserEmbeddingRepository extends JpaRepository<UserEmbedding, Long> {

    @Query(value = "SELECT * FROM user_embedding " +
            "ORDER BY " +
            "SQRT(POW(:e1 - embedding_1, 2) + " +
            "POW(:e2 - embedding_2, 2) + " +
            "POW(:e3 - embedding_3, 2) + " +
            "POW(:e4 - embedding_4, 2)) " +
            "LIMIT 10", nativeQuery = true)
    List<UserEmbedding> findSimilarUsersByEmbedding(
            @Param("e1") Float e1,
            @Param("e2") Float e2,
            @Param("e3") Float e3,
            @Param("e4") Float e4);
}