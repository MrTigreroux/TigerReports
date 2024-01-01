package fr.mrtigreroux.tigerreports.tasks;

@FunctionalInterface
public interface ResultCallback<R> {

    void onResultReceived(R r);

}
