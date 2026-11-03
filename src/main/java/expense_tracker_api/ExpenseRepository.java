package expense_tracker_api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

interface ExpenseRepository extends CrudRepository<Expense, Long>, PagingAndSortingRepository<Expense, Long> {

    Expense findByIdAndOwner(Long id, String owner);

    Page<Expense> findByOwner(String owner, Pageable pageable);

    List<Expense> findAllByOwner(String owner);

    boolean existsByIdAndOwner(Long id, String owner);
}
