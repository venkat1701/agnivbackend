package io.github.venkat1701.agnivbackend.embeddings;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_embedding")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id")
    private String documentId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding_1")
    private Float embedding1;

    @Column(name = "embedding_2")
    private Float embedding2;

    @Column(name = "embedding_3")
    private Float embedding3;

    @Column(name = "embedding_4")
    private Float embedding4;

    @Column(name = "topic")
    private String topic;

    @Column(name = "relevance_score")
    private Float relevanceScore;
}