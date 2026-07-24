package com.dreamoutbox.ams.announcement;

import com.dreamoutbox.ams.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'LANDLORD')")
    public Announcement createAnnouncement(String title, String content, AnnouncementType type, boolean pinned, User publishedBy) {
        Announcement announcement = Announcement.builder()
                .title(title)
                .content(content)
                .type(type)
                .pinned(pinned)
                .publishedBy(publishedBy)
                .publishedAt(LocalDateTime.now())
                .build();
        return announcementRepository.save(announcement);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'LANDLORD')")
    public Announcement updateAnnouncement(UUID id, String title, String content, AnnouncementType type, boolean pinned) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found: " + id));

        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType(type);
        announcement.setPinned(pinned);

        return announcementRepository.save(announcement);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'LANDLORD')")
    public void deleteAnnouncement(UUID id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found: " + id));
        announcementRepository.delete(announcement);
    }

    @Transactional(readOnly = true)
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByPinnedDescPublishedAtDesc();
    }

    @Transactional(readOnly = true)
    public Announcement getById(UUID id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found: " + id));
    }
}
