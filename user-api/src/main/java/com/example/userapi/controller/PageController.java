package com.example.userapi.controller;

import com.example.userapi.annotation.CurrentUser;
import com.example.userapi.entity.User;
import com.example.userapi.entity.Wallet;
import com.example.userapi.enums.UserStatus;
import com.example.userapi.exception.AuthenticationException;
import com.example.userapi.exception.DuplicateResourceException;
import com.example.userapi.service.AuthService;
import com.example.userapi.service.UserService;
import com.example.userapi.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PageController {

    private final AuthService authService;
    private final WalletService walletService;
    private final UserService userService;

    public PageController(AuthService authService,
                          WalletService walletService, UserService userService) {
        this.authService = authService;
        this.walletService = walletService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@CurrentUser User user) {
        if (user != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {
        try {
            User user = authService.login(username, password, request.getRemoteAddr());
            request.getSession().setAttribute("user", user);

            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(ur -> new SimpleGrantedAuthority(ur.getRole().getName()))
                    .toList();
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities));
            SecurityContextHolder.setContext(securityContext);
            new HttpSessionSecurityContextRepository().saveContext(securityContext, request, response);

            return "redirect:/dashboard";
        } catch (AuthenticationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(@CurrentUser User user) {
        if (user != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "密碼與確認密碼不一致");
            return "redirect:/register";
        }
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "密碼長度需至少 8 個字元");
            return "redirect:/register";
        }

        try {
            userService.register(username,email,password,UserStatus.ENABLED);

            redirectAttributes.addFlashAttribute("success", "註冊成功，請登入");
            return "redirect:/login";
        } catch (DuplicateResourceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(@CurrentUser User user, Model model) {
        model.addAttribute("user", user);

        try {
            Wallet wallet = walletService.getWalletByUserId(user.getId());
            model.addAttribute("walletBalance", wallet.getBalance());
        } catch (Exception e) {
            model.addAttribute("walletBalance", "N/A");
        }

        return "dashboard";
    }

    @GetMapping("/wallet")
    public String walletPage(@CurrentUser User user, Model model) {
        model.addAttribute("user", user);
        return "wallet";
    }

    @GetMapping("/stocks")
    public String stocksPage(@CurrentUser User user, Model model) {
        model.addAttribute("user", user);
        return "stocks";
    }

    @PostMapping("/account/delete")
    public String deleteAccount(@CurrentUser User user,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(user.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }

        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "帳號已刪除");
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        return "redirect:/login";
    }
}
