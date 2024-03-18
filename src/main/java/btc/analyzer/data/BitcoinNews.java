package btc.analyzer.data;

import lombok.*;

import java.time.LocalDate;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BitcoinNews {

    private LocalDate date;
    private String title;
    private String content;
    private boolean isPositive;
}
