package io.github.venkat1701.agnivbackend.repository.embeddings;

import io.github.venkat1701.agnivbackend.embeddings.SkillEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillEmbeddingRepository extends JpaRepository<SkillEmbedding, Long> {
    Optional<SkillEmbedding> findBySkillName(String skillName);
}