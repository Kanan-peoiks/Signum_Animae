package com.example.authservice.repo;

import com.example.authservice.model.ArtistProfile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

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
