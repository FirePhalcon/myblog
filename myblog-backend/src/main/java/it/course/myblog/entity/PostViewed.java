package it.course.myblog.entity;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import it.course.myblog.entity.audit.ViewAudit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_viewed")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PostViewed extends ViewAudit {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@ManyToOne
	@JoinColumn(name = "post_id")
	private Post post;
	
	//@Column(name = "ip", nullable = false, columnDefinition = "VARCHAR(40)")
	private String ip;
	
	@Column(name = "viewed_end", nullable = true)
	private Instant viewedEnd;

	public PostViewed(Post post, String ip) {
		super();
		this.post = post;
		this.ip = ip;
	}
}
