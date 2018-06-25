package ch.hevs.matrice210;

import java.util.Observable;

public class MocManager extends Observable {
    private static MocManager instance;

    // Singleton
    private MocManager() {
        instance = null;
    }

    public static MocManager getInstance()
    {
        if (instance == null)
            instance = new MocManager();
        return instance;
    }
}
