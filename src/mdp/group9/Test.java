package mdp.group9;

import java.util.*;

public class Test {

    static int count;

    public static void permutations(Set<Integer> items, Stack<Integer> permutation, int size) {

        /* permutation stack has become equal to size that we require */
        if(permutation.size() == size) {
            /* print the permutation */
            System.out.println(Arrays.toString(permutation.toArray(new Integer[0])));
        }

        /* items available for permutation */
        Integer[] availableItems = items.toArray(new Integer[0]);
        for(Integer i : availableItems) {
            System.out.println("available: " + Arrays.toString(availableItems));
            /* add current item */
            permutation.push(i);

            /* remove item from available item set */
            items.remove(i);

            /* pass it on for next permutation */
            permutations(items, permutation, size);

            /* pop and put the removed item back */
            items.add(permutation.pop());
        }
    }

    public static void main (String[] args) {
        Set<Integer> s = new HashSet<Integer>();
//        s.add(0);
        s.add(1);
        s.add(2);
        s.add(3);
//        s.add(4);

        count = 0;
        permutations(s, new Stack<Integer>(), s.size());
        System.out.println(count + "");

        int[] t = {1, 2};
        int[] q = {1, 2};
        System.out.println(Arrays.equals(t,q));
    }
}
