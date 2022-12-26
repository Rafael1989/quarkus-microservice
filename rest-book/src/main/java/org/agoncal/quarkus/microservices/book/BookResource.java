package org.agoncal.quarkus.microservices.book;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Instant;

@Path("/api/books")
@Tag(name = "Book REST Endpoint")
public class BookResource {

    @Inject
    Logger logger;

    @RestClient
    NumberProxy proxy;

    @Retry(delay = 3000, maxRetries = 3) // ESPERA 3 SEGUNDOS E TENTA 3 VEZES
    @Fallback(fallbackMethod = "fallbackOnCreatingABook") // QUANDO O OUTRO MICROSERVICE NAO ESTIVER DISPONIVEL VAI CHAMAR ESSE METODO
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(summary = "Creates a new book", description = "Creates a Book with an ISBN number")
    public Response createABook(@FormParam("title") String title, @FormParam("author") String author,
                                @FormParam("year") int yearOfPublication, @FormParam("genre") String genre) {
        Book book = new Book();
        book.isbn13 = proxy.generateIsbnNumbers().isbn13;
        book.title = title;
        book.author = author;
        book.yearOfPublication = yearOfPublication;
        book.genre = genre;
        book.creationDate = Instant.now();
        logger.info("Book created : " + book);
        return Response.status(201).entity(book).build();
    }

    public Response fallbackOnCreatingABook(String title, String author, int yearOfPublication, String genre) throws FileNotFoundException {
        Book book = new Book();
        book.isbn13 = "Will be set later";
        book.title = title;
        book.author = author;
        book.yearOfPublication = yearOfPublication;
        book.genre = genre;
        book.creationDate = Instant.now();
        saveBookOnDisk(book);
        logger.warn("Book saved on disk: " + book);
        return Response.status(206).entity(book).build(); // 206 parcialmente criado
    }

    private void saveBookOnDisk(Book book) throws FileNotFoundException {
        String bookJson = JsonbBuilder.create().toJson(book);
        try(PrintWriter out = new PrintWriter("book-" + Instant.now().toEpochMilli() + ".json")){
            out.println(bookJson);
        }
    }

}