package io.github.rihyri.til02.week06.day01.example.transfer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/week06/day01/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final JdbcTransferService jdbcService;
    private final ProgrammaticTransferService programmaticService;
    private final DeclarativeTransferService declarativeService;

    // JDBC 방식으로 계좌 이체
    @PostMapping("/jdbc")
    public ResponseEntity<Void> jdbc(@Valid @RequestBody TransferRequest request) {

        jdbcService.transfer(request);
        return ResponseEntity.noContent().build();
    }

    // 프로그래밍 방식으로 계좌 이체
    @PostMapping("/programmatic")
    public ResponseEntity<Void> programmatic(@Valid @RequestBody TransferRequest request) {

        programmaticService.transfer(request);
        return ResponseEntity.noContent().build();
    }

    // 선언적 트랜잭션으로 계좌 이체
    @PostMapping("/declarative")
    public ResponseEntity<Void> declarative(@Valid @RequestBody TransferRequest request) {

        declarativeService.transfer(request);
        return ResponseEntity.noContent().build();
    }
}
