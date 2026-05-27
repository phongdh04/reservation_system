package com.example.qlnh.helpers;

import com.example.qlnh.dto.request.CreateReservationRequest;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.services.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationPayloadResolver {

    private final IUserService userService;

    // TODO: Neu user da login (JWT hop le), tu dong fill name/email/phone vao request
    public void resolveUserInfo(CreateReservationRequest request, Authentication auth) {
        if (auth == null) return;
        boolean isLoggedIn = auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        if (isLoggedIn) {
            String email = auth.getName();
            User user = userService.getUserByEmail(email);
            if (user != null) {
                request.setEmail(email);
                request.setName(user.getName());
                if (user.getPhone() != null && !user.getPhone().isBlank()) {
                    request.setPhone(user.getPhone());
                }
            }
        }
    }

    // TODO: Neu chi co reservationAt (datetime gop), tach ra date + time
    public void resolveDatetime(CreateReservationRequest request) {
        String date = request.getDate();
        String time = request.getTime();
        String reservationAt = request.getReservationAt();
        if ((isBlank(date) || isBlank(time)) && !isBlank(reservationAt)) {
            String[] parts = reservationAt.contains("T") ? reservationAt.split("T") : reservationAt.split(" ");
            if (parts.length == 2) {
                request.setDate(parts[0]);
                String rawTime = parts[1];
                request.setTime(rawTime.length() > 5 ? rawTime.substring(0, 5) : rawTime);
            }
        }
    }

    // TODO: Neu orderDetails trong, dung note lam orderDetails. Neu ca hai trong, dat mac dinh.
    public void resolveOrderDetails(CreateReservationRequest request) {
        if (isBlank(request.getOrderDetails()) && !isBlank(request.getNote())) {
            request.setOrderDetails(request.getNote());
        }
        if (isBlank(request.getOrderDetails())) {
            request.setOrderDetails("Khong co mon dat truoc");
        }
        if (isBlank(request.getOrderType())) {
            request.setOrderType("food");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
