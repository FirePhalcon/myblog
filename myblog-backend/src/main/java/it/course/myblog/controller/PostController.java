package it.course.myblog.controller;

import java.awt.Image;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.course.myblog.entity.Comment;
import it.course.myblog.entity.DBFile;
import it.course.myblog.entity.Post;
import it.course.myblog.entity.PostViewed;
import it.course.myblog.entity.Rating;
import it.course.myblog.entity.Role;
import it.course.myblog.entity.RoleName;
import it.course.myblog.entity.Tag;
import it.course.myblog.entity.Users;
import it.course.myblog.payload.request.AddTagsToPosts;
import it.course.myblog.payload.request.PostSearchRequest;
import it.course.myblog.payload.response.ApiResponseCustom;
import it.course.myblog.payload.response.CountPosts;
import it.course.myblog.payload.response.CountRatings;
import it.course.myblog.payload.response.PostResponse;
import it.course.myblog.payload.response.PostResponseImage;
import it.course.myblog.payload.response.PostSearchResponse;
import it.course.myblog.payload.response.PostTagResponse;
import it.course.myblog.repository.CommentRepository;
import it.course.myblog.repository.CreditRepository;
import it.course.myblog.repository.DBFileRepository;
import it.course.myblog.repository.PostRepository;
import it.course.myblog.repository.PostViewedRepository;
import it.course.myblog.repository.RatingRepository;
import it.course.myblog.repository.RoleRepository;
import it.course.myblog.repository.TagRepository;
import it.course.myblog.repository.UserRepository;
import it.course.myblog.security.UserPrincipal;
import it.course.myblog.service.DBFileService;
import it.course.myblog.service.PostService;
import it.course.myblog.service.UserService;

@RestController
@RequestMapping("/posts")
public class PostController {

	@Autowired
	PostRepository postRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	CommentRepository commentRepository;

	@Autowired
	RatingRepository ratingRepository;

	@Autowired
	TagRepository tagRepository;

	@Autowired
	PostService postService;
	
	@Autowired
	PostViewedRepository postViewedRepository;
	
	@Autowired
	UserService userService;
	
	@Autowired
	CreditRepository creditRepository;
	
	@Autowired
	DBFileRepository dbFileRepository;
	
	@Autowired
	DBFileService dbFileService;
	
	@GetMapping("/view-two-posts-by-max-avg")
	@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_EDITOR')")
	public ResponseEntity<ApiResponseCustom> viewTwoPosts(HttpServletRequest request) {

		List<Post> post = postRepository.findTop2ByIsVisibleTrueOrderByAvgRatingDesc();
		List<PostResponse> response = post.stream().map(PostResponse::create).collect(Collectors.toList());

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, response, request.getRequestURI()),
				HttpStatus.OK);
	}

	@GetMapping("/view-two-posts-by-max-number-of-rates")
	@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_EDITOR')")
	public ResponseEntity<ApiResponseCustom> viewTwoPostsByMaxNumberOfVotes(HttpServletRequest request) {

		List<CountRatings> crs = new ArrayList<CountRatings>();
		List<Rating> rating = ratingRepository.groupByPost().stream().collect(Collectors.toList()).subList(0, 2);
		for (Rating r : rating) {
			crs.add(new CountRatings(r.getRatingUserPostCompositeKey().getPostId().getId(),
					Math.toIntExact(ratingRepository
							.countByRatingUserPostCompositeKeyPostId(r.getRatingUserPostCompositeKey().getPostId()))));
		}

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, crs, request.getRequestURI()),
				HttpStatus.OK);
	}

	@GetMapping("/view-all-posts")
	@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_EDITOR')")
	public ResponseEntity<ApiResponseCustom> viewAllPosts(HttpServletRequest request) {

		List<Post> allPosts = postRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, allPosts, request.getRequestURI()),
				HttpStatus.OK);
	}

	@GetMapping("/view-all-published-posts")
	public ResponseEntity<ApiResponseCustom> viewAllPublishedPosts(HttpServletRequest request) {

		List<Post> allPosts = postRepository.findAllByIsVisibleTrue(Sort.by(Sort.Direction.DESC, "updatedAt"));

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, allPosts, request.getRequestURI()),
				HttpStatus.OK);
	}

	@GetMapping("/view-single-post/{id}")
	public ResponseEntity<ApiResponseCustom> viewSinglePost(@PathVariable Long id, HttpServletRequest request) {

		Optional<Post> post = postRepository.findById(id);

		if (!post.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		if (!post.get().isVisible())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 403, null, "Post not visible", request.getRequestURI()),
					HttpStatus.FORBIDDEN);
		
		if(post.get().getCredit().getCreditImport() > 0) {
			if(userService.isLogged()) {
				UserPrincipal up = userService.getAuthenticatedUser();
				if(up.getAuthorities().stream().filter(g -> g.getAuthority().equals("ROLE_READER")).count() > 0 &&
					up.getAuthorities().size() == 1) {
				
					Optional<Users> user = userRepository.findById(up.getId());
					Set<Post> ownedPosts = user.get().getPosts();
					
					if(ownedPosts.stream().filter(p -> (p.getId() == post.get().getId())).count() == 0)
						return new ResponseEntity<ApiResponseCustom>(
								new ApiResponseCustom(Instant.now(), 403, null, "Post not owned", request.getRequestURI()),
								HttpStatus.FORBIDDEN);
				}
			} else
				return new ResponseEntity<ApiResponseCustom>(
						new ApiResponseCustom(Instant.now(), 403, null, "You must be logged in to see this post", request.getRequestURI()),
						HttpStatus.FORBIDDEN);
		}
		
		List<Comment> comments = post.get().getComments().stream().filter(c -> c.isVisible()).collect(Collectors.toList());
		
		PostResponse postResponse = PostResponse.create(post.get());
		postResponse.setComments(comments);
			
		PostViewed postViewed = new PostViewed();
		postViewed.setPost(post.get());
		postViewed.setIp(postService.findIp(request));
		postViewedRepository.save(postViewed);
		
		postResponse.setVisited(postViewed.getId());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, postResponse, request.getRequestURI()),
				HttpStatus.OK);
	}

	@GetMapping("/count-posts-by-editor/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> countPostsByEditor(@PathVariable Long id, HttpServletRequest request) {

		Optional<Users> user = userRepository.findById(id);
		if (!user.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "User Not Found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		long countByCreatedBy = postRepository.countByCreatedBy(id);

		CountPosts c = new CountPosts(user.get().getUsername(), countByCreatedBy);

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, c, request.getRequestURI()),
				HttpStatus.OK);
	}

	@GetMapping("/count-posts-group-by-editor")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> countPostsGroupByEditor(HttpServletRequest request) {

		List<Users> us = userRepository.findAll();
		List<Users> usFilterd = new ArrayList<Users>();
		List<CountPosts> cp = new ArrayList<CountPosts>();

		Role r = roleRepository.findByName(RoleName.valueOf("ROLE_EDITOR")).get();

		for (Users u : us) {
			if (u.getRoles().contains(r))
				usFilterd.add(u);
		}

		long countByCreatedBy = 0;

		for (Users u : usFilterd) {
			countByCreatedBy = postRepository.countByCreatedBy(u.getId());
			cp.add(new CountPosts(u.getUsername(), countByCreatedBy));
		}

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, cp, request.getRequestURI()),
				HttpStatus.OK);
	}

	@PostMapping("/create-post")
	@PreAuthorize("hasRole('EDITOR')")
	public ResponseEntity<ApiResponseCustom> createPost(@RequestBody Post post, HttpServletRequest request) {

		Post p = new Post();
		p.setTitle(post.getTitle());
		p.setContent(post.getContent());
		p.setCredit(creditRepository.findByEndDateIsNullAndCreditCodeStartingWith("P").get());
		postRepository.save(p);

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "New post successfully created", request.getRequestURI()),
				HttpStatus.OK);
	}

	@PutMapping("/approve-post/{id}")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	public ResponseEntity<ApiResponseCustom> approvePost(@PathVariable Long id, HttpServletRequest request) {

		Optional<Post> p = postRepository.findById(id);
		
		if (!p.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		if (p.get().isApproved())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 401, null, "The Post was just approved", request.getRequestURI()),
					HttpStatus.FORBIDDEN);

		p.get().setApproved(true);
		postRepository.save(p.get());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "Post successfully approved", request.getRequestURI()),
				HttpStatus.OK);
	}

	@PutMapping("/disapprove-post/{id}")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	@Transactional
	public ResponseEntity<ApiResponseCustom> disapprovePost(@PathVariable Long id, HttpServletRequest request) {

		Optional<Post> p = postRepository.findById(id);
		Optional<Users> u = userRepository.findById(p.get().getCreatedBy());

		if (!p.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		if (!p.get().isApproved())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 401, null, "The Post was just disapproved", request.getRequestURI()),
					HttpStatus.FORBIDDEN);

		p.get().setApproved(false);
		p.get().setVisible(false);
		postRepository.save(p.get());
		
		if(u.get().getRoles().stream().filter(r -> r.getName().equals(RoleName.ROLE_READER)).count() > 0) {
			u.get().setCredit(u.get().getCredit() - p.get().getCredit().getCreditImport());
			userRepository.save(u.get());
		}
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "Post successfully disapproved", request.getRequestURI()),
				HttpStatus.OK);
	}

	@PutMapping("/publish-post/{id}")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	@Transactional
	public ResponseEntity<ApiResponseCustom> publishPost(@PathVariable Long id, HttpServletRequest request) {

		Optional<Post> p = postRepository.findById(id);
		Optional<Users> u = userRepository.findById(p.get().getCreatedBy());

		if (!p.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		if (!p.get().isApproved())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 401, null, "You need to approve the post before publish it", request.getRequestURI()),
					HttpStatus.FORBIDDEN);

		if (p.get().isVisible())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 401, null, "The Post was just published", request.getRequestURI()),
					HttpStatus.FORBIDDEN);

		p.get().setVisible(true);

		postRepository.save(p.get());
		
		if(u.get().getRoles().stream().filter(r -> r.getName().equals(RoleName.ROLE_READER)).count() > 0) {
			u.get().setCredit(u.get().getCredit() + p.get().getCredit().getCreditImport());
			userRepository.save(u.get());
		}

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "Post successfully published", request.getRequestURI()),
				HttpStatus.OK);
	}

	@PutMapping("/unpublish-post/{id}")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	@Transactional
	public ResponseEntity<ApiResponseCustom> unpublishPost(@PathVariable Long id, @PathVariable boolean isDeluxe,
			HttpServletRequest request) {

		Optional<Post> p = postRepository.findById(id);
		Optional<Users> u = userRepository.findById(p.get().getCreatedBy());

		if (!p.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		if (!p.get().isVisible())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 401, null, "The Post was just unpublished", request.getRequestURI()),
					HttpStatus.FORBIDDEN);

		p.get().setVisible(false);

		postRepository.save(p.get());
		
		if(u.get().getRoles().stream().filter(r -> r.getName().equals(RoleName.ROLE_READER)).count() > 0) {
			u.get().setCredit(u.get().getCredit() - p.get().getCredit().getCreditImport());
			userRepository.save(u.get());
		}
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "Post successfully unpublished", request.getRequestURI()),
				HttpStatus.OK);
	}

	@PutMapping("/add-tags-to-posts")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> addTagsToPosts(@RequestBody AddTagsToPosts addTagsToPosts,
			HttpServletRequest request) {

		List<Post> postsToBeUpdated = postRepository.findByIdIn(addTagsToPosts.getIds());
		Set<Tag> tagsToBeAdded = tagRepository.findByIdIn(addTagsToPosts.getTagIds());

		if (postsToBeUpdated.size() == 0 || tagsToBeAdded.size() == 0)
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "No tags OR posts found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		for (Post p : postsToBeUpdated) {

			Set<Tag> ts = new HashSet<>(p.getTags());

			for (Tag tagToMatch : tagsToBeAdded) {

				ts.add(tagToMatch);
			}

			p.setTags(ts);
			postRepository.save(p);
		}

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "Posts successfully updated with new tags", request.getRequestURI()),
				HttpStatus.OK);
	}

	@GetMapping("/get-posts-by-tag/{tagName}")
	public ResponseEntity<ApiResponseCustom> getPostsByTag(@PathVariable String tagName, HttpServletRequest request) {

		Optional<Tag> t = tagRepository.findByTagName(tagName);
		if (!t.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Tag not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		List<Post> ps = postRepository.findAllByIsVisibleTrue();
		if (ps.size() == 0)
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "No posts published found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		List<Post> psByTag = new ArrayList<Post>();

		for (Post p : ps) {

			Set<Tag> ts = new HashSet<Tag>(p.getTags());
			if (ts.contains(t.get()))
				psByTag.add(p);
		}

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, psByTag, request.getRequestURI()), 
				HttpStatus.OK);
	}
	
	@GetMapping("/get-posts-by-tags")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseCustom> getPostsByTags(
			@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "3") Integer pageSize,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "ASC") String direction,
			@RequestParam(defaultValue = "") Set<String> tagsName,
			HttpServletRequest request) {

		Set<Tag> tagsSearched = tagRepository.findByTagNameIn(tagsName);
		if(tagsSearched.isEmpty())
			return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 404, null, "Tags not found", request.getRequestURI()),
				HttpStatus.NOT_FOUND);
		
		List<Long> ids = new ArrayList<Long>();
		Map<Long, Integer> tagsFound = new HashMap<Long, Integer>();
		
		for(Tag tag : tagsSearched) {
			for(Post post : tag.getPosts()) {
				
				if(post.isVisible()) {
					ids.add(post.getId());
					
					if(tagsFound.containsKey(post.getId()))
						tagsFound.replace(post.getId(), tagsFound.get(post.getId())+1);
					else
						tagsFound.put(post.getId(), 1);
				}
			}
		}
		
		List<Post> posts = postRepository.findByIdIn(ids);
		
		List<PostTagResponse> ptr = posts.stream()	
				.map(ps -> new PostTagResponse(
						ps.getId(),
						ps.getTitle(),
						ps.getTags(),
						PostService.calcRelevance(tagsName,	ps,	tagsFound))
				).sorted(Comparator.comparingDouble(PostTagResponse::getRelevance))
				.collect(Collectors.toList());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, ptr, request.getRequestURI()),
				HttpStatus.OK);
	}

	@GetMapping("/get-posts-without-tags")
	@PreAuthorize("hasRole('MANAGING_EDITOR')")
	public ResponseEntity<ApiResponseCustom> getPostsWithoutTags(HttpServletRequest request) {

		List<Post> ps = postRepository.findAllByIsVisibleTrue();
		if (ps.size() == 0)
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "No posts published found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		List<Post> psWithoutTags = ps.stream().filter(p -> p.getTags().size() == 0).collect(Collectors.toList());

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, psWithoutTags, request.getRequestURI()),
				HttpStatus.OK);
	}

	@PostMapping("/buy-post/{postId}")
	@PreAuthorize("hasRole('READER')")
	public ResponseEntity<ApiResponseCustom> buyPost(@PathVariable Long postId, HttpServletRequest request) {

		// RECOVER FROM SECURITY CONTEXT THE USER LOGGED IN
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

		Users u = userRepository.findById(userPrincipal.getId()).get();
		Optional<Post> p = postRepository.findById(postId);
		if (!p.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);
		if (p.get().getCredit().getCreditImport() == 0)
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post is not deluxe", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		int TotalCredits = u.getCredit();

		if (p.get().getCredit().getCreditImport() > TotalCredits)
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post is not buyable by you", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		Set<Post> posts = u.getPosts();

		if (!posts.contains(p.get())) {
			posts.add(p.get());
			u.setPosts(posts);
			if(u.getRoles().stream().filter(r -> r.getName().equals(RoleName.ROLE_READER)).count() > 0) {
				u.setCredit(TotalCredits - p.get().getCredit().getCreditImport());
				userRepository.save(u);
			}
		} else {
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom(
					Instant.now(), 404, null, "This Post has been bought by you", request.getRequestURI()),
					HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom(
				Instant.now(), 200, null, "Post " + p.get().getId() + " bought by " + u.getUsername(), request.getRequestURI()),
				HttpStatus.OK);

	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponseCustom> searchPostByKeyWord(@RequestBody PostSearchRequest postSearchRequest,
			HttpServletRequest request) {

		Instant start = Instant.now();

		List<Post> posts = new ArrayList<Post>();
		
		if (!postSearchRequest.isCaseSensitive() && !postSearchRequest.isExactMatch())
			posts = postRepository.findByIsVisibleTrueAndContentContaining(postSearchRequest.getKeyword());

		if (postSearchRequest.isCaseSensitive() && !postSearchRequest.isExactMatch()) {
			posts = postRepository.findAllByIsVisibleTrue().stream()
					.filter(p -> p.getContent().contains(postSearchRequest.getKeyword())).collect(Collectors.toList());
		}

		if (postSearchRequest.isCaseSensitive() && postSearchRequest.isExactMatch()) {
			posts = postRepository.findAllByIsVisibleTrue().stream()
					.filter(p -> p.getContent().contains(postSearchRequest.getKeyword())
							&& PostService.isExactMatch(p.getContent(), postSearchRequest.getKeyword()))
					.collect(Collectors.toList());
		}

		if (!postSearchRequest.isCaseSensitive() && postSearchRequest.isExactMatch()) {
			posts = postRepository.findAllByIsVisibleTrue().stream()
					.filter(p -> (p.getContent()).toLowerCase().contains(postSearchRequest.getKeyword().toLowerCase())
							&& PostService.isExactMatch(p.getContent().toLowerCase(),
									postSearchRequest.getKeyword().toLowerCase()))
					.collect(Collectors.toList());
		}

		List<PostSearchResponse> psr = posts.stream().map(PostSearchResponse::create).collect(Collectors.toList());

		for (PostSearchResponse p : psr) {
			
			Pattern word = null;
			String strTofind = null;
			String content = null;
			if(!postSearchRequest.isCaseSensitive()) {
				strTofind = postSearchRequest.getKeyword().toLowerCase();
				content = p.getContent().toLowerCase();
			} else {
				strTofind = postSearchRequest.getKeyword();
				content = p.getContent();
			}
			
			if(postSearchRequest.isExactMatch())
				word = Pattern.compile("\\b" + strTofind + "\\b");
			else
				word = Pattern.compile("("+strTofind+")");

			Matcher match = word.matcher(content);
			while (match.find())
				p.setRelevance(p.getRelevance() + 1);

			match = word.matcher(p.getTitle());
			while (match.find())
				p.setRelevance(p.getRelevance() + 1);
		}

		List<PostSearchResponse> psrOrdered = psr.stream().sorted(Comparator.comparingLong(PostSearchResponse::getRelevance).reversed()).collect(Collectors.toList());
		
		Instant end = Instant.now();
		Duration between = Duration.between(start, end);

		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom(
				Instant.now(), 200, "Duration: " + (between.getNano() / 1000000), psrOrdered, request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@GetMapping("/view-all-posts-paged")
	@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_EDITOR')")
	public ResponseEntity<ApiResponseCustom> viewAllPostsPaged(
			@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "3") Integer pageSize,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			HttpServletRequest request){
		
		List<PostSearchResponse> postsPaged = postService.findAllPaged(pageNo, pageSize, sortBy).stream().map(PostSearchResponse::create).collect(Collectors.toList());
		
		return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom(
				Instant.now(), 200, null, postsPaged, request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@GetMapping("/view-all-published-posts-paged")
	public ResponseEntity<ApiResponseCustom> viewAllPublishedPostsPaged(
			@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "3") Integer pageSize,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "ASC") String direction,
			HttpServletRequest request) {

		List<PostSearchResponse> allPosts = postService.findAllPublishedPaged(pageNo, pageSize, sortBy, direction).stream().map(PostSearchResponse::create).collect(Collectors.toList());

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, allPosts, request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@PutMapping("/update-post-with-image")
	@PreAuthorize("hasRole('EDITOR')")
	@Transactional
	public ResponseEntity<ApiResponseCustom> updatePostWithImage(@RequestPart("dbFile") MultipartFile file,
			@RequestParam("postId") Long id, HttpServletRequest request) throws IOException {

		Optional<Post> post = postRepository.findById(id);
		
		if(!post.isPresent())
			return new ResponseEntity<ApiResponseCustom>(new ApiResponseCustom(
					Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);
		
		DBFile dbFile = new DBFile(file.getOriginalFilename(), file.getContentType(), file.getBytes());
		
		dbFileRepository.save(dbFile);
		
		post.get().setDbFile(dbFile);
		
		postRepository.save(post.get());

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "Post succesfully updated", request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@PostMapping("/create-post-with-image")
	@PreAuthorize("hasRole('EDITOR')")
	@Transactional
	public ResponseEntity<ApiResponseCustom> createPostWithImage(@RequestPart("dbFile") MultipartFile file,
			@RequestParam("title") String title, @RequestParam("content") String content, HttpServletRequest request) throws IOException {

		String fileType = file.getContentType();
		
		if(!fileType.contains("png") &&
		   !fileType.contains("jpg") &&
		   !fileType.contains("gif"))
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 403, null, "Extension of the file must be PNG, JPG or GIF", request.getRequestURI()),
					HttpStatus.FORBIDDEN);
		
		try {
		    Image image = ImageIO.read(file.getInputStream());
		    
			if(image.getWidth(null) > 600)
				return new ResponseEntity<ApiResponseCustom>(
						new ApiResponseCustom(Instant.now(), 403, null, "Width of the file must be maximum 600 px", request.getRequestURI()),
						HttpStatus.FORBIDDEN);
			
			if(image.getHeight(null) > 300)
				return new ResponseEntity<ApiResponseCustom>(
						new ApiResponseCustom(Instant.now(), 403, null, "Height of the file must be maximum 300 px", request.getRequestURI()),
						HttpStatus.FORBIDDEN);
				
		} catch (IOException ex) {
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 403, null, ex.getMessage(), request.getRequestURI()),
					HttpStatus.FORBIDDEN);
		}
		
		DBFile dbFile = dbFileService.fromMultiToDBFile(file);
		
		dbFileRepository.save(dbFile);
		
		Post post = new Post();
		post.setTitle(title);
		post.setContent(content);
		post.setCredit(creditRepository.findByEndDateIsNullAndCreditCodeStartingWith("P").get());
		post.setDbFile(dbFile);
		
		postRepository.save(post);

		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, "New post successfully created", request.getRequestURI()),
				HttpStatus.OK);
	}
	
	@GetMapping("/view-single-post-with-image/{id}")
	public ResponseEntity<ApiResponseCustom> viewSinglePostWithImage(@PathVariable Long id, HttpServletRequest request) throws IOException {

		Optional<Post> post = postRepository.findById(id);

		if (!post.isPresent())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 404, null, "Post not found", request.getRequestURI()),
					HttpStatus.NOT_FOUND);

		if (!post.get().isVisible())
			return new ResponseEntity<ApiResponseCustom>(
					new ApiResponseCustom(Instant.now(), 403, null, "Post not visible", request.getRequestURI()),
					HttpStatus.FORBIDDEN);
		
		if(post.get().getCredit().getCreditImport() > 0) {
			if(userService.isLogged()) {
				UserPrincipal up = userService.getAuthenticatedUser();
				if(up.getAuthorities().stream().filter(g -> g.getAuthority().equals("ROLE_READER")).count() > 0 &&
					up.getAuthorities().size() == 1) {
				
					Optional<Users> user = userRepository.findById(up.getId());
					Set<Post> ownedPosts = user.get().getPosts();
					
					if(ownedPosts.stream().filter(p -> (p.getId() == post.get().getId())).count() == 0)
						return new ResponseEntity<ApiResponseCustom>(
								new ApiResponseCustom(Instant.now(), 403, null, "Post not owned", request.getRequestURI()),
								HttpStatus.FORBIDDEN);
				}
			} else
				return new ResponseEntity<ApiResponseCustom>(
						new ApiResponseCustom(Instant.now(), 403, null, "You must be logged in to see this post", request.getRequestURI()),
						HttpStatus.FORBIDDEN);
		}
		
		List<Comment> comments = post.get().getComments().stream().filter(c -> c.isVisible()).collect(Collectors.toList());
		
		PostResponseImage postResponseImage = PostResponseImage.create(post.get());
		postResponseImage.setComments(comments);
			
		PostViewed postViewed = new PostViewed();
		postViewed.setPost(post.get());
		postViewed.setIp(postService.findIp(request));
		postViewedRepository.save(postViewed);
		
		postResponseImage.setVisited(postViewed.getId());
		
		return new ResponseEntity<ApiResponseCustom>(
				new ApiResponseCustom(Instant.now(), 200, null, postResponseImage, request.getRequestURI()),
				HttpStatus.OK);
	}
}
