package com.example.samuraitravel.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReviewForm;
import com.example.samuraitravel.repository.ReviewRepository;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Page<Review> findReviewsByHouse(House house, Pageable pageable) {
        return reviewRepository.findByHouseOrderByCreatedAtDesc(house, pageable);
    }

    public Optional<Review> findByHouseAndUser(House house, User user) {
        return reviewRepository.findByHouseAndUser(house, user);
    }

    public Review findByIdAndUser(Integer id, User user) {
        return reviewRepository.findByIdAndUser(id, user).orElseThrow(() -> new IllegalArgumentException("レビューが見つかりません"));
    }

    @Transactional
    public Review create(House house, User user, ReviewForm reviewForm) {
        if (reviewRepository.findByHouseAndUser(house, user).isPresent()) {
            throw new IllegalStateException("既にレビューが投稿されています");
        }

        Review review = new Review();
        review.setHouse(house);
        review.setUser(user);
        review.setRating(reviewForm.getRating());
        review.setComment(reviewForm.getComment());

        return reviewRepository.save(review);
    }

    @Transactional
    public Review update(Review review, ReviewForm reviewForm) {
        review.setRating(reviewForm.getRating());
        review.setComment(reviewForm.getComment());
        return reviewRepository.save(review);
    }

    @Transactional
    public void delete(Review review) {
        reviewRepository.delete(review);
    }
}
