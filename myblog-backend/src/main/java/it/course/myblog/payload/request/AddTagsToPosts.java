package it.course.myblog.payload.request;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class AddTagsToPosts {
	
	List<Long> ids;
	Set<Long> tagIds;
	
}
