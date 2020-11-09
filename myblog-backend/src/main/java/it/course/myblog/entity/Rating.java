package it.course.myblog.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Check;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rating")
@Check(constraints = "rating >= 1 and rating <= 5")
@Data @AllArgsConstructor @NoArgsConstructor
public class Rating implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Column(name = "rating", nullable = false, columnDefinition = "TINYINT(1)")
	private int rating;
	
	@EmbeddedId
	private RatingUserPostCompositeKey ratingUserPostCompositeKey;
}