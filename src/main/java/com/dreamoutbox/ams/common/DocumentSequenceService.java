package com.dreamoutbox.ams.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentSequenceService {

    private final DocumentSequenceRepository repository;

    @Transactional
    public synchronized String getNextNumber(String docType, String periodYm) {
        String key = docType + "-" + periodYm;
        DocumentSequence seq = repository.findById(key)
                .orElseGet(() -> DocumentSequence.builder().sequenceKey(key).lastNumber(0L).build());

        long next = seq.getLastNumber() + 1;
        seq.setLastNumber(next);
        repository.save(seq);

        return String.format("%s-%s-%05d", docType, periodYm, next);
    }
}
