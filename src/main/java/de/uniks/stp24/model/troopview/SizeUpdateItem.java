package de.uniks.stp24.model.troopview;

public class SizeUpdateItem {
    private final String type;
    private final int planned;
    private int amount;

    public SizeUpdateItem(String type, int planned, int amount) {
        this.type = type;
        this.planned = planned;
        this.amount = amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String type() {
        return type;
    }

    public int planned() {
        return planned;
    }

    public int amount() {
        return amount;
    }
}
