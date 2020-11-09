package it.course.myblog.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.NaturalId;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags")
@Data @NoArgsConstructor @AllArgsConstructor
public class Tag {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@NaturalId(mutable = true)
	@NotNull
	@Column(length = 20, name = "tag_name")
	private String tagName;
	
	@JsonBackReference
	@ManyToMany(fetch = FetchType.EAGER, mappedBy = "tags")
	private Set<Post> posts = new HashSet<>();
	
	@JsonBackReference
	@ManyToMany(fetch = FetchType.EAGER, mappedBy = "preferredTags")
	private Set<Users> users = new HashSet<>();
	
	public Tag(String name) {
		this.tagName = name;
	}
}
