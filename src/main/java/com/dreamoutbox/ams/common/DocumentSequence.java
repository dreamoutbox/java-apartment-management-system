package com.dreamoutbox.ams.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSequence {

    @Id
    @Column(name = "sequence_key", nullable = false)
    private String sequenceKey; // e.g. "INV-202608" or "REC-202608"

    @Column(name = "last_number", nullable = false)
    private Long lastNumber;
}
