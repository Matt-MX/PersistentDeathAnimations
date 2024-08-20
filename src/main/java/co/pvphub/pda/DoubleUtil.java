package co.pvphub.pda;

import java.util.Optional;

public class DoubleUtil {

    public static Optional<Double> parseDouble(String toParse) {
        try {
            return Optional.of(Double.parseDouble(toParse));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

}
