package net.craftcitizen.imagemaps.clcore.command;

import java.util.ArrayList;
import java.util.Arrays;

public class CommandUtils {
    public static String[] parseArgumentStrings(final String[] args) {
        final ArrayList<String> tmp = new ArrayList<String>();
        final int[] open = new int[args.length];
        final int[] close = new int[args.length];
        for (int i = 0; i < args.length; ++i) {
            int j;
            for (j = 0; j < args[i].length() && args[i].charAt(j) == '\"'; ++j) {
                final int n = i;
                open[n] = open[n] + 1;
            }
            for (j = args[i].length() - 1; j >= 0 && args[i].charAt(j) == '\"'; --j) {
                final int n = i;
                close[n] = close[n] + 1;
            }
        }
        int stringPtr = 0;
        while (stringPtr < args.length) {
            if (open[stringPtr] <= 0) {
                tmp.add(args[stringPtr]);
                ++stringPtr;
                continue;
            }
            int count = 0;
            for (int j = stringPtr; j < args.length; ++j) {
                count += open[j];
                if ((count -= close[j]) > 0)
                    continue;
                final String joined = String.join((CharSequence) " ", Arrays.copyOfRange(args, stringPtr, j + 1));
                tmp.add(joined.substring(1, joined.length() - 1));
                stringPtr = j;
                break;
            }
            if (count > 0) {
                tmp.add(args[stringPtr]);
            }
            ++stringPtr;
        }
        return tmp.toArray(new String[tmp.size()]);
    }
}
