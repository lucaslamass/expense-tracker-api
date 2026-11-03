package expense_tracker_api;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    @GetMapping("/{requestedId}")
    public ResponseEntity<Expense> findById(@PathVariable Long requestedId) {
        if (requestedId.equals(99L)) {
            Expense expense = new Expense(99L, "Compra supermercado",
                    new BigDecimal("45.30"), LocalDate.of(2026, 11, 3), "Comida");
            return ResponseEntity.ok(expense);
        }
        return ResponseEntity.notFound().build();
    }
}