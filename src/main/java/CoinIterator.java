import java.util.*;

interface CustomIterator<T>  {
    boolean hasNext();

    T next();

    CustomIterator<T> clone();
}

class ListIterator implements CustomIterator<Integer> {
    List<Integer> list;
    int currIndex = 0;
    List<Integer> ans = new ArrayList<>();

    public ListIterator(List<Integer> list) {
        this.list = list;
    }

    @Override
    public boolean hasNext() {
        return currIndex < list.size();
    }

    @Override
    public Integer next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        return list.get(currIndex++);
    }

    @Override
    public CustomIterator<Integer> clone() {
        try {
            return new ListIterator(list);
        } catch (Exception e) {
            throw new InternalError("ListIterator cloning failed", e);
        }

    }
}

class RangeIterator implements CustomIterator<Integer> {
    public int start;
    public int end;
    public int step;
    List<Integer> ans = new ArrayList<>();

    public RangeIterator(int start, int end, int step) {
        if (step == 0) {
            throw new IllegalArgumentException("step must be greater than zero");
        }

        if (step < 0 && start < end) {
            throw new IllegalArgumentException("step must be greater than zero");
        }

        if (step > 0 && start > end) {
            throw new IllegalArgumentException("step must be greater than zero");
        }

        this.start = start;
        this.end = end;
        this.step = step;
    }

    @Override
    public boolean hasNext() {
        if (step > 0) {
            return start <= end;
        } else {
            return start >= end;
        }
    }

    @Override
    public Integer next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        int curr = start;
        start += step;
        return curr;
    }

    @Override
    public CustomIterator<Integer> clone() {
        try {
            return new RangeIterator(start, end, step);
        } catch (Exception e) {
            throw new InternalError("ListIterator cloning failed", e);
        }

    }
}

class ZigZagIterator implements CustomIterator<Integer> {
    Queue<CustomIterator<Integer>> q = new LinkedList<>();
    List<CustomIterator<Integer>> iterators;

    public ZigZagIterator(List<CustomIterator<Integer>> iterators) {
        this.iterators = iterators;
        for (CustomIterator iterator : iterators) {
            if (!iterator.hasNext()) {
                continue;
            }
            q.add(iterator);
        }
    }

    @Override
    public boolean hasNext() {
        return !q.isEmpty();
    }

    @Override
    public Integer next() {
        CustomIterator cur = q.poll();
        int res = (int) cur.next();
        if (cur.hasNext()) {
            q.offer(cur);
        }
        return res;
    }

    @Override
    public CustomIterator<Integer> clone() {
        try {
            List<CustomIterator<Integer>> list = new ArrayList<>();
            for (CustomIterator iterator : iterators) {
                if (iterator.hasNext()) {
                    list.add(iterator.clone());
                }
            }
            return new ZigZagIterator(list);
        } catch (Exception e) {
            throw new InternalError("ListIterator cloning failed", e);
        }

    }
}


public class CoinIterator {
    public static void printIterator(CustomIterator<Integer> i) {
        while (i.hasNext()) {
            System.out.println(i.next());
        }
    }

    public static void main(String[] args) {
        List<Integer> l1 = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        CustomIterator i1 = new ListIterator(l1);
        CustomIterator i2 = new RangeIterator(50, 60, 1);
        CustomIterator i3 = new ZigZagIterator(List.of(i1.clone(), i2.clone()));
        CustomIterator i4 = i3.clone();
        System.out.println("List Iterator 1 : ");
        printIterator(i1);
        System.out.println("Range Iterator 1 : ");
        printIterator(i2);
        System.out.println("ZigZag Iterator 1 : ");
        printIterator(i3);
        System.out.println("ZigZag Iterator 2 : ");
        printIterator(i4);
    }
}

/*

ListIterator implements Iterator
    Input -> list of integers

RangeIterator implements Iterator
    Input -> start, end, step

ZigZagIterator implements Iterator
    Input -> List of iterators




 */