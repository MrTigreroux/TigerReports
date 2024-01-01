package fr.mrtigreroux.tigerreports.objects;

/**
 * @author MrTigreroux
 */
public interface DeeplyCloneable<T extends DeeplyCloneable<T>> {

    T deepClone();

}
