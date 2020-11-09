package it.course.myblog.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.course.myblog.entity.Credit;

@Repository
public interface CreditRepository extends JpaRepository<Credit, Long>{
	
	Optional<Credit> findByCreditCodeAndEndDateIsNull(String creditCode);
	
	Optional<Credit> findByCreditCode(String creditCode);
	
	List<Credit> findByEndDateIsNullOrderByCreditCodeAsc();
	
	List<Credit> findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndCreditCode(Date startDate, Date EndDate, String creditCode);
	
	List<Credit> findByStartDateLessThanEqualAndEndDateIsNullAndCreditCode(Date startDate, String creditCode);
	
	Optional<Credit> findByEndDateIsNullAndCreditCodeStartingWith(String str);
}
