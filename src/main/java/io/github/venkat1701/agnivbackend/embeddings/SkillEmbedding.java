package io.github.venkat1701.agnivbackend.embeddings;


import jakarta.persistence.*;

import java.util.List;

@Entity
public class SkillEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String skillName;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Float> embedding;


    public SkillEmbedding(String skillName, List<Float> embedding) {
        this.skillName = skillName;
        this.embedding = embedding;
    }

    public SkillEmbedding() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }
}
