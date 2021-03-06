package de.codecentric.moviedatabase.movies.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.codecentric.moviedatabase.movies.domain.Comment;
import de.codecentric.moviedatabase.movies.domain.Movie;
import de.codecentric.moviedatabase.movies.domain.Tag;
import de.codecentric.moviedatabase.movies.events.MovieEvent;
import de.codecentric.moviedatabase.movies.events.MovieEventType;
import de.codecentric.moviedatabase.movies.exception.ResourceNotFoundException;

public class InMemoryMovieService implements MovieService {

	private StringRedisTemplate redisTemplate;
	private ObjectMapper objectMapper;

	private Map<UUID, Movie> idToMovieMap = new ConcurrentHashMap<UUID, Movie>();
	private Map<Tag, Set<Movie>> tagToMoviesMap = new ConcurrentHashMap<Tag, Set<Movie>>();

	public InMemoryMovieService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		// let the dummy Movie have always the same ID to test it easily via
		// command line tools / unit tests
		Movie movie = new Movie(UUID.fromString("240342ea-84c8-415f-b1d5-8e4376191aeb"), "Star Wars: The Force Awakens",
				"In einer Galaxie weit, weit entfernt", new GregorianCalendar(2015, 12, 17).getTime());
		Tag tag = new Tag("Science Fiction");
		movie.getTags().add(tag);
		createMovie(movie);

		movie = new Movie(UUID.fromString("240342ea-84c8-415f-b1d5-8e4376191aef"), "Batman v Superman: Dawn of Justice",
				"Classic heroes movie", new GregorianCalendar(2016, 3, 24).getTime());
		tag = new Tag("Heroes");
		movie.getTags().add(tag);
		createMovie(movie);

		movie = new Movie(UUID.fromString("69e550ba-ac8e-4620-bf1b-0792d3854938"), "The Martian", "BRING HIM HOME",
				new GregorianCalendar(2015, 10, 8).getTime());
		tag = new Tag("Adventures");
		movie.getTags().add(tag);
		createMovie(movie);

		movie = new Movie(UUID.fromString("fbcc657d-eb8e-4af7-8bb7-9665247c8c94"), "Silver Linings Playbook", "",
				new GregorianCalendar(2013, 1, 3).getTime());
		tag = new Tag("Drama");
		movie.getTags().add(tag);
		createMovie(movie);

		movie = new Movie(UUID.fromString("3ca4e76d-a041-4ddf-aeed-4fc360efd31a"), "The Revenant", "",
				new GregorianCalendar(2016, 1, 14).getTime());
		tag = new Tag("Western");
		movie.getTags().add(tag);
		createMovie(movie);

		movie = new Movie(UUID.fromString("c62c680d-a71a-4327-b931-5ad126c936ea"), "Kingsman: The Secret Service", "",
				new GregorianCalendar(2015, 3, 12).getTime());
		tag = new Tag("Action");
		movie.getTags().add(tag);
		createMovie(movie);

	}

	@Override
	public void createMovie(Movie movie) {
		Assert.notNull(movie);
		idToMovieMap.put(movie.getId(), movie);
		for (Tag tag : movie.getTags()) {
			Set<Movie> movies = tagToMoviesMap.get(tag);
			if (movies == null) {
				movies = new HashSet<Movie>();
			}
			movies.add(movie);
			tagToMoviesMap.put(tag, movies);
		}
		sendNotification(movie, MovieEventType.MOVIE_CREATED);
	}

	private void sendNotification(Movie movie, MovieEventType eventType) {
		try {
			String object = objectMapper.writeValueAsString(new MovieEvent(eventType, movie));
			redisTemplate.convertAndSend("movieChannel", object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Problem with Redis topic.", e);
		}
	}

	@Override
	public void updateMovie(Movie movie) {
		// Tags may not be added or removed by this method
		Assert.notNull(movie);
		idToMovieMap.put(movie.getId(), movie);
		sendNotification(movie, MovieEventType.MOVIE_CHANGED);
	}

	@Override
	public void deleteMovie(UUID id) {
		Assert.notNull(id);
		Movie movie = idToMovieMap.remove(id);
		for (Set<Movie> movies : tagToMoviesMap.values()) {
			movies.remove(movie);
		}
	}

	@Override
	public void addCommentToMovie(Comment comment, UUID movieId) {
		Assert.notNull(comment);
		Assert.notNull(movieId);
		Movie movie = idToMovieMap.get(movieId);
		if (movie == null) {
			throw new ResourceNotFoundException("Movie not found.");
		}
		movie.getComments().add(comment);
	}

	@Override
	public void addTagToMovie(Tag tag, UUID movieId) {
		Assert.notNull(tag);
		Assert.hasText(tag.getLabel());
		Assert.notNull(movieId);
		Movie movie = idToMovieMap.get(movieId);
		if (movie == null) {
			throw new ResourceNotFoundException("Movie not found.");
		}
		movie.getTags().add(tag);
		Set<Movie> movies = tagToMoviesMap.get(tag);
		if (movies == null) {
			movies = new HashSet<Movie>();
		}
		movies.add(movie);
		tagToMoviesMap.put(tag, movies);
	}

	@Override
	public void removeTagFromMovie(Tag tag, UUID movieId) {
		Assert.notNull(tag);
		Assert.notNull(movieId);
		Movie movie = idToMovieMap.get(movieId);
		if (movie == null) {
			throw new ResourceNotFoundException("Movie not found.");
		}
		movie.getTags().remove(tag);
		Set<Movie> movies = tagToMoviesMap.get(tag);
		if (movies != null) {
			movies.remove(movie);
		}
	}

	@Override
	public Movie findMovieById(UUID id) {
		Assert.notNull(id);
		Movie movie = idToMovieMap.get(id);
		if (movie == null) {
			throw new ResourceNotFoundException("Movie not found.");
		}
		return movie;
	}

	@Override
	public List<Movie> findMovieByTagsAndSearchString(Set<Tag> tags, Set<String> searchWords) {
		Set<Movie> taggedMovies = new HashSet<Movie>();
		List<Movie> searchResult = new ArrayList<Movie>();
		if (tags == null || tags.isEmpty()) {
			taggedMovies = new HashSet<Movie>(idToMovieMap.values());
		} else {
			for (Tag tag : tags) {
				Collection<Movie> movies = tagToMoviesMap.get(tag);
				if (movies != null) {
					taggedMovies.addAll(tagToMoviesMap.get(tag));
				}
			}
		}
		searchResult.addAll(taggedMovies);
		if (searchWords != null && !searchWords.isEmpty()) {
			for (String searchWord : searchWords) {
				for (Iterator<Movie> it = searchResult.iterator(); it.hasNext();) {
					Movie movie = it.next();
					if (!(movie.getTitle().toLowerCase().contains(searchWord.toLowerCase()))
							&& !(movie.getDescription() != null
									&& movie.getDescription().toLowerCase().contains(searchWord.toLowerCase()))) {
						it.remove();
					}
				}
			}
		}
		return searchResult;
	}

	@Override
	public List<Tag> findAllTags() {
		return new ArrayList<Tag>(tagToMoviesMap.keySet());
	}

}
