package it.course.myblog.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import it.course.myblog.entity.Post;
import it.course.myblog.entity.Users;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.UserRepository;
import it.course.myblog.security.UserPrincipal;
import it.course.myblog.service.FileService;
import it.course.myblog.service.UserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/download")
@Api(value = "File generation", description = "All generation of files based on a class")
@Slf4j
public class FileController {
	
	@Autowired
	PostRepository postRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	UserService userService;
	
	@Autowired
	FileService fileService;
	
	 @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_EDITOR') or hasRole('READER') or hasRole('EDITOR')")
	 @GetMapping("/pdf-post/{postId}")
		@ApiOperation(value = "Post PDF generation", response = ResponseEntity.class)
		@ApiResponses(value = {
				@ApiResponse(code = 200, message = "PDF succesfully generated"),
				@ApiResponse(code = 403, message = "Post not visible or not acquisited"),
				@ApiResponse(code = 404, message = "Post not found")
		})
	 public ResponseEntity<?> downloadPost(
			 @PathVariable
			 @ApiParam(value = "ID of the post", required = true)
			 	Long postId,
			 HttpServletRequest request)  {
		 
		 log.info("Call controller download with Parameters postId {}",	postId);
		 
		 Optional<Post> post = postRepository.findById(postId);
		 
		if (!post.isPresent()) {
			log.error("Post {} not found", postId);
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);
		}

		if (!post.get().isVisible()) {
			log.info("Post {} not visible", post.get().getId());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 403, null, "Post not visible", request.getRequestURI()),
					HttpStatus.FORBIDDEN);
		}
		
		if(post.get().getCredit().getCreditImport() > 0) {
			UserPrincipal up = userService.getAuthenticatedUser();
			if(up.getAuthorities().stream().filter(g -> g.getAuthority().equals("ROLE_READER")).count() > 0 &&
				up.getAuthorities().size() == 1) {
			
				Optional<Users> user = userRepository.findById(up.getId());
				Set<Post> ownedPosts = user.get().getPosts();
				
				if(ownedPosts.stream().filter(p -> (p.getId() == post.get().getId())).count() == 0) {
					log.info("User {} not able to access the post {}. Reason: not acquisited", user.get().getId(), post.get().getId());
					return new ResponseEntity<ApiResponseCustom>(
							new ApiResponseCustom(Instant.now(), 403, null, "Post not owned", request.getRequestURI()),
							HttpStatus.FORBIDDEN);
				}
			}
		}
		
		try {
			log.info("Generating PDF for the post {}", post.get().getId());
			InputStream pdfFile = fileService.createPdfFromPost(post.get());
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("application/pdf"));
			headers.add("Access-Control-Allow-Origin", "*");
			headers.add("Access-Control-Allow-Methods", "GET, POST, PUT");
			headers.add("Access-Control-Allow-Headers", "Content-Type");
			headers.add("Content-Disposition", "inline; filename=" + post.get().getTitle().replaceAll(" ", "_") + " " +	formatter.format(Date.from(Instant.now())) + ".pdf");
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			headers.add("Pragma", "no-cache");
			headers.add("Expires", "0");

			log.info("Succesfully generated PDF of the post {}", post.get().getId());
			return new ResponseEntity<InputStreamResource>(new InputStreamResource(pdfFile), headers, HttpStatus.OK);
		} catch (Exception e) {
			log.error(e.getMessage());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 503, null, "Something went wrong during the generation of the PDF", request.getRequestURI()),
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	 }
	 
	@GetMapping("/excel-report")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> downloadExcel(HttpServletRequest request){
		
		try {
			InputStream excelFile = fileService.createExcel();
			
			HttpHeaders headers = new HttpHeaders();
		    headers.set("Content-Type", "application/vnd.ms-excel;");
		    headers.set("content-length",Integer.toString(excelFile.available()));
		    headers.set("Content-Disposition", "attachment; filename=Report.xls");

			log.info("Succesfully generated Excel document");
			return new ResponseEntity<InputStreamResource>(new InputStreamResource(excelFile), headers, HttpStatus.OK);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 503, null, "Something went wrong during the generation of the PDF", request.getRequestURI()),
					HttpStatus.SERVICE_UNAVAILABLE);
		} catch (IOException e) {
			log.error(e.getMessage());
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 503, null, "Something went wrong during the generation of the PDF", request.getRequestURI()),
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}
}
