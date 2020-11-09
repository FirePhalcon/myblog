package it.course.myblog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import it.course.myblog.repository.PostPagedRepository;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.entity.Post;
import it.course.myblog.entity.Tag;

@Service
public class PostService {
	
	@Autowired
	PostPagedRepository postPagedRepository;
	
	@Autowired
	PostRepository postRepository;
	
	public List<Post> findAllPaged(Integer pageNo, Integer pageSize, String sortBy){
		
		Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
		
		Page<Post> pagedResult = postPagedRepository.findAll(paging);
		
		if(pagedResult.hasContent())
			return pagedResult.getContent();
		
		return new ArrayList<Post>();
	}
	
	public List<Post> findAllPublishedPaged(Integer pageNo, Integer pageSize, String sortBy, String direction){
		
		Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(Direction.fromString(direction), sortBy));
		
		Page<Post> pagedResult = postRepository.findAllByIsVisibleTrue(paging);
		
		if(pagedResult.hasContent())
			return pagedResult.getContent();
		
		return new ArrayList<Post>();
	}
	
	public List<Post> findAllPagedAndPublishedWithTags(List<Long> ids, Integer pageNo, Integer pageSize, String sortBy, String direction){
		
		Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(Direction.valueOf(direction.toUpperCase()), sortBy));
		
		Page<Post> pagedResult = postRepository.findByIdIn(ids, paging);
		
		if(pagedResult.hasContent())
			return pagedResult.getContent();
		
		return new ArrayList<Post>();
	}
	
	public static double calcRelevance(Set<String> tagsName, Post post, Map<Long, Integer> tagsFound) {
		/**
		 * Numero Tags Cercati : S
			Numero Tag Trovati in un post: F
			Numero tag totali post: T
			Percentuale di base per ogni tag trovato:  B = 100 / S
			Correttiva della rilevanza: C = B * (1- F/T)
			% di rilevanza totale: R = B*F-C
			100/S * (F-1+F/T)
		*/
		
		double s = tagsName.size();
		double t = post.getTags().size();
		double f = tagsFound.get(post.getId());
		
		double k = (100/s) * ( f - 1 + (f/t));
		
		k = Math.floor(k*100) / 100; // round to two decimal
		
		return k;
	}
	
	public double getRelevance(Post post, Set<Tag> tagsSearched) {
		Double relevance = 0.00;
			
		Set<Tag> postTags = post.getTags();
		
		for(Tag tag : postTags) {
			if(tagsSearched.stream().filter(t -> (t.getId() == tag.getId())).collect(Collectors.toList()).size() > 0)
				relevance += 1.00;
			else
				relevance -= 0.20;
		}
		
		return relevance;
	}
	
	public static boolean isExactMatch(String source, String substring) {
		
		String pattern = "\\b" + substring + "\\b";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(source);
		
		return m.find();
	}
	
	public String findIp(HttpServletRequest request) {
		String ip = request.getHeader("X-FORWARDED-FOR");
		
		if(ip == null)
			ip = request.getRemoteAddr();
		
		return ip;
	}
}
