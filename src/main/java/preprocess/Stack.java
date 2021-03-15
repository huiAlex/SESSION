package preprocess;
import java.util.LinkedList;
public class Stack {
	private LinkedList<Object> linklist = new LinkedList<Object>();
    public void push(Object o) {
    	linklist.addFirst(o);
    }

    public Object pop() {
        return linklist.removeFirst();
    }

    public Object peek() {
        return linklist.getFirst();
    }

    public boolean empty() {
        return linklist.isEmpty();
    }
}