package de.hsrm.mi.enia.moodplayer.presentation.views;

import javafx.scene.layout.Pane;

/**
 * gemeinsame Basisklasse für alle Controller
 * speichert das Root-Node der View und erzwingt initialize()
 */
public abstract class BaseController<T extends Pane> {

    protected T root;

    /** wird von jedem Controller implementiert */
    public abstract void initialize();

    /** gibt das Root-Element zurück, damit MoodPlayerGUI es in die Scene setzen kann */
    public T getRoot() {
        return root;
    }
}