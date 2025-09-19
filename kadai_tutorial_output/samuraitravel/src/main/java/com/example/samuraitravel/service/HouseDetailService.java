package com.example.samuraitravel.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReservationInputForm;
import com.example.samuraitravel.form.ReviewForm;

@Service
public class HouseDetailService {
    private final ReviewService reviewService;

    public HouseDetailService(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public void prepareHouseDetail(Model model, House house, Pageable pageable, User user) {
        Page<Review> reviewPage = reviewService.findReviewsByHouse(house, pageable);
        model.addAttribute("reviewPage", reviewPage);

        Review userReview = null;
        if (user != null) {
            userReview = reviewService.findByHouseAndUser(house, user).orElse(null);
        }
        model.addAttribute("existingReview", userReview);

        if (!model.containsAttribute("reviewForm")) {
            ReviewForm reviewForm = new ReviewForm();
            if (userReview != null) {
                reviewForm.setRating(userReview.getRating());
                reviewForm.setComment(userReview.getComment());
            }
            model.addAttribute("reviewForm", reviewForm);
        }

        if (!model.containsAttribute("reservationInputForm")) {
            model.addAttribute("reservationInputForm", new ReservationInputForm());
        }
    }
}
