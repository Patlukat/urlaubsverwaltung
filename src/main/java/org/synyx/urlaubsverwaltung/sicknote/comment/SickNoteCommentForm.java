package org.synyx.urlaubsverwaltung.sicknote.comment;

import java.io.Serializable;

public class SickNoteCommentForm implements Serializable {

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
