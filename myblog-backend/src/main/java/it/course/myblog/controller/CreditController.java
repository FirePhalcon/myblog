package it.course.myblog.controller;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.course.myblog.entity.Credit;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.repository.CreditRepository;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.UserRepository;

@RestController
@RequestMapping("/credit")
public class CreditController {
	
	@Autowired
	CreditRepository creditRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	PostRepository postRepository;
	
	@PostMapping("/insert-credit")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> insertCredit(@RequestBody Credit credit, HttpServletRequest request){
		
		Optional<Credit> cr = creditRepository.findByCreditCodeAndEndDateIsNull(credit.getCreditCode());
		
		Credit newCr = new Credit();
		
		newCr.setCreditCode(credit.getCreditCode());
		newCr.setCreditDescription(credit.getCreditDescription());
		newCr.setCreditImport(credit.getCreditImport());
		newCr.setStartDate(credit.getStartDate());
		newCr.setEndDate(null);
		if(cr.isPresent()) {
			cr.get().setEndDate(new Date());
			creditRepository.save(cr.get());
		}
		
		creditRepository.save(newCr);
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, "Credit inserted succesfully", request.getRequestURI()), HttpStatus.OK);
	}
	
	@GetMapping("/all-credits")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> getAllCredits(HttpServletRequest request){
		
		List<Credit> allCredits = creditRepository.findByEndDateIsNullOrderByCreditCodeAsc();
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom( Instant.now(), 200, null, allCredits, request.getRequestURI()), HttpStatus.OK);
	}

}
