package top.nextnet.greekmythcoding.onto;

public interface LabeledObject<T> {
    String label();

    T resource();

    public static <T> LabeledObject<T> getDefault() {
        return new LabeledObject<T>() {
            @Override
            public String label() {
                return "";
            }

            @Override
            public T resource() {
                return null;
            }
        };

    }

    default String resourceAsStr() {
        return resource().toString();
    }


}
