package io.github.rihyri.til02.week06.day01.example.account;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/week06/day01/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountRepository accountRepository;

    // 특정 계좌의 정보를 조회한다.
    @GetMapping("/{id}")
    public Account get(@PathVariable long id) {
        return accountRepository.findById(id);
    }
}
