package com.example.authservice.repo;

import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /* ---- admin platform statistikası ---- */

    long countByRole(Role role);

    /** "banned" sütunu nullable-dır (köhnə sətirlərdə null = bloklanmayıb),
     *  ona görə yalnız açıq-aşkar TRUE olanları sayırıq. */
    long countByBannedTrue();

    long countByCreatedAtAfter(LocalDateTime since);

    /** Ən çox istifadəçisi olan şəhərlər. Hər sətir: [şəhər adı, say].
     *  Limit üçün Pageable ötürülür (JPQL-də LIMIT yoxdur). */
    @Query("SELECT u.city, COUNT(u) FROM User u WHERE u.city IS NOT NULL AND u.city <> '' " +
           "GROUP BY u.city ORDER BY COUNT(u) DESC")
    List<Object[]> countUsersByCity(Pageable pageable);
}
