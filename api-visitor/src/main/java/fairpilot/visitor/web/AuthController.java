package fairpilot.visitor.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.user.AuthService;
import com.fairpilot.core.user.LoginRequest;
import com.fairpilot.core.user.SignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 회원가입 (VISITOR만 가능) */
    @PostMapping("/signup")
    public ApiResponse<Void> signUp(@Valid @RequestBody SignUpRequest req) {
        authService.signUp(req.email(), req.password(), req.name(), req.phone());
        return ApiResponse.ok(null);
    }

    /** 로그인 → JWT 반환 */
    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req.email(), req.password());
        return ApiResponse.ok(Map.of("token", token));
    }
}