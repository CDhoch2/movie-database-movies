package de.codecentric.moviedatabase.movies;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import de.codecentric.moviedatabase.movies.domain.Movie;
import de.codecentric.moviedatabase.movies.domain.Tag;
import de.codecentric.moviedatabase.movies.service.InMemoryMovieService;

public class MovieTest {
	
	public UUID uuid;
	public String title;
	public String description;
	public Date startDate;
	public String tagLabel;
	
	@Before
	public void setup(){
		//Daten zum Test-Movie, das angelegt werden soll
		uuid = UUID.randomUUID();
		title = "The Dark Knight";
		description = "Nanananana Batmaaaaan!";
		startDate = new Date(2008, 01, 01);
		tagLabel = "Test-Tag"; 
	}
	
	@Test
	public void testCreateMovie(){
		Movie movie = new Movie(uuid, title, description, startDate);
		
		assertThat(movie.getId(), is(uuid));
		assertThat(movie.getTitle(), is(title));
		assertThat(movie.getDescription(), is(description));
		assertThat(movie.getStartDate(), is(startDate));
	}
	
	@Test
	public void testAddTagToMovie(){
		Movie movie = new Movie(uuid, title, description, startDate);
		Tag tag = new Tag("Test-Tag");
		assertThat(tag.getLabel(), is(tagLabel));
		
		movie.getTags().add(tag);
		assertThat(movie.getTags().size(), is(1));
		assertThat(movie.getTags().iterator().next().getLabel(), is(tagLabel));
	}

	
}
