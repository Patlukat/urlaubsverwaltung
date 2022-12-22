package org.synyx.urlaubsverwaltung.comment;

import org.synyx.urlaubsverwaltung.person.Person;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Optional.ofNullable;
import static javax.persistence.GenerationType.SEQUENCE;


/**
 * Represents a basic comment.
 */
@MappedSuperclass
public abstract class AbstractComment {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @GeneratedValue(strategy = SEQUENCE, generator = "comment_generator")
    @SequenceGenerator(name = "comment_generator", sequenceName = "comment_id_seq")
    private Long id;

    // Who has written the comment?
    @ManyToOne
    private Person person;

    // When has the comment be written?
    @Column(nullable = false)
    private Instant date;

    // What is the content of the comment?
    private String text;

    protected AbstractComment() {
        // needed for hibernate
    }

    protected AbstractComment(Clock clock) {
        final Clock c = ofNullable(clock).orElse(Clock.systemUTC());
        this.date = Instant.now(c).truncatedTo(DAYS);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Instant getDate() {
        if (date == null) {
            throw new IllegalStateException("Date of comment can never be null!");
        }

        return date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AbstractComment that = (AbstractComment) o;
        return null != this.getId() && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
