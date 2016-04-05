package c.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by denny on 4/5/16.
 */
public class Path {
    List<Integer> path = new ArrayList<>();
    void push(int i){
        path.add(i);
    }
    void pop(){
        path.remove(path.size()-1);
    }
    Path copy(){
        Path p = new Path();
        p.path.addAll(path);
        return p;
    }
    int firstStep(){
        return path.get(0);
    }
    Path tail(){
        Path p = new Path();
        p.path.addAll(path.subList(1, path.size()));
        return p;
    }
    boolean isEmpty(){
        return path.isEmpty();
    }


    @Override
    public String toString() {
        return path.toString();
    }
}
