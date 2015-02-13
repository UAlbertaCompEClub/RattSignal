package ca.jviau.rattsignal.app;

/**
 * @author Jacob
 * @version 1.0
 * @since 2015-02-11
 */
public class SignalResponder {

    private String id;
    private boolean responding;

    public SignalResponder() { }

    public SignalResponder(String id, boolean responding) {
        this.id = id;
        this.responding = responding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isResponding() {
        return responding;
    }

    public void setResponding(boolean responding) {
        this.responding = responding;
    }

    @Override
    public String toString() {
        return "SignalResponder: " + id + " responding: " + responding;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof SignalResponder) && this.id.equals(((SignalResponder) o).id);
    }
}
