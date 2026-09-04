package com.example.authservice.repo;

import com.example.authservice.model.ArtistProfile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Null-safe filter builders for artist search.
 *
 * The previous approach used a single JPQL query with "(:city IS NULL OR LOWER(...) LIKE ...)"
 * guards. That broke in practice: when city/style/minRating are not supplied, Hibernate has to
 * bind a null parameter into a position also used inside LOWER(CONCAT(...)), and in doing so the
 * PostgreSQL JDBC driver sent that null with an unspecified/bytea type - causing
 * "function lower(bytea) does not exist" at the database level.
 *
 * Specifications avoid the problem entirely: when a filter isn't supplied we simply don't add a
 * predicate for it, so LOWER() is never evaluated against a null parameter in the first place.
 */
public final class ArtistSpecifications {

    private ArtistSpecifications() {
    }

    public static Specification<ArtistProfile> hasCity(String city) {
        if (!StringUtils.hasText(city)) {
            return null;
        }
        String pattern = "%" + city.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.join("user").get("city")), pattern);
    }

    public static Specification<ArtistProfile> hasStyle(String style) {
        if (!StringUtils.hasText(style)) {
            return null;
        }
        String pattern = "%" + style.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("styles")), pattern);
    }

    public static Specification<ArtistProfile> minRating(Double minRating) {
        if (minRating == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ratingAvg"), minRating);
    }

    public static Specification<ArtistProfile> minExperience(Integer minExperienceYears) {
        if (minExperienceYears == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("experienceYears"), minExperienceYears);
    }
}
