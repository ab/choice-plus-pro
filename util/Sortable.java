

package util;

public interface Sortable
{
   public final int UNKNOWN = -2;
   public final int LESSTHAN = -1;
   public final int WORSETHAN = LESSTHAN;
   public final int EQUAL = 0;
   public final int EQUALS = EQUAL;
   public final int GREATERTHAN = 1;
   public final int BETTERTHAN = GREATERTHAN;

   /** @return -1 if this is < other; 0 if equal,
            and 1 if this is > other.
    */
   int compare(Object other);
}
