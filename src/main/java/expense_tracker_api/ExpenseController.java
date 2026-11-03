package expense_tracker_api;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<Expense> findById(@PathVariable Long requestedId, Principal principal) {
        Expense expense = expenseRepository.findByIdAndOwner(requestedId, principal.getName());
        if (expense != null) {
            return ResponseEntity.ok(expense);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Expense>> findAll(Pageable pageable, Principal principal) {
        Page<Expense> page = expenseRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.DESC, "date"))
                ));
        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping
    public ResponseEntity<Void> createExpense(@RequestBody Expense newExpenseRequest,
                                              UriComponentsBuilder ucb,
                                              Principal principal) {
        // El dueño siempre es el usuario autenticado, nunca el que venga en el JSON
        Expense expenseWithOwner = new Expense(null, newExpenseRequest.description(),
                newExpenseRequest.amount(), newExpenseRequest.date(),
                newExpenseRequest.category(), principal.getName());
        Expense savedExpense = expenseRepository.save(expenseWithOwner);
        URI locationOfNewExpense = ucb
                .path("/expenses/{id}")
                .buildAndExpand(savedExpense.id())
                .toUri();
        return ResponseEntity.created(locationOfNewExpense).build();
    }
}
