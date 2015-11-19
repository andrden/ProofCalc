package c.calc;

import c.model.Rule;

/**
* Created by denny on 11/19/15.
*/
class NamedRule extends Rule {
    String name;
    public NamedRule(String name) {
        super(null, null, null);
        this.name = name;
    }

    @Override
    public String toLineString() {
        return "["+name+"]";
    }

    @Override
    public String toString() {
        return toLineString();
    }
}
