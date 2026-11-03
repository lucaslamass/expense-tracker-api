package expense_tracker_api;

import org.springframework.data.repository.CrudRepository;

interface ExpenseRepository extends CrudRepository<Expense, Long> {
}