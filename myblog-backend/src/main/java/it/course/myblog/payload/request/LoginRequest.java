package it.course.myblog.payload.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@ApiModel(value = "LoginRequest object is used to log in")
public class LoginRequest {
	
	@NotBlank(message = "Username or Email must not be blank!")
	@Size(min = 2, message = "Username or Email too short!")
	@Size(max = 120, message = "Username or Email too long!")
	@ApiModelProperty(notes = "Insert username or email")
	private String usernameOrEmail;
	
	@NotBlank(message = "Password must not be blank!")
	@Size(min = 5, max = 8)
	@ApiModelProperty(notes = "Insert password")
	private String password;
}
