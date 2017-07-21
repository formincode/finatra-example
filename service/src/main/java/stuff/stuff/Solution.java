package stuff.stuff;

/**
 * Created by msrivastava on 6/13/17.
 */
public class Solution {
    public static String convert(String s, int numRows) {
        String arr[] = new String[numRows];

        for (int i=0; i<numRows; i++) {
            arr[i]="";
        }

        int j=0;
        char c[] = s.toCharArray();
        for (int i=0; i<s.toCharArray().length; i++) {
            j = i%numRows;
            arr[j]=arr[j]+c[i];
        }

        return "";
    }

    public static void main(String ... args) {
        String ans = convert("PAYPALISHIRING", 3);
        //PAHNAPLSIIGYIR
        /*
          0    6      12
          1  5 7   11 13
          2 4  8 10   14
          3    9      15
         */
        /*
          0   4
          1 3 5
          2   6
         */
    }
}
