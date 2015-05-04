package Jet.ExpandEntitySet;

/**
 * Created by yhe on 2/20/14.
 */
public class RankChoiceEntity extends Entity {
    public enum EntityDecision {
        YES, NO, UNDECIDED;
    }

    private EntityDecision decision;

    public RankChoiceEntity(String text) {
        super(text);
        decision = EntityDecision.UNDECIDED;
    }

    public RankChoiceEntity(String text, String type, double score) {
        super(text, type, score);
        decision = EntityDecision.UNDECIDED;
    }

    public EntityDecision getDecision() {
        return decision;
    }

    public void setDecision(EntityDecision decision) {
        this.decision = decision;
    }

    @Override
    public String toString() {
        return decision == EntityDecision.UNDECIDED ? getText() : getText() + " / " + getDecision();
    }
}
