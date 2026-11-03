package expense_tracker_api;

import java.net.URI;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

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

    @PostMapping
    public ResponseEntity<Void> createExpense(@RequestBody Expense newExpenseRequest,
                                              UriComponentsBuilder ucb) {
        Expense savedExpense = expenseRepository.save(newExpenseRequest);
        URI locationOfNewExpense = ucb
                .path("/expenses/{id}")
                .buildAndExpand(savedExpense.id())
                .toUri();
        return ResponseEntity.created(locationOfNewExpense).build();
    }
}
