package com.example.authservice.service;

import com.example.authservice.dto.ArtistProfileDto;
import com.example.authservice.exception.AlreadyFollowingException;
import com.example.authservice.exception.ArtistNotFoundException;
import com.example.authservice.exception.UserNotFoundException;
import com.example.authservice.model.ArtistFollow;
import com.example.authservice.repo.ArtistFollowRepository;
import com.example.authservice.repo.ArtistProfileRepository;
import com.example.authservice.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Müştərinin usta izləməsi (favoritlər).
 *
 *  Kimin kimi izlədiyini yoxlamaq gateway-in işidir: bu servisə yalnız gateway-in
 *  doğruladığı sorğular gəlir, ona görə customerId body/query-dən götürülür -
 *  layihədəki bütün digər endpoint-lərlə eyni məntiq (məs. bron yaradılması). */
@Service
@RequiredArgsConstructor
public class ArtistFollowService {

    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final UserRepo userRepository;
    private final ArtistService artistService;

    @Transactional
    public void follow(Long customerId, Long artistId) {
        if (!userRepository.existsById(customerId)) {
            throw new UserNotFoundException("İstifadəçi tapılmadı! ID: " + customerId);
        }
        if (artistProfileRepository.findByUserId(artistId).isEmpty()) {
            throw new ArtistNotFoundException("Rəssam tapılmadı! userId: " + artistId);
        }
        if (artistFollowRepository.existsByCustomerIdAndArtistId(customerId, artistId)) {
            throw new AlreadyFollowingException("Bu ustanı artıq izləyirsən.");
        }

        artistFollowRepository.save(ArtistFollow.builder()
                .customerId(customerId)
                .artistId(artistId)
                .build());
    }

    /** İdempotentdir: izləmə yoxdursa da səssizcə uğurlu sayılır - istifadəçi düyməyə
     *  iki dəfə basanda UI-da yalançı xəta çıxmasın. */
    @Transactional
    public void unfollow(Long customerId, Long artistId) {
        artistFollowRepository.findByCustomerIdAndArtistId(customerId, artistId)
                .ifPresent(artistFollowRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long customerId, Long artistId) {
        return artistFollowRepository.existsByCustomerIdAndArtistId(customerId, artistId);
    }

    @Transactional(readOnly = true)
    public long followerCount(Long artistId) {
        return artistFollowRepository.countByArtistId(artistId);
    }

    /** İzlənən ustaların profil xülasəsi. Profili tapılmayan sətir (məs. istifadəçi
     *  silinibsə) sadəcə buraxılır - bütün siyahını sındırmağın mənası yoxdur. */
    @Transactional(readOnly = true)
    public List<ArtistProfileDto> followedArtists(Long customerId) {
        List<ArtistProfileDto> result = new ArrayList<>();
        for (ArtistFollow follow : artistFollowRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)) {
            artistProfileRepository.findByUserId(follow.getArtistId())
                    .ifPresent(profile -> result.add(artistService.toDto(profile)));
        }
        return result;
    }
}
