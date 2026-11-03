package expense_tracker_api;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;

public record Expense(@Id Long id, String description, BigDecimal amount,
                      LocalDate date, String category, String owner) {
}
