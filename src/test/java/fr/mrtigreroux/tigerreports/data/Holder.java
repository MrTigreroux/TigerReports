package fr.mrtigreroux.tigerreports.data;

/**
 * @author MrTigreroux
 */
public class Holder<T> {

    private T value = null;

    public Holder() {}

    public Holder(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

}
