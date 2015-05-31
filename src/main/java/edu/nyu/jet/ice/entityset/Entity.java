package edu.nyu.jet.ice.entityset;

/**
 * Java bean for entities in entity set expansion. equality is based on
 * text and type, NOT score.
 *
 * @author yhe
 * @version 1.0
 */
public class Entity {
    private String text;
    private String type;
    private double score;

    public Entity(String text) {
        this.text = text;
    }

    public Entity(String text, String type, double score) {
        this.score = score;
        this.text = text;
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public static Entity fromString(String line) {
//        StringTokenizer t = new StringTokenizer(line, " \t\n\r");
//        double score = Double.valueOf(t.nextToken());
//        String[] parts = t.nextToken().split("/");
        String[] parts = line.trim().split("\\t");
        if (parts.length == 2) {
            double score = Double.valueOf(parts[0]);
            String[] smallParts = parts[1].split("/");
            if (smallParts.length == 2 &&
                    smallParts[0].length() > 1 &&
                    Character.isLetter(smallParts[0].charAt(0))) {
                return new Entity(smallParts[0], smallParts[1], score);
            }
        }
        return null;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entity entity = (Entity) o;

        if (text != null ? !text.equals(entity.text) : entity.text != null) return false;
        if (type != null ? !type.equals(entity.type) : entity.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return text;
    }
}
