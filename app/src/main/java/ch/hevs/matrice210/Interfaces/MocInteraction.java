package ch.hevs.matrice210.Interfaces;

import dji.common.error.DJIError;

public interface MocInteraction {
    void dataReceived(byte[] bytes);
    void onResult(DJIError djiError);
}
