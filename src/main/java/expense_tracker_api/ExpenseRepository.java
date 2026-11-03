package expense_tracker_api;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

interface ExpenseRepository extends CrudRepository<Expense, Long>, PagingAndSortingRepository<Expense, Long> {
}
