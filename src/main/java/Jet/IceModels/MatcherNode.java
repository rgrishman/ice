package Jet.IceModels;

/**
 * Describe the code here
 *
 * @author yhe
 * @version 1.0
 */
public class MatcherNode {
    String label;
    String token;

    public MatcherNode(String label, String token) {
        this.label = label;
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatcherNode that = (MatcherNode) o;

        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = label != null ? label.hashCode() : 0;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        return result;
    }
}
