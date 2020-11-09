package it.course.myblog.payload.request;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
@ApiModel(value = "BlacklistRequest object is used to report a user for a inappropried post or comment")
public class BlacklistRequest {
	
	@NotNull
	@ApiModelProperty(notes = "Insert the start date of the ban")
	private LocalDate blacklistedFrom;
	
	@NotNull
	@ApiModelProperty(notes = "Insert the end date of the ban")
	private LocalDate blacklistedUntil;
	
	@NotNull
	@ApiModelProperty(notes = "Insert the id of the user reported")
	private Long userId;
	
	@NotNull
	@ApiModelProperty(notes = "Insert the id of the post")
	private Long postId;
	
	@ApiModelProperty(notes = "Insert the id of the comment")
	private Long commentId = Long.valueOf(0);
	
	@NotNull
	@ApiModelProperty(notes = "Insert the id of the blacklist reson")
	private Long blacklistReasonId;
	
	@NotNull
	@ApiModelProperty(notes = "Insert the id of the reporting user")
	private long reporter;
	
	
}
