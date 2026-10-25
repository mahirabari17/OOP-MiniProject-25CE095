package util;

/** Small AutoCloseable used to show try-with-resources. */
public class DemoResource implements AutoCloseable {
    private final String name;

    public DemoResource(String name) {
        this.name = name;
        System.out.println("  [resource] " + name + " opened");
    }

    public void use(boolean fail) {
        System.out.println("  [resource] " + name + " in use");
        if (fail) {
            throw new IllegalStateException("something went wrong inside the block");
        }
    }

    @Override
    public void close() {
        System.out.println("  [resource] " + name + " closed");
    }
}
