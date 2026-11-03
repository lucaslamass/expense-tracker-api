package expense_tracker_api;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;

    public ExpenseController(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @GetMapping("/{requestedId}")
    public ResponseEntity<Expense> findById(@PathVariable Long requestedId) {
        Optional<Expense> expenseOptional = expenseRepository.findById(requestedId);
        if (expenseOptional.isPresent()) {
            return ResponseEntity.ok(expenseOptional.get());
        }
        return ResponseEntity.notFound().build();
    }
}