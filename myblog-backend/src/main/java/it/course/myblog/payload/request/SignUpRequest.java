package it.course.myblog.payload.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@ApiModel(value = "SignUpRequest object is used to register a new user")
public class SignUpRequest {
	
	@Size(max = 100)
	@ApiModelProperty(notes = "Insert name for the new user")
	private String name;
	
	@Size(max = 100)
	@ApiModelProperty(notes = "Insert lastname for the new user")
	private String lastname;
	
	@NotBlank
	@Size(min = 3, max = 20)
	@Pattern(regexp = "[A-Za-z0-9]+", message = "Please provide a valid username")
	@ApiModelProperty(notes = "Insert username for the new user")
	private String username;
	
	@NotBlank
	@Size(min = 6, max = 120)
	@Email
	@Pattern(regexp = ".+@.+\\..+", message = "Please provide a valid email address")
	@ApiModelProperty(notes = "Insert email for the new user")
	private String email;
	
	@NotBlank
	@Size(min = 6, max= 20)
	@ApiModelProperty(notes = "Insert password(min = 6, max = 20) for the new user")
	private String password;
	
	@ApiModelProperty(notes = "Insert newsletter preferences for the new user")
	private boolean hasNewsletter = false;
}