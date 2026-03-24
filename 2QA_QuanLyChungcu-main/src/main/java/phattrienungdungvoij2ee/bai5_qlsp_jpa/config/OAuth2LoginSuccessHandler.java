package phattrienungdungvoij2ee.bai5_qlsp_jpa.config;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Account;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.model.Role;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.AccountRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.repository.RoleRepository;
import phattrienungdungvoij2ee.bai5_qlsp_jpa.service.AccountService;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
//this is new
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    public OAuth2LoginSuccessHandler(
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AccountService accountService
    ) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        String email = null;

        if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            email = oidcUser.getEmail();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            Object maybeEmail = oauth2User.getAttributes().get("email");
            if (maybeEmail != null) {
                email = maybeEmail.toString();
            }
        }

        if (email == null || email.trim().isEmpty()) {
            String targetUrl = UriComponentsBuilder.fromPath("/login")
                    .queryParam("oauth2error", "missing_email")
                    .build()
                    .toUriString();
            response.sendRedirect(targetUrl);
            return;
        }

        final String finalEmail = email.trim().toLowerCase();

        accountRepository.findByLoginName(finalEmail).orElseGet(() -> {
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_USER");
                        return roleRepository.save(r);
                    });

            Account account = new Account();
            account.setLogin_name(finalEmail);
            account.setPassword(passwordEncoder.encode(randomPassword()));
            account.getRoles().add(roleUser);
            return accountRepository.save(account);
        });

        UserDetails userDetails = accountService.loadUserByUsername(finalEmail);
        UsernamePasswordAuthenticationToken internalAuth =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
        internalAuth.setDetails(authentication.getDetails());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(internalAuth);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        response.sendRedirect("/Apartments");
    }

    private static String randomPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);//
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
