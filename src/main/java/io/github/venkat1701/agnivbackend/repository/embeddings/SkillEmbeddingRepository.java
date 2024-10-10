package io.github.venkat1701.agnivbackend.repository.embeddings;

import io.github.venkat1701.agnivbackend.embeddings.SkillEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SkillEmbeddingRepository extends JpaRepository<SkillEmbedding, Long> {

    @Transactional
    Optional<SkillEmbedding> findBySkillName(String skillName);
}