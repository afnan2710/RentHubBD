package com.aoop.renthubbd.service;

import com.aoop.renthubbd.model.Favorite;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.repository.FavoriteRepository;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           PropertyRepository propertyRepository,
                           UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    public List<Favorite> listForUser(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public boolean isFavorited(Long userId, Long propertyId) {
        return favoriteRepository.existsByUserIdAndPropertyId(userId, propertyId);
    }

    public long countForUser(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }

    @Transactional
    public void toggle(Long userId, Long propertyId) {
        if (favoriteRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            favoriteRepository.deleteByUserIdAndPropertyId(userId, propertyId);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));

        Favorite f = new Favorite();
        f.setUser(user);
        f.setProperty(property);
        favoriteRepository.save(f);
    }

    @Transactional
    public void remove(Long userId, Long propertyId) {
        favoriteRepository.deleteByUserIdAndPropertyId(userId, propertyId);
    }
}