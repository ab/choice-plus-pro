

package util;

import java.math.BigDecimal;
///import prmaster.engine.Mode;//deprecated -- shouldn't need 'engine'...

public class BCD extends BigDecimal
{
   public static final int DEFAULT_PRECISION = 10;//FIXIT?
   public static final int DEFAULT_ROUNDING = ROUND_HALF_UP;

   public static BCD ZERO = null;
   public static BCD ONE = null;

   String str = null; // for debugging purposes

   public static void classInit()
   {
      ZERO = new BCD(0.0D);
      ONE = new BCD(1.0D);
   }
/*
   public BCD(BCD val)
   {
      super(val.toString());
   }
*/
   public BCD()
   {
      super("0.0");
      if (UtilOptions.developing())
         str = this.toString();
   }

   public BCD(Object val)
   {
      super(val.toString());
      ///this = setScale(DEFAULT_PRECISION,DEFAULT_ROUNDING);
      if (UtilOptions.developing())
         str = this.toString();
   }

   /** trivially overrides BigDecimal */
   public BCD(double val)
   {
      super(val);
      ///this = setScale(DEFAULT_PRECISION,DEFAULT_ROUNDING);
      if (UtilOptions.developing())
         str = this.toString();
   }

   /** simpler divide() */
   public BCD divide(BCD other)
   {
      return new BCD(super.divide((BigDecimal)other, DEFAULT_PRECISION, DEFAULT_ROUNDING));
   }

   /** trivially overrides BigDecimal */
   public BCD multiply(BCD other)
   {
      BigDecimal val = super.multiply(other);
      return new BCD(val.setScale(DEFAULT_PRECISION, DEFAULT_ROUNDING));
   }
   public BCD add(BCD other)
   {
      BigDecimal res = super.add(other);
      return new BCD(res);
   }
   public BCD subtract(BCD other)
   {
      return new BCD( super.subtract(other) );
   }

   /** return 'true' if this == 0*/
   public boolean isZero()
   {
      return(this.equals(ZERO));
   }
   /** return 'true' if this < 0*/
   public boolean isNegative()
   {
      return(this.compareTo(ZERO) < 0);
   }
   /** return 'true' if this > 0*/
   public boolean isPositive()
   {
      return(this.compareTo(ZERO) > 0);
   }

   /** return 'true' if this < other */
   public boolean lessThan(BCD other)
   {
      return(this.compareTo(other) < 0);
   }
   public boolean equals(BCD other)
   {
      return(this.compareTo(other) == 0);
   }
   public boolean greaterThan(BCD other)
   {
      return(this.compareTo(other) > 0);
   }

   public BCD round(int precision)
   {
   	return new BCD(setScale(precision,DEFAULT_ROUNDING));
   }

   /** Overrides BigDecimal */
   public String toString(int precision)
   {
      BCD temp = new BCD(setScale(precision, DEFAULT_ROUNDING));
      String s = temp.toString();
      return s;
   }


   public String toStringSigned(int precision)
   {
      StringBuffer buf = new StringBuffer();
      if (isPositive())
         buf.append('+');
      buf.append(toString(precision));

      return buf.toString();
   }
 }
