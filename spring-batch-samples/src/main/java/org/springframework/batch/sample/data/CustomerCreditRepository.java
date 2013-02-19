package org.springframework.batch.sample.data;

import java.math.BigDecimal;

import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CustomerCreditRepository extends PagingAndSortingRepository<CustomerCredit, Long>{
	Page<CustomerCredit> findByCreditGreaterThan(BigDecimal credit, Pageable request);
}
