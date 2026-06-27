package com.trackpro.controller;

import com.trackpro.model.Expense;
import com.trackpro.service.FleetService;
import com.trackpro.service.FleetService.ExpenseRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final FleetService fleetService;

    public ExpenseController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public Page<Expense> list(@PageableDefault(size = 50) Pageable pageable) {
        return fleetService.listExpenses(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expense create(@Valid @RequestBody ExpenseRequest req) {
        return fleetService.createExpense(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fleetService.deleteExpense(id);
    }
}
