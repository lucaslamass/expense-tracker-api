package expense_tracker_api;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;

public record Expense(
        @Id Long id,
        @NotBlank(message = "Description is required") String description,
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0") BigDecimal amount,
        @NotNull(message = "Date is required") LocalDate date,
        @NotBlank(message = "Category is required") String category,
        String owner) {
}
