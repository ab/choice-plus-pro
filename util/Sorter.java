package util;

import java.util.Enumeration;
import java.util.Vector;

public class Sorter
{
   public static final int ASCENDING = 0;
   public static final int DESCENDING = 1;

   // List different types of sorts?
   ///public static final int

   /**
    *   E.g:
    *     Vector candidates = ...
    *     Rules rules = ...
    *     Vector sortedCands = Sorter.sort(candidates, DESCENDING);
    *
    *   Note: does not sort in place.  See sortInPlace() for that.
    *   TODO: Better sorting algorithms.
    */
   public static Vector sort(Enumeration objects, int orderDirection)
   {
//System.out.print("Sorter.sort got ");
//Vector sorted = new Vector();
//int i;
//while (objects.hasMoreElements())
//sorted.add(objects.nextElement());
//for (i = 0; i < sorted.size(); i++)
//System.out.print(((Candidate)sorted.get(i)).toString() + " ");
//sorted = insertionSort(sorted.elements(), orderDirection);
//System.out.print("\nSorter.sort returning ");
//for (i = 0; i < sorted.size(); i++)
//System.out.print(((Candidate)sorted.get(i)).toString() + " ");
//System.out.println("");
//return sorted;
	 return insertionSort(objects, orderDirection);
   }

   /**
    * <pre>
    * This should be a surprisingly decent algorithm, because it
    * takes advantage of pseudo-random input, and even with completely
    * random input is effectively n-log-n, since it uses a binary search per
    * insertion.
    * Disadvantage: Not an in-place sort, so need enough memory.
    */
   public static Vector insertionSort(Enumeration objects, int orderDirection)
   {
      int loBound;
      int hiBound;
      int lastNdx = -1;
      int lastNdxPlusOne = -1;
      int checkNdx;
      Object curr = null;
      Vector vec = new Vector();
      int result; // result of comparisons
      int diff;

      // Move data to the Vector
      while (objects.hasMoreElements()) {
         loBound = 0;
         hiBound = vec.size()-1;
         curr = objects.nextElement();

         if (vec.size() == 0)
            vec.addElement(curr);
         else {
            diff = hiBound - loBound;
            while (diff > 0) {
               if (lastNdx != -1) {
                  checkNdx = lastNdx;
                  lastNdx = -1;
               }
               else if (lastNdxPlusOne != -1 && lastNdxPlusOne < vec.size()) {
                  checkNdx = lastNdxPlusOne;
                  lastNdxPlusOne = -1;
               }
               else checkNdx = (int)(diff / 2) + loBound;
               result = compare(curr, vec.elementAt(checkNdx) );
               if (result == Sortable.EQUALS)
                  hiBound = loBound = checkNdx;
               else if (belongsBefore(result,orderDirection))
                  hiBound = checkNdx;
               else  // belongs after
                  loBound = checkNdx+1;
               diff = hiBound - loBound;
            }

            // We've got it down to one element; it goes before or after this element.
            result = compare(curr, vec.elementAt(hiBound) );
            if (belongsBefore(result, orderDirection)) {
               // insert before
               vec.insertElementAt(curr, hiBound);
               lastNdx = hiBound;
            }
            else { // insert after
               vec.insertElementAt(curr, hiBound + 1);
               lastNdx = hiBound + 1;
            }
            if (lastNdx < vec.size())
               lastNdxPlusOne = lastNdx + 1;
         } // end if...else
      } // end while()

      return vec;
   } // end method insertionSort()

   // return 'true' if the current one belongs before the new one
   private static boolean belongsBefore(int result, int orderDirection)
   {
      return ((result == Sortable.LESSTHAN && orderDirection == Sorter.ASCENDING)
               || (result == Sortable.GREATERTHAN && orderDirection == Sorter.DESCENDING)
             );
   }

   // Compare two Sortable's, Strings, or Integers
   private static int compare(Object object1, Object object2)
   {
      int result = Sortable.UNKNOWN;
      if (object1 instanceof Sortable)
         result = ((Sortable)object1).compare((Sortable)object2);
      else if (object1 instanceof String)
      {
         result = ((String)object1).compareTo((String)object2);
         if (result < 0)
            result = Sortable.LESSTHAN;
         else if (result > 0)
            result = Sortable.GREATERTHAN;
         // else result == 0 == Sortable.EQUALS
      }
      else if (object1 instanceof Integer) {
         int int1 = ((Integer)object1).intValue();
         int int2 = ((Integer)object2).intValue();
         if (int1 == int2)
            result = Sortable.EQUALS;
         else if (int1 > int2)
            result = Sortable.GREATERTHAN;
         else result = Sortable.LESSTHAN;
      }

      return result;
   }

   /** For now, orderDirection is ascending */
   public static void quickSort(Vector v)
   {
      quickSortPass(v, 0, v.size()-1);
   }

   private static void quickSortPass(Vector v, int lowBound, int highBound)
   {
	   System.out.println("quickSortPass entered with lowBound=" + lowBound + " and highBound=" + highBound);
      if (highBound - lowBound < 1)
         ; // do nothing, it shouldn't even get here, I think
      else if (highBound - lowBound == 1) {
         if (compareNdx(v,lowBound,highBound) > 0)
            swap(v, lowBound, highBound);
      }
      else {
         int leftNdx = lowBound + 1;
         int rightNdx = highBound;

         while (rightNdx - leftNdx > 1) {
            boolean leftFound = false;
            boolean rightFound = false;

            // Increment 'first' until a number is found
            // that is greater than the anchor
            if (compareNdx(v, leftNdx, lowBound) <= 0)
               ++leftNdx;
            else leftFound = true;
            // Increment 'second' until a number is found
            // that is greater than the anchor
            if (compareNdx(v,rightNdx,lowBound) >= 0)
               --rightNdx;
            else rightFound = true;
            if (leftFound && rightFound) {
               swap(v, leftNdx, rightNdx);
               leftFound = rightFound = false;
            }
         }
         if (compareNdx(v,leftNdx,rightNdx) > 0)
            swap(v, leftNdx, rightNdx);
         int newHighBound, newLowBound;
         if (compareNdx(v,rightNdx,lowBound) < 0) {
            swap(v, rightNdx, lowBound);
            newHighBound = rightNdx - 1;
            newLowBound = rightNdx + 1;
         }
         else if (compareNdx(v,leftNdx,lowBound) < 0) {
            swap(v, leftNdx, lowBound);
            newHighBound = leftNdx - 1;
            newLowBound = leftNdx + 1;
         }
         else {
            newHighBound = leftNdx - 1;
            newLowBound = leftNdx;
         }

         quickSortPass(v, lowBound, newHighBound);
         quickSortPass(v, newLowBound, highBound);
      }
   }

   // Compare, given the indices to a vector
   private static int compareNdx(Vector vec, int first, int second)
   {
      Object objFirst = vec.elementAt(first);
      Object objSecond = vec.elementAt(second);
      return compare(objFirst, objSecond);
   }

   private static void swap(Vector objects, int left, int right)
   {
      Object temp = objects.elementAt(left);
      objects.setElementAt(objects.elementAt(right), left);
      objects.setElementAt(temp, right);
   }
}