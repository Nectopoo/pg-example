package test.dev.demo.business.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import test.dev.demo.business.application.dto.TransactionDto;
import test.dev.demo.business.application.entity.Account;
import test.dev.demo.business.application.entity.Transaction;
import test.dev.demo.business.application.mapper.EntityDtoMapper;
import test.dev.demo.business.application.repository.TransactionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public List<TransactionDto> findAllTransactionsByAccount(Long accountId) {
        return transactionRepository.findAllByFromAccount(Account.builder().id(accountId).build()).stream()
            .map(EntityDtoMapper::map)
            .collect(Collectors.toList());
    }

    public void saveTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }

}
