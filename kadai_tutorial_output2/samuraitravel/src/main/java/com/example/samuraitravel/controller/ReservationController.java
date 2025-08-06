package com.example.samuraitravel.controller;

import java.time.LocalDate;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Reservation;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReservationInputForm;
import com.example.samuraitravel.form.ReservationRegisterForm;
import com.example.samuraitravel.repository.HouseRepository;
import com.example.samuraitravel.repository.ReservationRepository;
import com.example.samuraitravel.security.UserDetailsImpl;
import com.example.samuraitravel.service.ReservationService;
import com.example.samuraitravel.service.StripeService;

@Controller
public class ReservationController {
    private final ReservationRepository reservationRepository;   
    private final HouseRepository houseRepository;
    private final ReservationService reservationService; 
    private final StripeService stripeService; 

    public ReservationController(ReservationRepository reservationRepository, HouseRepository houseRepository, ReservationService reservationService, StripeService stripeService) { 
        this.reservationRepository = reservationRepository; 
        this.houseRepository = houseRepository;
        this.reservationService = reservationService;
        this.stripeService = stripeService;

    }    

    @GetMapping("/reservations")
    public String index(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PageableDefault(page = 0, size = 10, sort = "id", direction = Direction.ASC) Pageable pageable, Model model) {
        User user = userDetailsImpl.getUser();
        Page<Reservation> reservationPage = reservationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        model.addAttribute("reservationPage", reservationPage);         
        
        return "reservations/index";
    }
    
    //宿泊予約ページの入力内容をチェックする
    @GetMapping("/houses/{id}/reservations/input")
    public String input(@PathVariable(name = "id") Integer id, //URLにidを入れる
                        @ModelAttribute @Validated ReservationInputForm reservationInputForm,
                        //@ModelAttribute：フォームからのデータ受取り @Validated：入力チェックする
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        Model model)
    {   
    	//
        House house = houseRepository.getReferenceById(id);
        //予約人数を取得する　Formから
        Integer numberOfPeople = reservationInputForm.getNumberOfPeople();   
        //定員を取得する
        Integer capacity = house.getCapacity();
        
        //もし宿泊人数が入力されている場合
        if (numberOfPeople != null) {
            // もし宿泊人数が定員を超えていたらエラーを追加
            if (!reservationService.isWithinCapacity(numberOfPeople, capacity)) {
            	// エラーメッセージを作成してBindingResultに追加
                FieldError fieldError = new FieldError(bindingResult.getObjectName(), "numberOfPeople", "宿泊人数が定員を超えています。");
                bindingResult.addError(fieldError);                
            }            
        }         
        
        if (bindingResult.hasErrors()) {            
            model.addAttribute("house", house);            
            model.addAttribute("errorMessage", "予約内容に不備があります。"); 
            return "houses/show";
        }
        
        redirectAttributes.addFlashAttribute("reservationInputForm", reservationInputForm);           
        
        return "redirect:/houses/{id}/reservations/confirm";
    }    
    
    //予約の確認ページを表示する
    @GetMapping("/houses/{id}/reservations/confirm")
    public String confirm(@PathVariable(name = "id") Integer id,
                          @ModelAttribute ReservationInputForm reservationInputForm,
                          @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, 
                          HttpServletRequest httpServletRequest,
                          Model model) 
    {        
        House house = houseRepository.getReferenceById(id);
        User user = userDetailsImpl.getUser(); 
                
        //チェックイン日とチェックアウト日を取得する
        LocalDate checkinDate = reservationInputForm.getCheckinDate();
        LocalDate checkoutDate = reservationInputForm.getCheckoutDate();
 
        // 宿泊料金を計算する
        Integer price = house.getPrice();        
        Integer amount = reservationService.calculateAmount(checkinDate, checkoutDate, price);
        
        ReservationRegisterForm reservationRegisterForm = new ReservationRegisterForm(house.getId(), user.getId(), checkinDate.toString(), checkoutDate.toString(), reservationInputForm.getNumberOfPeople(), amount);

        String sessionId = stripeService.createStripeSession(house.getName(), reservationRegisterForm, httpServletRequest);
        
        model.addAttribute("house", house);  
        model.addAttribute("reservationRegisterForm", reservationRegisterForm);   
        model.addAttribute("sessionId", sessionId);

        
        return "reservations/confirm";
    }    
    /*   
    //次のURLのPOSTrequestを受け付ける
    @PostMapping("/houses/{id}/reservations/create")
    //@ModelAttribute フォームからのデータを受け取り
    public String create(@ModelAttribute ReservationRegisterForm reservationRegisterForm) {     
    	//reservationServiceクラスのcreateメソッドを呼び出し
        reservationService.create(reservationRegisterForm);        
        
        //ダイレクト redirect: は Spring MVC で使う 特別な戻り値の書き方
        return "redirect:/reservations?reserved";
    }
       */   
}