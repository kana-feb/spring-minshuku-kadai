package com.example.samuraitravel.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReviewForm;
import com.example.samuraitravel.repository.HouseRepository;
import com.example.samuraitravel.security.UserDetailsImpl;
import com.example.samuraitravel.service.HouseDetailService;
import com.example.samuraitravel.service.ReviewService;

@Controller
@RequestMapping("/houses/{houseId}/reviews")
public class ReviewController {
    private static final int DEFAULT_REVIEW_PAGE_SIZE = 5;

    private final HouseRepository houseRepository;
    private final ReviewService reviewService;
    private final HouseDetailService houseDetailService;

    public ReviewController(HouseRepository houseRepository, ReviewService reviewService, HouseDetailService houseDetailService) {
        this.houseRepository = houseRepository;
        this.reviewService = reviewService;
        this.houseDetailService = houseDetailService;
    }

    @PostMapping
    public String create(@PathVariable(name = "houseId") Integer houseId,
                         @ModelAttribute("reviewForm") @Validated ReviewForm reviewForm,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
                         @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                         Model model,
                         RedirectAttributes redirectAttributes)
    {
        if (userDetailsImpl == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        House house = houseRepository.getReferenceById(houseId);
        User user = userDetailsImpl.getUser();
        Pageable pageable = createReviewPageable(page);

        if (bindingResult.hasErrors()) {
            model.addAttribute("house", house);
            model.addAttribute("reviewErrorMessage", "レビューの入力内容に不備があります。");
            houseDetailService.prepareHouseDetail(model, house, pageable, user);
            return "houses/show";
        }

        try {
            reviewService.create(house, user, reviewForm);
        } catch (IllegalStateException e) {
            bindingResult.reject("", "レビューの投稿に失敗しました。");
            model.addAttribute("house", house);
            model.addAttribute("reviewErrorMessage", "レビューの投稿に失敗しました。");
            houseDetailService.prepareHouseDetail(model, house, pageable, user);
            return "houses/show";
        }

        redirectAttributes.addFlashAttribute("reviewSuccessMessage", "レビューを投稿しました。");
        return "redirect:/houses/" + houseId;
    }

    @PostMapping("/{reviewId}/update")
    public String update(@PathVariable(name = "houseId") Integer houseId,
                         @PathVariable(name = "reviewId") Integer reviewId,
                         @ModelAttribute("reviewForm") @Validated ReviewForm reviewForm,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
                         @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                         Model model,
                         RedirectAttributes redirectAttributes)
    {
        if (userDetailsImpl == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        House house = houseRepository.getReferenceById(houseId);
        User user = userDetailsImpl.getUser();
        Pageable pageable = createReviewPageable(page);
        Review review = getUserReview(reviewId, user);

        if (!review.getHouse().getId().equals(house.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("house", house);
            model.addAttribute("reviewErrorMessage", "レビューの入力内容に不備があります。");
            houseDetailService.prepareHouseDetail(model, house, pageable, user);
            return "houses/show";
        }

        reviewService.update(review, reviewForm);
        redirectAttributes.addFlashAttribute("reviewSuccessMessage", "レビューを更新しました。");
        return buildRedirectUrl(houseId, page);
    }

    @PostMapping("/{reviewId}/delete")
    public String delete(@PathVariable(name = "houseId") Integer houseId,
                         @PathVariable(name = "reviewId") Integer reviewId,
                         @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
                         @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                         RedirectAttributes redirectAttributes)
    {
        if (userDetailsImpl == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        House house = houseRepository.getReferenceById(houseId);
        User user = userDetailsImpl.getUser();
        Review review = getUserReview(reviewId, user);

        if (!review.getHouse().getId().equals(house.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        reviewService.delete(review);
        redirectAttributes.addFlashAttribute("reviewSuccessMessage", "レビューを削除しました。");
        return buildRedirectUrl(houseId, page);
    }

    private Pageable createReviewPageable(int page) {
        return PageRequest.of(Math.max(page, 0), DEFAULT_REVIEW_PAGE_SIZE, Sort.by(Direction.DESC, "createdAt"));
    }

    private Review getUserReview(Integer reviewId, User user) {
        try {
            return reviewService.findByIdAndUser(reviewId, user);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private String buildRedirectUrl(Integer houseId, int page) {
        if (page > 0) {
            return "redirect:/houses/" + houseId + "?page=" + page;
        }
        return "redirect:/houses/" + houseId;
    }
}
