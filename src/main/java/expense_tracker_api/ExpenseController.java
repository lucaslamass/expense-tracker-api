package expense_tracker_api;

import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummary> getSummary(Principal principal) {
        List<Expense> expenses = expenseRepository.findAllByOwner(principal.getName());

        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new TreeMap<>();
        for (Expense expense : expenses) {
            total = total.add(expense.amount());
            byCategory.merge(expense.category(), expense.amount(), BigDecimal::add);
        }
        return ResponseEntity.ok(new ExpenseSummary(total, byCategory));
    }

    @PostMapping
    public ResponseEntity<Void> createExpense(@Valid @RequestBody Expense newExpenseRequest,
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

    @PutMapping("/{requestedId}")
    public ResponseEntity<Void> updateExpense(@PathVariable Long requestedId,
                                              @Valid @RequestBody Expense expenseUpdate,
                                              Principal principal) {
        Expense existingExpense = expenseRepository.findByIdAndOwner(requestedId, principal.getName());
        if (existingExpense == null) {
            return ResponseEntity.notFound().build();
        }
        Expense updatedExpense = new Expense(existingExpense.id(), expenseUpdate.description(),
                expenseUpdate.amount(), expenseUpdate.date(),
                expenseUpdate.category(), principal.getName());
        expenseRepository.save(updatedExpense);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id, Principal principal) {
        if (!expenseRepository.existsByIdAndOwner(id, principal.getName())) {
            return ResponseEntity.notFound().build();
        }
        expenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
